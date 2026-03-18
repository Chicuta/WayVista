package br.com.wayvista.minimap.command;

import br.com.wayvista.minimap.data.PlayerSettings;
import br.com.wayvista.minimap.data.WaypointStore;
import br.com.wayvista.minimap.i18n.Messages;
import br.com.wayvista.minimap.render.MinimapMapRenderer;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class MinimapCommand implements CommandExecutor, TabCompleter {
    private final WaypointStore store;
    private final Messages messages;
    private final MinimapMapRenderer mapRenderer;

    public MinimapCommand(WaypointStore store, Messages messages, MinimapMapRenderer mapRenderer) {
        this.store = store;
        this.messages = messages;
        this.mapRenderer = mapRenderer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wayvista.use")) {
            sender.sendMessage(messages.get("prefix") + messages.get("no-permission"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("prefix") + messages.get("player-only"));
            return true;
        }

        PlayerSettings settings = store.getSettings(player.getUniqueId());

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            String state = settings.minimapEnabled() ? messages.get("state-on") : messages.get("state-off");
            player.sendMessage(messages.get("prefix") + messages.get("minimap-ok", "state", state));
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        if (mode.equals("toggle")) {
            boolean next = !settings.minimapEnabled();
            applyState(player, next);
            return true;
        }

        if (mode.equals("on") || mode.equals("off")) {
            boolean value = mode.equals("on");
            applyState(player, value);
            return true;
        }

        player.sendMessage(messages.get("prefix") + messages.get("minimap-usage"));
        return true;
    }

    private void applyState(Player player, boolean enabled) {
        store.setMinimapEnabled(player.getUniqueId(), enabled);
        if (enabled) {
            mapRenderer.giveMap(player);
            player.sendMessage(messages.get("prefix") + messages.get("minimap-given"));
        } else {
            mapRenderer.removeMap(player);
            String state = messages.get("state-off");
            player.sendMessage(messages.get("prefix") + messages.get("minimap-ok", "state", state));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle", "status");
        }
        return List.of();
    }
}
