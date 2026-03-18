package br.com.wayvista.minimap.i18n;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {
    private final JavaPlugin plugin;
    private final Map<String, String> pt = new HashMap<>();
    private final Map<String, String> en = new HashMap<>();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        seed();
    }

    public String get(String key, Object... replacements) {
        String language = plugin.getConfig().getString("language", "pt_BR");
        Map<String, String> selected = language.equalsIgnoreCase("en_US") ? en : pt;
        String text = selected.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String token = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);
            text = text.replace("{" + token + "}", value);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void seed() {
        pt.put("prefix", "&8[&bWayVista&8] &r");
        pt.put("no-permission", "&cVoce nao tem permissao.");
        pt.put("player-only", "&cApenas jogadores podem usar este comando.");
        pt.put("invalid-name", "&cNome invalido. Use apenas letras, numeros, '_' e '-'.");
        pt.put("set-usage", "&eUso: /waypoint set <nome> [shared]");
        pt.put("remove-usage", "&eUso: /waypoint remove <nome>");
        pt.put("share-usage", "&eUso: /waypoint share <nome> <on|off>");
        pt.put("viewshared-usage", "&eUso: /waypoint viewshared <on|off>");
        pt.put("minimap-usage", "&eUso: /waypoint minimap <on|off>");
        pt.put("set-created", "&aWaypoint '{name}' criado em {world} ({x}, {y}, {z}) [{shared}].");
        pt.put("set-updated", "&aWaypoint '{name}' atualizado em {world} ({x}, {y}, {z}) [{shared}].");
        pt.put("limit-reached", "&cVoce atingiu o limite de {limit} waypoints.");
        pt.put("remove-ok", "&aWaypoint '{name}' removido.");
        pt.put("remove-missing", "&cWaypoint '{name}' nao encontrado.");
        pt.put("share-ok", "&aWaypoint '{name}' agora esta {state}.");
        pt.put("share-state-on", "&acompartilhado");
        pt.put("share-state-off", "&cprivado");
        pt.put("list-header", "&eSeus waypoints (&f{count}&e):");
        pt.put("list-empty", "&7Voce ainda nao tem waypoints.");
        pt.put("list-line", "&7- &b{name}&7 | {world} ({x}, {y}, {z}) | {shared}");
        pt.put("list-shared-yes", "&acompartilhado");
        pt.put("list-shared-no", "&cprivado");
        pt.put("viewshared-ok", "&aVisualizacao de compartilhados: {state}.");
        pt.put("minimap-ok", "&aMinimapa HUD: {state}.");
        pt.put("state-on", "&aligado");
        pt.put("state-off", "&cdesligado");
        pt.put("help-1", "&e/waypoint set <nome> [shared]");
        pt.put("help-2", "&e/waypoint remove <nome>");
        pt.put("help-3", "&e/waypoint list");
        pt.put("help-4", "&e/waypoint share <nome> <on|off>");
        pt.put("help-5", "&e/waypoint viewshared <on|off>");
        pt.put("help-6", "&e/waypoint minimap <on|off>");
        pt.put("help-7", "&e/minimap <on|off|toggle|status>");
        pt.put("minimap-given", "&aMinimapa ativado! Mapa adicionado na offhand.");
        pt.put("minimap-removed", "&cMinimapa desativado. Mapa removido.");
        pt.put("boolean-invalid", "&cValor invalido. Use on/off, true/false, sim/nao.");

        en.put("prefix", "&8[&bWayVista&8] &r");
        en.put("no-permission", "&cYou do not have permission.");
        en.put("player-only", "&cOnly players can use this command.");
        en.put("invalid-name", "&cInvalid name. Use only letters, numbers, '_' and '-'.");
        en.put("set-usage", "&eUsage: /waypoint set <name> [shared]");
        en.put("remove-usage", "&eUsage: /waypoint remove <name>");
        en.put("share-usage", "&eUsage: /waypoint share <name> <on|off>");
        en.put("viewshared-usage", "&eUsage: /waypoint viewshared <on|off>");
        en.put("minimap-usage", "&eUsage: /waypoint minimap <on|off>");
        en.put("set-created", "&aWaypoint '{name}' created at {world} ({x}, {y}, {z}) [{shared}].");
        en.put("set-updated", "&aWaypoint '{name}' updated at {world} ({x}, {y}, {z}) [{shared}].");
        en.put("limit-reached", "&cYou reached the limit of {limit} waypoints.");
        en.put("remove-ok", "&aWaypoint '{name}' removed.");
        en.put("remove-missing", "&cWaypoint '{name}' not found.");
        en.put("share-ok", "&aWaypoint '{name}' is now {state}.");
        en.put("share-state-on", "&ashared");
        en.put("share-state-off", "&cprivate");
        en.put("list-header", "&eYour waypoints (&f{count}&e):");
        en.put("list-empty", "&7You have no waypoints yet.");
        en.put("list-line", "&7- &b{name}&7 | {world} ({x}, {y}, {z}) | {shared}");
        en.put("list-shared-yes", "&ashared");
        en.put("list-shared-no", "&cprivate");
        en.put("viewshared-ok", "&aShared visibility: {state}.");
        en.put("minimap-ok", "&aMinimap HUD: {state}.");
        en.put("state-on", "&aon");
        en.put("state-off", "&coff");
        en.put("help-1", "&e/waypoint set <name> [shared]");
        en.put("help-2", "&e/waypoint remove <name>");
        en.put("help-3", "&e/waypoint list");
        en.put("help-4", "&e/waypoint share <name> <on|off>");
        en.put("help-5", "&e/waypoint viewshared <on|off>");
        en.put("help-6", "&e/waypoint minimap <on|off>");
        en.put("help-7", "&e/minimap <on|off|toggle|status>");
        en.put("minimap-given", "&aMinimap enabled! Map added to offhand.");
        en.put("minimap-removed", "&cMinimap disabled. Map removed.");
        en.put("boolean-invalid", "&cInvalid value. Use on/off, true/false, yes/no.");
    }
}
