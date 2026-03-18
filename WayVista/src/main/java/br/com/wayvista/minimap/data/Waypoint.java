package br.com.wayvista.minimap.data;

import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Waypoint {
    private final UUID owner;
    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private boolean shared;

    public Waypoint(UUID owner, String name, String worldName, double x, double y, double z, boolean shared) {
        this.owner = owner;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shared = shared;
    }

    public UUID owner() {
        return owner;
    }

    public String name() {
        return name;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public boolean shared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String key() {
        return key(owner, name);
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public static String key(UUID owner, String name) {
        return owner + ":" + normalizeName(name);
    }

    public static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
