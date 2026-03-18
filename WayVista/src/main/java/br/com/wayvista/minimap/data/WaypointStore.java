package br.com.wayvista.minimap.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointStore {
    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_-]{1,24}$");

    private final JavaPlugin plugin;
    private final File waypointsFile;
    private final File playersFile;
    private final Map<String, Waypoint> byKey = new HashMap<>();
    private final Map<UUID, PlayerSettings> settingsByPlayer = new HashMap<>();

    public WaypointStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.waypointsFile = new File(plugin.getDataFolder(), "waypoints.yml");
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        loadWaypoints();
        loadPlayers();
    }

    public void save() {
        saveWaypoints();
        savePlayers();
    }

    public boolean isValidName(String name) {
        return SAFE_NAME.matcher(name).matches();
    }

    public int maxWaypointsPerPlayer() {
        return plugin.getConfig().getInt("limits.max-waypoints-per-player", 20);
    }

    public SetResult setWaypoint(Player owner, String name, Location location, boolean shared) {
        String key = Waypoint.key(owner.getUniqueId(), name);
        boolean exists = byKey.containsKey(key);
        if (!exists && getPlayerWaypoints(owner.getUniqueId()).size() >= maxWaypointsPerPlayer()) {
            return SetResult.LIMIT_REACHED;
        }

        Waypoint waypoint = new Waypoint(
            owner.getUniqueId(),
            name,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            shared
        );
        byKey.put(waypoint.key(), waypoint);
        saveWaypoints();
        return exists ? SetResult.UPDATED : SetResult.CREATED;
    }

    public boolean removeWaypoint(UUID owner, String name) {
        String key = Waypoint.key(owner, name);
        Waypoint removed = byKey.remove(key);
        if (removed != null) {
            saveWaypoints();
            return true;
        }
        return false;
    }

    public boolean setShared(UUID owner, String name, boolean shared) {
        Waypoint waypoint = byKey.get(Waypoint.key(owner, name));
        if (waypoint == null) {
            return false;
        }
        waypoint.setShared(shared);
        saveWaypoints();
        return true;
    }

    public List<Waypoint> getPlayerWaypoints(UUID owner) {
        return byKey.values().stream()
            .filter(waypoint -> waypoint.owner().equals(owner))
            .sorted(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Waypoint> getVisibleWaypoints(Player viewer) {
        PlayerSettings settings = getSettings(viewer.getUniqueId());
        UUID viewerId = viewer.getUniqueId();

        return byKey.values().stream()
            .filter(waypoint -> waypoint.owner().equals(viewerId) || (settings.showShared() && waypoint.shared()))
            .sorted(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public Collection<Waypoint> allWaypoints() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    public PlayerSettings getSettings(UUID playerId) {
        return settingsByPlayer.computeIfAbsent(playerId, id -> PlayerSettings.defaults());
    }

    public void setShowShared(UUID playerId, boolean showShared) {
        getSettings(playerId).setShowShared(showShared);
        savePlayers();
    }

    public void setMinimapEnabled(UUID playerId, boolean enabled) {
        getSettings(playerId).setMinimapEnabled(enabled);
        savePlayers();
    }

    private void loadWaypoints() {
        byKey.clear();
        if (!waypointsFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(waypointsFile);
        ConfigurationSection root = yaml.getConfigurationSection("waypoints");
        if (root == null) {
            return;
        }

        for (String index : root.getKeys(false)) {
            String base = "waypoints." + index;
            String ownerString = yaml.getString(base + ".owner");
            String name = yaml.getString(base + ".name");
            String world = yaml.getString(base + ".world");
            if (ownerString == null || name == null || world == null) {
                continue;
            }
            try {
                UUID owner = UUID.fromString(ownerString);
                double x = yaml.getDouble(base + ".x");
                double y = yaml.getDouble(base + ".y");
                double z = yaml.getDouble(base + ".z");
                boolean shared = yaml.getBoolean(base + ".shared", false);
                Waypoint waypoint = new Waypoint(owner, name, world, x, y, z, shared);
                byKey.put(waypoint.key(), waypoint);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid waypoint UUID at key: " + base);
            }
        }
    }

    private void saveWaypoints() {
        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (Waypoint waypoint : byKey.values()) {
            String key = "waypoints." + i++;
            yaml.set(key + ".owner", waypoint.owner().toString());
            yaml.set(key + ".name", waypoint.name());
            yaml.set(key + ".world", waypoint.worldName());
            yaml.set(key + ".x", waypoint.x());
            yaml.set(key + ".y", waypoint.y());
            yaml.set(key + ".z", waypoint.z());
            yaml.set(key + ".shared", waypoint.shared());
        }
        saveYaml(yaml, waypointsFile);
    }

    private void loadPlayers() {
        settingsByPlayer.clear();
        if (!playersFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playersFile);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                boolean showShared = yaml.getBoolean(key + ".show-shared", true);
                boolean minimapEnabled = yaml.getBoolean(key + ".minimap-enabled", true);
                settingsByPlayer.put(uuid, new PlayerSettings(showShared, minimapEnabled));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid player settings UUID: " + key);
            }
        }
    }

    private void savePlayers() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerSettings> entry : settingsByPlayer.entrySet()) {
            String key = entry.getKey().toString();
            PlayerSettings settings = entry.getValue();
            yaml.set(key + ".show-shared", settings.showShared());
            yaml.set(key + ".minimap-enabled", settings.minimapEnabled());
        }
        saveYaml(yaml, playersFile);
    }

    private void saveYaml(YamlConfiguration yaml, File file) {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
                return;
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    public enum SetResult {
        CREATED,
        UPDATED,
        LIMIT_REACHED
    }
}
