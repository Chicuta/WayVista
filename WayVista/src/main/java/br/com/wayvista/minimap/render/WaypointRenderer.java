package br.com.wayvista.minimap.render;

import br.com.wayvista.minimap.data.PlayerSettings;
import br.com.wayvista.minimap.data.Waypoint;
import br.com.wayvista.minimap.data.WaypointStore;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class WaypointRenderer {
    private final JavaPlugin plugin;
    private final WaypointStore store;
    private final Map<String, TextDisplay> displays = new HashMap<>();
    private BukkitTask task;

    public WaypointRenderer(JavaPlugin plugin, WaypointStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        int refreshTicks = Math.max(10, plugin.getConfig().getInt("render.refresh-ticks", 20));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, refreshTicks, refreshTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (TextDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
    }

    private void tick() {
        removeOrphans();
        ensureDisplays();
        updateVisibility();
        spawnParticles();
        renderHud();
    }

    private void removeOrphans() {
        Set<String> keys = new HashSet<>();
        for (Waypoint waypoint : store.allWaypoints()) {
            keys.add(waypoint.key());
        }

        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, TextDisplay> entry : displays.entrySet()) {
            if (!keys.contains(entry.getKey()) || entry.getValue() == null || !entry.getValue().isValid()) {
                TextDisplay display = entry.getValue();
                if (display != null && display.isValid()) {
                    display.remove();
                }
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(displays::remove);
    }

    private void ensureDisplays() {
        double textHeight = plugin.getConfig().getDouble("render.text-height", 2.4D);
        for (Waypoint waypoint : store.allWaypoints()) {
            Location location = waypoint.toLocation();
            if (location == null) {
                continue;
            }

            TextDisplay display = displays.get(waypoint.key());
            if (display == null || !display.isValid()) {
                Location spawn = location.clone().add(0.5, textHeight, 0.5);
                display = location.getWorld().spawn(spawn, TextDisplay.class, spawned -> {
                    spawned.setBillboard(Display.Billboard.CENTER);
                    spawned.setShadowed(false);
                    spawned.setSeeThrough(true);
                    spawned.setPersistent(false);
                });
                displays.put(waypoint.key(), display);
            }

            display.text(Component.text(label(waypoint)));
            Location expected = location.clone().add(0.5, textHeight, 0.5);
            if (display.getLocation().distanceSquared(expected) > 0.01) {
                display.teleport(expected);
            }
        }
    }

    private void updateVisibility() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Set<String> visibleKeys = new HashSet<>();
            List<Waypoint> visible = store.getVisibleWaypoints(player);
            for (Waypoint waypoint : visible) {
                visibleKeys.add(waypoint.key());
            }

            for (Map.Entry<String, TextDisplay> entry : displays.entrySet()) {
                TextDisplay display = entry.getValue();
                if (display == null || !display.isValid()) {
                    continue;
                }
                if (visibleKeys.contains(entry.getKey())) {
                    player.showEntity(plugin, display);
                } else {
                    player.hideEntity(plugin, display);
                }
            }
        }
    }

    private void spawnParticles() {
        double maxDistance = plugin.getConfig().getDouble("render.particle-distance", 128.0D);
        double maxDistanceSquared = maxDistance * maxDistance;

        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            List<Waypoint> visible = store.getVisibleWaypoints(viewer);
            for (Waypoint waypoint : visible) {
                Location location = waypoint.toLocation();
                if (location == null || !location.getWorld().equals(viewer.getWorld())) {
                    continue;
                }
                if (viewer.getLocation().distanceSquared(location) > maxDistanceSquared) {
                    continue;
                }

                Location particleBase = location.clone().add(0.5, 0.3, 0.5);
                viewer.spawnParticle(
                    Particle.DUST,
                    particleBase,
                    6,
                    0.15,
                    0.25,
                    0.15,
                    new Particle.DustOptions(waypoint.shared() ? Color.AQUA : Color.ORANGE, 1.1F)
                );
            }
        }
    }

    private void renderHud() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerSettings settings = store.getSettings(player.getUniqueId());
            if (!settings.minimapEnabled()) {
                continue;
            }

            List<Waypoint> visible = store.getVisibleWaypoints(player);
            Waypoint closest = visible.stream()
                .filter(waypoint -> player.getWorld().getName().equalsIgnoreCase(waypoint.worldName()))
                .min(Comparator.comparingDouble(waypoint -> {
                    Location location = waypoint.toLocation();
                    return location == null ? Double.MAX_VALUE : player.getLocation().distanceSquared(location);
                }))
                .orElse(null);

            if (closest == null) {
                player.sendActionBar(Component.text("MiniMap | no waypoint in this world"));
                continue;
            }

            Location target = closest.toLocation();
            if (target == null) {
                continue;
            }

            int distance = (int) Math.round(player.getLocation().distance(target));
            String direction = directionTo(player.getLocation(), target);
            String vis = settings.showShared() ? "all" : "private";
            String sharedLabel = closest.shared() ? "shared" : "private";

            String message = "MiniMap | " + closest.name() + " | " + distance + "m | dir: " + direction
                + " | target: " + sharedLabel + " | view: " + vis;
            player.sendActionBar(Component.text(message));
        }
    }

    private String label(Waypoint waypoint) {
        return waypoint.shared() ? "WP [S] " + waypoint.name() : "WP [P] " + waypoint.name();
    }

    private String directionTo(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double delta = normalizeYaw(targetYaw - from.getYaw());

        if (delta >= -22.5 && delta < 22.5) {
            return "AHEAD";
        }
        if (delta >= 22.5 && delta < 67.5) {
            return "RIGHT-FRONT";
        }
        if (delta >= 67.5 && delta < 112.5) {
            return "RIGHT";
        }
        if (delta >= 112.5 && delta < 157.5) {
            return "RIGHT-BACK";
        }
        if (delta >= 157.5 || delta < -157.5) {
            return "BACK";
        }
        if (delta >= -157.5 && delta < -112.5) {
            return "LEFT-BACK";
        }
        if (delta >= -112.5 && delta < -67.5) {
            return "LEFT";
        }
        return "LEFT-FRONT";
    }

    private double normalizeYaw(double yaw) {
        double value = yaw;
        while (value > 180.0) {
            value -= 360.0;
        }
        while (value < -180.0) {
            value += 360.0;
        }
        return value;
    }
}
