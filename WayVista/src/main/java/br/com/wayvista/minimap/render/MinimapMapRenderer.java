package br.com.wayvista.minimap.render;

import br.com.wayvista.minimap.data.PlayerSettings;
import br.com.wayvista.minimap.data.Waypoint;
import br.com.wayvista.minimap.data.WaypointStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MinimapMapRenderer {
    private static final int MAP_SIZE = 128;
    private static final int HALF = MAP_SIZE / 2;
    private static final String MAP_TAG = "wayvista_minimap";

    private final JavaPlugin plugin;
    private final WaypointStore store;
    private final Map<UUID, MapView> playerMaps = new HashMap<>();
    private BukkitTask task;

    public MinimapMapRenderer(JavaPlugin plugin, WaypointStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerSettings settings = store.getSettings(player.getUniqueId());
            if (!settings.minimapEnabled()) {
                continue;
            }

            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() != Material.FILLED_MAP) {
                continue;
            }
            if (!(offhand.getItemMeta() instanceof MapMeta meta)) {
                continue;
            }
            if (!meta.getPersistentDataContainer().has(
                    new org.bukkit.NamespacedKey(plugin, MAP_TAG),
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                continue;
            }

            MapView view = meta.getMapView();
            if (view == null) {
                continue;
            }
            playerMaps.put(player.getUniqueId(), view);
        }
    }

    public void giveMap(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();

        if (isWayVistaMap(offhand)) {
            return;
        }

        MapView view = Bukkit.createMap(player.getWorld());
        view.setScale(MapView.Scale.CLOSEST);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(new WayVistaMapRenderer(player.getUniqueId()));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(view);
        meta.setDisplayName("\u00a7bWayVista MiniMap");
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, MAP_TAG),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        mapItem.setItemMeta(meta);

        if (offhand.getType() != Material.AIR) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(offhand);
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        player.getInventory().setItemInOffHand(mapItem);
        playerMaps.put(player.getUniqueId(), view);
    }

    public void removeMap(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isWayVistaMap(offhand)) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isWayVistaMap(item)) {
                item.setAmount(0);
            }
        }

        playerMaps.remove(player.getUniqueId());
    }

    private boolean isWayVistaMap(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) {
            return false;
        }
        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            return false;
        }
        return meta.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(plugin, MAP_TAG),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }

    @SuppressWarnings("deprecation")
    private class WayVistaMapRenderer extends MapRenderer {
        private final UUID ownerId;

        WayVistaMapRenderer(UUID ownerId) {
            super(true);
            this.ownerId = ownerId;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (!player.getUniqueId().equals(ownerId)) {
                return;
            }

            PlayerSettings settings = store.getSettings(player.getUniqueId());
            if (!settings.minimapEnabled()) {
                return;
            }

            Location center = player.getLocation();
            World world = center.getWorld();
            int cx = center.getBlockX();
            int cz = center.getBlockZ();
            int scale = plugin.getConfig().getInt("minimap.scale", 1);

            for (int px = 0; px < MAP_SIZE; px++) {
                for (int pz = 0; pz < MAP_SIZE; pz++) {
                    int worldX = cx + (px - HALF) * scale;
                    int worldZ = cz + (pz - HALF) * scale;

                    int topY = world.getHighestBlockYAt(worldX, worldZ);
                    Block block = world.getBlockAt(worldX, topY, worldZ);
                    byte color = blockToMapColor(block.getType());
                    canvas.setPixel(px, pz, color);
                }
            }

            drawPlayerArrow(canvas, player);
            drawWaypoints(canvas, player, cx, cz, scale);
        }

        private void drawPlayerArrow(MapCanvas canvas, Player player) {
            int cx = HALF;
            int cz = HALF;
            byte white = MapPalette.matchColor(255, 255, 255);

            canvas.setPixel(cx, cz, white);
            canvas.setPixel(cx - 1, cz, white);
            canvas.setPixel(cx + 1, cz, white);
            canvas.setPixel(cx, cz - 1, white);
            canvas.setPixel(cx, cz + 1, white);

            float yaw = player.getLocation().getYaw();
            double rad = Math.toRadians(yaw);
            int dx = (int) Math.round(-Math.sin(rad) * 3);
            int dz = (int) Math.round(Math.cos(rad) * 3);
            int ax = clamp(cx + dx, 0, MAP_SIZE - 1);
            int az = clamp(cz + dz, 0, MAP_SIZE - 1);
            canvas.setPixel(ax, az, white);
        }

        private void drawWaypoints(MapCanvas canvas, Player player, int centerX, int centerZ, int scale) {
            List<Waypoint> visible = store.getVisibleWaypoints(player);
            byte markerOwn = MapPalette.matchColor(255, 170, 0);
            byte markerShared = MapPalette.matchColor(0, 200, 255);
            byte textColor = MapPalette.matchColor(255, 255, 255);

            for (Waypoint wp : visible) {
                if (!wp.worldName().equalsIgnoreCase(player.getWorld().getName())) {
                    continue;
                }

                int relX = (int) ((wp.x() - centerX) / scale) + HALF;
                int relZ = (int) ((wp.z() - centerZ) / scale) + HALF;

                if (relX < 2 || relX >= MAP_SIZE - 2 || relZ < 2 || relZ >= MAP_SIZE - 2) {
                    continue;
                }

                byte color = wp.shared() ? markerShared : markerOwn;

                canvas.setPixel(relX, relZ, color);
                canvas.setPixel(relX - 1, relZ, color);
                canvas.setPixel(relX + 1, relZ, color);
                canvas.setPixel(relX, relZ - 1, color);
                canvas.setPixel(relX, relZ + 1, color);

                String name = wp.name();
                if (name.length() > 8) {
                    name = name.substring(0, 8);
                }
                try {
                    // draw name above marker
                    int textX = relX - (name.length() * 2);
                    int textZ = relZ - 4;
                    if (textX >= 0 && textZ >= 0 && textX + name.length() * 5 < MAP_SIZE) {
                        for (int i = 0; i < name.length(); i++) {
                            int dotX = clamp(relX - name.length() + i * 2, 0, MAP_SIZE - 1);
                            int dotZ = clamp(relZ - 3, 0, MAP_SIZE - 1);
                            canvas.setPixel(dotX, dotZ, textColor);
                        }
                    }
                } catch (Exception ignored) {
                    // skip text if out of bounds
                }
            }
        }

        @SuppressWarnings("deprecation")
        private byte blockToMapColor(Material material) {
            return switch (material) {
                case WATER, BUBBLE_COLUMN -> MapPalette.matchColor(64, 64, 255);
                case LAVA -> MapPalette.matchColor(255, 100, 0);
                case GRASS_BLOCK, SHORT_GRASS, TALL_GRASS -> MapPalette.matchColor(90, 160, 55);
                case OAK_LEAVES, BIRCH_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES,
                     DARK_OAK_LEAVES, SPRUCE_LEAVES, MANGROVE_LEAVES,
                     AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES
                     -> MapPalette.matchColor(0, 124, 0);
                case CHERRY_LEAVES -> MapPalette.matchColor(230, 160, 180);
                case SAND, SANDSTONE -> MapPalette.matchColor(219, 211, 160);
                case STONE, COBBLESTONE, ANDESITE, DIORITE, GRANITE, GRAVEL
                     -> MapPalette.matchColor(130, 130, 130);
                case DIRT, COARSE_DIRT, ROOTED_DIRT, FARMLAND
                     -> MapPalette.matchColor(150, 105, 55);
                case SNOW, SNOW_BLOCK, POWDER_SNOW -> MapPalette.matchColor(255, 255, 255);
                case ICE, BLUE_ICE, PACKED_ICE -> MapPalette.matchColor(160, 160, 255);
                case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG,
                     DARK_OAK_LOG, CHERRY_LOG, MANGROVE_LOG, OAK_WOOD
                     -> MapPalette.matchColor(110, 85, 45);
                case OAK_PLANKS, BIRCH_PLANKS, SPRUCE_PLANKS, JUNGLE_PLANKS,
                     ACACIA_PLANKS, DARK_OAK_PLANKS
                     -> MapPalette.matchColor(160, 130, 70);
                case NETHERRACK, NETHER_BRICKS -> MapPalette.matchColor(110, 0, 0);
                case SOUL_SAND, SOUL_SOIL -> MapPalette.matchColor(85, 65, 50);
                case END_STONE -> MapPalette.matchColor(220, 220, 170);
                case OBSIDIAN -> MapPalette.matchColor(20, 18, 30);
                case DEEPSLATE, COBBLED_DEEPSLATE -> MapPalette.matchColor(80, 80, 80);
                case MYCELIUM -> MapPalette.matchColor(115, 95, 115);
                case CLAY -> MapPalette.matchColor(160, 165, 180);
                case TERRACOTTA -> MapPalette.matchColor(150, 90, 60);
                case MOSS_BLOCK -> MapPalette.matchColor(90, 140, 45);
                default -> MapPalette.matchColor(90, 160, 55);
            };
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
