package org.oreo.nodesregions.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oreo.nodesregions.Nodes_regions;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Town;
import phonon.nodes.war.FlagWar;

import java.util.*;

public class RegionsCommand implements CommandExecutor, TabCompleter {

    private final Nodes_regions plugin;
    private final Nodes nodes;

    private final int maxWhitelist;

    public RegionsCommand(Nodes_regions plugin) {
        this.plugin = plugin;
        this.maxWhitelist = plugin.getConfig().getInt("maxPlayerWhitelist");
        this.nodes = Nodes.INSTANCE;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Check if the command sender is a player
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You can only use this command in-game");
            return true;
        }

        Player sender = (Player) commandSender;

        // Assume this returns whether the war is enabled
        boolean war = FlagWar.INSTANCE.getEnabled$nodes();

        // Check if the command sender is not an operator
        if (!commandSender.isOp()) {
            if (!isWhiteListed(sender.getName())) {
                commandSender.sendMessage(ChatColor.RED + "You cannot use this command");
                return true;
            }

            if (war) {
                commandSender.sendMessage(ChatColor.RED + "You cannot use this during war");
                return true;
            }

            // Ensure there is at least one argument
            if (args.length < 1) {
                commandSender.sendMessage("Usage: /regions whitelist <add/remove/get> <playerName>");
                return true;
            }

            // args[0] is the subcommand (add/remove/get)
            String subCommand = args[1].toLowerCase();

            // Debug statement to check subCommand value
            commandSender.sendMessage(ChatColor.YELLOW + "SubCommand: " + subCommand);

            if (subCommand.equals("get")) {
                if (getPlayerList(sender.getName()) == null) {
                    sender.sendMessage(ChatColor.RED + "You aren't whitelisted by staff");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Your region whitelisted players:");
                    for (String player : getPlayerList(sender.getName())) {
                        sender.sendMessage(ChatColor.GREEN + ("- " + player));
                    }
                }
                return true; // Early return since "get" doesn't need further processing
            }

            // Check if the subcommand is "add" or "remove"
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                // Ensure there is a second argument for the player name
                if (args.length < 2) {
                    commandSender.sendMessage("Usage: /regions whitelist <add/remove> <playerName>");
                    return true;
                }

                // args[1] is the playerName
                String playerName = args[2];

                // Convert the player name to a Player object
                Player targetPlayer = Bukkit.getPlayer(playerName);

                if (targetPlayer == null) {
                    commandSender.sendMessage(ChatColor.RED + "Player " + playerName + " is not online or does not exist.");
                    return true;
                }

                if (subCommand.equals("add")) {
                    // Add player to whitelist
                    if (addToPlayerWhitelist(sender.getName(), targetPlayer.getName())) { // Implement this method
                        commandSender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has been added to your whitelist.");
                        plugin.saveWhiteList();
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "Failed to add player " + playerName + " to your whitelist.");
                    }
                } else if (subCommand.equals("remove")) {
                    // Remove player from whitelist
                    if (removeFromPlayerWhitelist(sender.getName(), targetPlayer.getName())) { // Implement this method
                        commandSender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has been removed from your whitelist.");
                        plugin.saveWhiteList();
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "Failed to remove player " + playerName + " from your whitelist.");
                    }
                }

                return true;
            }

            // If the subcommand is not recognized
            commandSender.sendMessage("Usage: /regions whitelist <add/remove/get> <playerName>");
            return true;
        } else {

            if (args.length < 1) {
                commandSender.sendMessage("Usage: /regions <add/remove/get/toggle> <region/town_name> <town_name(s)>");
                return false;
            }

            String subcommand = args[0];

            switch (subcommand.toLowerCase()) {
                case "add":
                case "remove":
                    if (args.length < 3) {
                        commandSender.sendMessage("Usage: /regions <add/remove> <europe/pacific> <town_name(s)>");
                        return false;
                    }
                    String region = args[1];
                    List<String> townNames = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
                    if (!region.equalsIgnoreCase("europe") && !region.equalsIgnoreCase("pacific")) {
                        commandSender.sendMessage("Invalid region. Use 'europe' or 'pacific'.");
                        return false;
                    }

                    for (String townName : townNames) {
                        Town town = nodes.getTownFromName(townName);
                        if (town == null) {
                            commandSender.sendMessage(ChatColor.RED + "Town " + townName + " does not exist.");
                            continue;
                        }

                        if (subcommand.equalsIgnoreCase("add")) {
                            if (region.equalsIgnoreCase("europe")) {
                                if (!plugin.europe.contains(town.getName())) {
                                    plugin.europe.add(town.getName());
                                    commandSender.sendMessage(ChatColor.AQUA + "Added Town: " + townName + " to Europe.");
                                    plugin.saveEurope();
                                } else {
                                    commandSender.sendMessage(ChatColor.RED + "Town " + townName + " is already part of Europe.");
                                }
                            } else if (region.equalsIgnoreCase("pacific")) {
                                if (!plugin.pacific.contains(town.getName())) {
                                    plugin.pacific.add(town.getName());
                                    commandSender.sendMessage(ChatColor.AQUA + "Added Town: " + townName + " to Pacific.");
                                    plugin.savePacific();
                                } else {
                                    commandSender.sendMessage(ChatColor.RED + "Town " + townName + " is already part of the Pacific.");
                                }
                            }
                        } else if (subcommand.equalsIgnoreCase("remove")) {
                            if (region.equalsIgnoreCase("europe")) {
                                if (plugin.europe.remove(town.getName())) {
                                    commandSender.sendMessage(ChatColor.AQUA + "Removed Town: " + townName + " from Europe.");
                                    plugin.saveEurope();
                                } else {
                                    commandSender.sendMessage(ChatColor.RED + "Town " + townName + " isn't part of Europe.");
                                }
                            } else if (region.equalsIgnoreCase("pacific")) {
                                if (plugin.pacific.remove(town.getName())) {
                                    commandSender.sendMessage(ChatColor.AQUA + "Removed Town: " + townName + " from Pacific.");
                                    plugin.savePacific();
                                } else {
                                    commandSender.sendMessage(ChatColor.RED + "Town " + townName + " isn't part of the Pacific.");
                                }
                            }
                        }
                    }
                    break;

                case "get":
                    if (args.length < 2) {  // Check for the town name argument
                        commandSender.sendMessage("Usage: /regions get <town_name>");
                        return false;
                    }

                    String town = args[1];
                    Town getTown = nodes.getTownFromName(town);
                    if (getTown == null) {
                        commandSender.sendMessage(ChatColor.RED + "This town does not exist.");
                    } else {
                        commandSender.sendMessage(ChatColor.AQUA + "Town " + town + " is in groups:");
                        boolean hasRegion = false;
                        if (plugin.europe.contains(getTown.getName())) {
                            commandSender.sendMessage(ChatColor.AQUA + "- Europe");
                            hasRegion = true;
                        }
                        if (plugin.pacific.contains(getTown.getName())) {
                            commandSender.sendMessage(ChatColor.AQUA + "- Pacific");
                            hasRegion = true;
                        }
                        if (!hasRegion) {
                            commandSender.sendMessage(ChatColor.RED + "This town is in no group. Something is wrong, please contact staff.");
                        }
                    }
                    break;

                case "toggle":
                    plugin.regionsDebuff = !plugin.regionsDebuff;
                    String status = plugin.regionsDebuff ? "enabled" : "disabled";
                    commandSender.sendMessage(ChatColor.AQUA + "Regions debuff has been " + status + ".");

                    if (plugin.regionsDebuff) {
                        plugin.addPotionEffect();
                    } else {
                        plugin.playerOutOfRegion.clear();
                    }

                    break;

                case "whitelist":
                    if (args.length < 2) {
                        commandSender.sendMessage("Usage: /regions whitelist <add/remove/get> <playerName>");
                        return true;
                    }

                    String subCommand = args[1];

                    if (subCommand.equalsIgnoreCase("get")) {
                        commandSender.sendMessage(ChatColor.GREEN + "All whitelisted leaders by staff:");
                        for (HashMap<String, List<String>> map : plugin.whiteList) {
                            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                                String keyPlayerName = entry.getKey();
                                commandSender.sendMessage(ChatColor.GREEN + "- " + keyPlayerName);
                            }
                        }
                        return true;
                    }

                    // If the command is "add" or "remove", we need a player name
                    if (args.length < 3) {
                        commandSender.sendMessage("Usage: /regions whitelist <add/remove> <playerName>");
                        return true;
                    }

                    String playerName = args[2];
                    Player targetPlayer = Bukkit.getPlayer(playerName);

                    if (targetPlayer == null) {
                        commandSender.sendMessage(ChatColor.RED + "Player " + playerName + " not found or not online.");
                        return true;
                    }

                    if (subCommand.equalsIgnoreCase("add")) {
                        if (addToWhiteList(targetPlayer.getName(), sender)) {
                            commandSender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has been added to the whitelist.");
                            plugin.saveWhiteList();
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "Failed to add player " + playerName + " to the whitelist.");
                        }
                    } else if (subCommand.equalsIgnoreCase("remove")) {
                        boolean removed = false;
                        Iterator<HashMap<String, List<String>>> iterator = plugin.whiteList.iterator();

                        while (iterator.hasNext()) {
                            HashMap<String, List<String>> map = iterator.next();
                            if (map.containsKey(targetPlayer.getName())) {
                                iterator.remove();
                                commandSender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has been removed from the whitelist.");
                                Objects.requireNonNull(Bukkit.getPlayer(playerName)).sendMessage(ChatColor.DARK_RED + "You have been removed from the leader whitelist");
                                plugin.saveWhiteList();
                                removed = true;
                                break;
                            }
                        }

                        if (!removed) {
                            commandSender.sendMessage(ChatColor.RED + "Player " + playerName + " was not found in the whitelist.");
                        }

                        return true;
                    }

                    break;

                default:
                    commandSender.sendMessage("Usage: /regions <add/remove/get/toggle/whitelist> <arguments>");
                    break;
            }
        }
        return true;
    }




    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(commandSender instanceof Player)){
            return Collections.emptyList();
        }

        Player sender = (Player) commandSender;

        if (!(sender.isOp() || isWhiteListed(sender.getName()))){
            return Collections.emptyList();
        }

        if (args.length == 1) {
            completions.add("whitelist");
        }

        // Ensure only operators can use tab completion
        if (!commandSender.isOp()) {
            if (args.length == 1) {
                // Only show "whitelist" subcommand
                if ("whitelist".startsWith(args[0].toLowerCase())) {
                    completions.add("whitelist");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
                // After "whitelist", only show "add" and "remove"
                List<String> subcommands = Arrays.asList("add", "remove","get");
                for (String subcommand : subcommands) {
                    if (subcommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(subcommand);
                    }
                }
            } else if (args.length == 3 && (args[1].equalsIgnoreCase("add")) && args[0].equalsIgnoreCase("whitelist")){
                for (Player player : Bukkit.getOnlinePlayers()){
                    completions.add(player.getName());
                }
            } else if (args.length == 3 && (args[1].equalsIgnoreCase("remove")) && args[0].equalsIgnoreCase("whitelist")){
                for (HashMap<String, List<String>> map : plugin.whiteList) {
                    if (map.containsKey(sender.getName())) {
                        completions.addAll(map.get(sender.getName()));
                        break;
                    }
                }
            }
        } else {

            // Track already used town names
            Set<String> usedTownNames = new HashSet<>();
            if (args.length > 1) {
                // Collect already specified town names
                for (int i = 2; i < args.length; i++) {
                    usedTownNames.add(args[i].toLowerCase());
                }
            }

            if (args.length == 1) {
                // Handle the first argument (subcommands)
                List<String> subcommands = Arrays.asList("add", "remove", "get", "toggle","whitelist");
                for (String subcommand : subcommands) {
                    if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(subcommand);
                    }
                }
            } else if (args.length == 2) {
                // Handle the second argument (region or town name)
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                    List<String> regions = Arrays.asList("europe", "pacific");
                    for (String region : regions) {
                        if (region.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(region);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("get")) {
                    completions.addAll(getAllTowns());
                } else if (args[0].equalsIgnoreCase("whitelist")) {

                    completions.add("add");
                    completions.add("remove");
                    completions.add("get");

                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("remove")) {
                    // Handle third argument based on the selected region
                    String region = args[1].toLowerCase();
                    List<String> towns = region.equals("europe") ? plugin.europe : plugin.pacific;

                    for (String town : towns) {
                        if (town.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(town);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("add")) {
                    // Autocomplete towns for the add subcommand, excluding those already in the specified region
                    String region = args[1].toLowerCase();
                    List<String> regionTowns = region.equals("europe") ? plugin.europe : plugin.pacific;
                    Set<String> regionTownNames = new HashSet<>();
                    for (String town : regionTowns) {
                        regionTownNames.add(town.toLowerCase());
                    }
                    List<String> allTowns = getAllTowns();
                    for (String townName : allTowns) {
                        if (!regionTownNames.contains(townName.toLowerCase()) && townName.toLowerCase().startsWith(args[2].toLowerCase()) && !usedTownNames.contains(townName.toLowerCase())) {
                            completions.add(townName);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("whitelist")) {
                    if (args[1].equalsIgnoreCase("add")){
                        for (Player player : Bukkit.getOnlinePlayers()){
                            if (!isWhiteListed(player.getName())){
                                completions.add(player.getName());
                            }
                        }
                    } else if (args[1].equalsIgnoreCase("remove")) {
                        for (HashMap<String, List<String>> map : plugin.whiteList) {
                            // There should be only one key per map
                            completions.addAll(map.keySet());
                        }
                    }
                }
            } else if (args.length > 3) {
                // Handle additional town names for both 'add' and 'remove' subcommands
                String subcommand = args[0].toLowerCase();
                String region = args[1].toLowerCase();
                String partialTownName = args[args.length - 1].toLowerCase();

                if (subcommand.equals("add")) {
                    // For 'add', suggest towns that are not in the specified region and not already used
                    List<String> regionTowns = region.equals("europe") ? plugin.europe : plugin.pacific;
                    Set<String> regionTownNames = new HashSet<>();
                    for (String town : regionTowns) {
                        regionTownNames.add(town.toLowerCase());
                    }
                    List<String> allTowns = getAllTowns();
                    for (String townName : allTowns) {
                        if (!regionTownNames.contains(townName.toLowerCase()) && townName.toLowerCase().startsWith(partialTownName) && !usedTownNames.contains(townName.toLowerCase())) {
                            completions.add(townName);
                        }
                    }
                } else if (subcommand.equals("remove")) {
                    // For 'remove', suggest towns that are currently in the specified region and not already used
                    List<String> towns = region.equals("europe") ? plugin.europe : plugin.pacific;
                    for (String town : towns) {
                        if (town.toLowerCase().startsWith(partialTownName) && !usedTownNames.contains(town.toLowerCase())) {
                            completions.add(town);
                        }
                    }
                }
            }

        }
        return completions;
    }


    private List<String> getAllTowns(){

        List<String> towns = new ArrayList<>();

        for (Map.Entry<String, Town> entry : nodes.getTowns$nodes().entrySet()) {
            Town town = entry.getValue();
            towns.add(town.getName());
        }

        return towns;
    }

    private boolean isWhiteListed(String keyToSearch){
        boolean keyFound = false;

        for (HashMap<String, List<String>> map : plugin.whiteList) {
            if (map.containsKey(keyToSearch)) {
                keyFound = true;
                break;
            }
        }

        return keyFound;
    }

    private boolean addToWhiteList(String target , Player sender){
        if (isWhiteListed(target)){
            return false;
        }else {
            HashMap<String, List<String>> playerMap = new HashMap<>();
            playerMap.put(target,new ArrayList<>());
            plugin.whiteList.add(playerMap);

            Objects.requireNonNull(Bukkit.getPlayer(target)).sendMessage(ChatColor.DARK_GREEN + "You can now whitelist players for region immunity");

            return true;
        }
    }


    private boolean addToPlayerWhitelist(String sender, String target){
        for (HashMap<String, List<String>> map : plugin.whiteList) {
            if (map.containsKey(sender)) {
                if (!map.get(sender).contains(target)){
                    if (map.get(sender).size() >= maxWhitelist){
                        Objects.requireNonNull(Bukkit.getPlayer(sender)).sendMessage(ChatColor.RED + "You have already whitelisted " + maxWhitelist + " players");
                        return false;
                    }else {
                        plugin.playerOutOfRegion.remove(Bukkit.getPlayer(target));
                        map.get(sender).add(target) ;
                        Objects.requireNonNull(Bukkit.getPlayer(target)).sendMessage(ChatColor.DARK_GREEN + "You have been whitelisted by  " + sender + " !");
                    }

                }else {
                    Objects.requireNonNull(Bukkit.getPlayer(sender)).sendMessage(ChatColor.RED + "Player specified is already in the whitelist");
                }
                return true;

            }
        }
        return false;
    }

    private boolean removeFromPlayerWhitelist(String sender, String target){
        for (HashMap<String, List<String>> map : plugin.whiteList) {
            if (map.containsKey(sender)) {
                if (map.get(sender).contains(target)){
                    map.get(sender).remove(target) ;
                    Objects.requireNonNull(Bukkit.getPlayer(target)).sendMessage(ChatColor.DARK_RED + "You have been removed from " + sender + "'s whitelist");
                }else {
                    Objects.requireNonNull(Bukkit.getPlayer(sender)).sendMessage(ChatColor.RED + "Player specified is not in the whitelist");
                    return false;
                }
                return true;

            }
        }
        return false;
    }

    private List<String> getPlayerList(String sender){
        for (HashMap<String, List<String>> map : plugin.whiteList) {
            if (map.containsKey(sender)) {
                return map.get(sender);
            }
        }
        Objects.requireNonNull(Bukkit.getPlayer(sender)).sendMessage(ChatColor.RED + "You arent whitelisted by staff");
        return null;
    }

}
