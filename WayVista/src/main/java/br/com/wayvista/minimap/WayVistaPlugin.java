package br.com.wayvista.minimap;

import br.com.wayvista.minimap.command.MinimapCommand;
import br.com.wayvista.minimap.command.WaypointCommand;
import br.com.wayvista.minimap.data.WaypointStore;
import br.com.wayvista.minimap.i18n.Messages;
import br.com.wayvista.minimap.render.MinimapMapRenderer;
import br.com.wayvista.minimap.render.WaypointRenderer;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WayVistaPlugin extends JavaPlugin {
    private WaypointStore store;
    private WaypointRenderer renderer;
    private MinimapMapRenderer mapRenderer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.store = new WaypointStore(this);
        this.store.load();

        Messages messages = new Messages(this);
        this.renderer = new WaypointRenderer(this, store);
        this.mapRenderer = new MinimapMapRenderer(this, store);

        WaypointCommand waypointCommand = new WaypointCommand(store, messages);
        PluginCommand waypoint = Objects.requireNonNull(getCommand("waypoint"), "waypoint command missing");
        waypoint.setExecutor(waypointCommand);
        waypoint.setTabCompleter(waypointCommand);

        MinimapCommand minimapCommand = new MinimapCommand(store, messages, mapRenderer);
        PluginCommand minimap = Objects.requireNonNull(getCommand("minimap"), "minimap command missing");
        minimap.setExecutor(minimapCommand);
        minimap.setTabCompleter(minimapCommand);

        renderer.start();
        mapRenderer.start();
        getLogger().info("WayVista enabled.");
    }

    @Override
    public void onDisable() {
        if (mapRenderer != null) {
            mapRenderer.stop();
        }
        if (renderer != null) {
            renderer.stop();
        }
        if (store != null) {
            store.save();
        }
        getLogger().info("WayVista disabled.");
    }
}
