package br.com.wayvista.minimap.command;

import br.com.wayvista.minimap.data.PlayerSettings;
import br.com.wayvista.minimap.data.Waypoint;
import br.com.wayvista.minimap.data.WaypointStore;
import br.com.wayvista.minimap.i18n.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class WaypointCommand implements CommandExecutor, TabCompleter {
    private final WaypointStore store;
    private final Messages messages;

    public WaypointCommand(WaypointStore store, Messages messages) {
        this.store = store;
        this.messages = messages;
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

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            printHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "set" -> handleSet(player, args);
            case "remove", "delete" -> handleRemove(player, args);
            case "list" -> handleList(player);
            case "share" -> handleShare(player, args);
            case "viewshared" -> handleViewShared(player, args);
            case "minimap" -> handleMinimap(player, args);
            default -> {
                printHelp(player);
                yield true;
            }
        };
    }

    private boolean handleSet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(messages.get("prefix") + messages.get("set-usage"));
            return true;
        }

        String name = args[1];
        if (!store.isValidName(name)) {
            player.sendMessage(messages.get("prefix") + messages.get("invalid-name"));
            return true;
        }

        boolean shared = args.length >= 3 && parseBoolean(args[2], false);
        Location location = player.getLocation();
        WaypointStore.SetResult result = store.setWaypoint(player, name, location, shared);

        if (result == WaypointStore.SetResult.LIMIT_REACHED) {
            player.sendMessage(messages.get("prefix") + messages.get("limit-reached", "limit", store.maxWaypointsPerPlayer()));
            return true;
        }

        String state = shared ? messages.get("share-state-on") : messages.get("share-state-off");
        String key = result == WaypointStore.SetResult.CREATED ? "set-created" : "set-updated";
        player.sendMessage(messages.get("prefix") + messages.get(
            key,
            "name", name,
            "world", location.getWorld().getName(),
            "x", String.format("%.1f", location.getX()),
            "y", String.format("%.1f", location.getY()),
            "z", String.format("%.1f", location.getZ()),
            "shared", state
        ));
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(messages.get("prefix") + messages.get("remove-usage"));
            return true;
        }
        String name = args[1];
        boolean ok = store.removeWaypoint(player.getUniqueId(), name);
        String msg = ok ? messages.get("remove-ok", "name", name) : messages.get("remove-missing", "name", name);
        player.sendMessage(messages.get("prefix") + msg);
        return true;
    }

    private boolean handleList(Player player) {
        List<Waypoint> mine = store.getPlayerWaypoints(player.getUniqueId());
        if (mine.isEmpty()) {
            player.sendMessage(messages.get("prefix") + messages.get("list-empty"));
            return true;
        }

        player.sendMessage(messages.get("prefix") + messages.get("list-header", "count", mine.size()));
        for (Waypoint waypoint : mine) {
            String shared = waypoint.shared() ? messages.get("list-shared-yes") : messages.get("list-shared-no");
            player.sendMessage(messages.get(
                "list-line",
                "name", waypoint.name(),
                "world", waypoint.worldName(),
                "x", String.format("%.1f", waypoint.x()),
                "y", String.format("%.1f", waypoint.y()),
                "z", String.format("%.1f", waypoint.z()),
                "shared", shared
            ));
        }
        return true;
    }

    private boolean handleShare(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(messages.get("prefix") + messages.get("share-usage"));
            return true;
        }
        Boolean value = parseBooleanNullable(args[2]);
        if (value == null) {
            player.sendMessage(messages.get("prefix") + messages.get("boolean-invalid"));
            return true;
        }

        String name = args[1];
        if (!store.setShared(player.getUniqueId(), name, value)) {
            player.sendMessage(messages.get("prefix") + messages.get("remove-missing", "name", name));
            return true;
        }

        String state = value ? messages.get("share-state-on") : messages.get("share-state-off");
        player.sendMessage(messages.get("prefix") + messages.get("share-ok", "name", name, "state", state));
        return true;
    }

    private boolean handleViewShared(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(messages.get("prefix") + messages.get("viewshared-usage"));
            return true;
        }

        Boolean value = parseBooleanNullable(args[1]);
        if (value == null) {
            player.sendMessage(messages.get("prefix") + messages.get("boolean-invalid"));
            return true;
        }

        store.setShowShared(player.getUniqueId(), value);
        String state = value ? messages.get("state-on") : messages.get("state-off");
        player.sendMessage(messages.get("prefix") + messages.get("viewshared-ok", "state", state));
        return true;
    }

    private boolean handleMinimap(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(messages.get("prefix") + messages.get("minimap-usage"));
            return true;
        }

        Boolean value = parseBooleanNullable(args[1]);
        if (value == null) {
            player.sendMessage(messages.get("prefix") + messages.get("boolean-invalid"));
            return true;
        }

        store.setMinimapEnabled(player.getUniqueId(), value);
        String state = value ? messages.get("state-on") : messages.get("state-off");
        player.sendMessage(messages.get("prefix") + messages.get("minimap-ok", "state", state));
        return true;
    }

    private void printHelp(Player player) {
        player.sendMessage(messages.get("prefix") + messages.get("help-1"));
        player.sendMessage(messages.get("prefix") + messages.get("help-2"));
        player.sendMessage(messages.get("prefix") + messages.get("help-3"));
        player.sendMessage(messages.get("prefix") + messages.get("help-4"));
        player.sendMessage(messages.get("prefix") + messages.get("help-5"));
        player.sendMessage(messages.get("prefix") + messages.get("help-6"));
        player.sendMessage(messages.get("prefix") + messages.get("help-7"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("set", "remove", "list", "share", "viewshared", "minimap", "help"), args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("share"))) {
            List<String> names = store.getPlayerWaypoints(player.getUniqueId()).stream().map(Waypoint::name).collect(Collectors.toList());
            return filter(names, args[1]);
        }

        if ((args.length == 2 && (args[0].equalsIgnoreCase("viewshared") || args[0].equalsIgnoreCase("minimap")))
            || (args.length == 3 && args[0].equalsIgnoreCase("share"))) {
            return filter(Arrays.asList("on", "off", "true", "false"), args[args.length - 1]);
        }

        return List.of();
    }

    private List<String> filter(List<String> source, String partial) {
        String lowered = partial.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                out.add(value);
            }
        }
        return out;
    }

    private boolean parseBoolean(String value, boolean fallback) {
        Boolean parsed = parseBooleanNullable(value);
        return parsed == null ? fallback : parsed;
    }

    private Boolean parseBooleanNullable(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        if (v.equals("on") || v.equals("true") || v.equals("yes") || v.equals("sim")) {
            return true;
        }
        if (v.equals("off") || v.equals("false") || v.equals("no") || v.equals("nao")) {
            return false;
        }
        return null;
    }
}
