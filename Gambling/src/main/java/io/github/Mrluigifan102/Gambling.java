package io.github.Mrluigifan102;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Gambling extends JavaPlugin {
    private int winpercent;
    private int jackpotpercent;
    private ItemStack item;
    private int max;
    private int maxDigits;
    private ArrayList<UUID> invertedPermission;
    private boolean allowedByDefault;
    private File customYml;
    private FileConfiguration customConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        invertedPermission = new ArrayList<>();

        customYml = new File(getDataFolder()+File.separator+"players.yml");
        customConfig = YamlConfiguration.loadConfiguration(customYml);

        try {
            //Take all UUIDs in players.yml, and add to invertedPermissions.
            String ids = customConfig.getString("players");
            if (ids != null) {
                String[] uuids = customConfig.getString("players").split(",");
                for (String id : uuids) {
                    invertedPermission.add(UUID.fromString(id.trim()));
                }
            }
        } catch (Exception e) {
            customConfig.set("player", null);
            saveCustomYml(customConfig, customYml);
        }

        try {
            winpercent = getConfig().getInt("win");
        } catch (Exception e) {
            getLogger().warning("Gambling: The win chance is improperly configured!");
            winpercent = 45;
        }

        try {
            jackpotpercent = getConfig().getInt("jackpot");
        } catch (Exception e) {
            getLogger().warning("Gambling: The jackpot chance is improperly configured!");
            jackpotpercent = 10;
        }

        if (winpercent+jackpotpercent > 99) {
            getLogger().warning("Gambling: It is impossible to lose! Are you sure you configured the chance correctly?");
        }

        try {
            item = new ItemStack(Material.getMaterial(getConfig().getString("item").toUpperCase()));
        } catch (Exception e) {
            getLogger().warning("Gambling: The material is improperly configured!");
            item = new ItemStack(Material.GOLD_INGOT);
        }

        try {
            max = getConfig().getInt("maxItemsToGamble");
        } catch (Exception e) {
            getLogger().warning("Gambling: The maximum items to gamble is improperly configured!");
            max = 32;
        }

        updateMaxDigits();

        try {
            allowedByDefault = getConfig().getBoolean("allowedByDefault");
        } catch (Exception e) {
            getLogger().warning("Gambling: The allowedByDefault is improperly configured!");
            allowedByDefault = true;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("gamble")) { //Someone sent the command gamble

            //Check if the sender is a player.
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command is for players only!");
                return false;
            }

            Player player = (Player) sender;
            Inventory inv = player.getInventory();

            //Check if the player is allowed to gamble.
            if (allowedByDefault ^ invertedPermission.contains(player.getUniqueId())) {

                //Check if there's an argument, and if that argument is a positive integer.
                if (args.length == 1) {
                    if (isInteger(args[0])) {
                        int n = Integer.parseInt(args[0]);

                        //Check if the specified number falls in the allowed range.
                        if (n <= max) {

                            //Check if player has the right amount of the required item.
                            if (inv.containsAtLeast(item, n)) {
                                player.sendMessage("Feeling lucky, are we? Then let's gamble!");

                                //The gambling minigame.
                                switch (gamble()) {
                                    case 0:
                                        player.sendMessage("Whoops, you lost. Better luck next time.");

                                        //Remove the item one stack at a time, until n items or more have been removed.
                                        while (n > 0) {
                                            ItemStack a = inv.getItem(inv.first(item.getType()));
                                            HashMap<Integer, ItemStack> removed = inv.removeItem(a);

                                            //Put back stacks so we're only taking 1 at a time.
                                            for (int i = 1; i < removed.size(); i++) {
                                                inv.addItem(a);
                                            }

                                            //Decrease the amount left to take.
                                            n -= a.getAmount();
                                        }

                                        //Put back items if we took too many.
                                        if (n < 0) {
                                            n = Math.abs(n);
                                            item.setAmount(n);
                                            inv.addItem(item);
                                        }
                                        break;
                                    case 1:
                                        player.sendMessage("You are a winner!");
                                        item.setAmount(n);
                                        inv.addItem(item);
                                        break;
                                    case 2:
                                        player.sendMessage("Congratulations! You've won the jackpot!");
                                        item.setAmount(n * 4);
                                        inv.addItem(item);
                                        break;
                                    default:
                                        player.sendMessage("Oh. I'm sorry. Something went wrong.");
                                        player.sendMessage("Please check if your inventory's the same," +
                                                " and contact an admin if it isn't.");
                                }
                                return true;

                            }
                            //The player didn't have enough of the item in their inventory.
                            player.sendMessage("You need to actually have " + n + " " +
                                    item.getType().toString().toLowerCase().replace('_', ' ') +
                                    " in your inventory to gamble that many.");
                            return true;

                        }
                        //A number higher than the maximum was specified
                        player.sendMessage("I can't let you gamble with more than " + max + " " +
                                item.getType().toString().toLowerCase().replace('_', ' ') + ".");
                        return true;

                    }
                }
                //The argument was incorrectly formatted.
                player.sendMessage("Please use the correct format.");
                return false;

            }
            player.sendMessage("You aren't allowed to gamble on this server.");
            return true;

        }

        if (cmd.getName().equalsIgnoreCase("togglegamblingrights")) {

            //Check if sender is OP.
            if (!sender.isOp()) {
                sender.sendMessage("You must be OP to use this command.");
                return true;
            }

            //Check if there are arguments
            if (args.length == 0) {
                sender.sendMessage("Please specify a player.");
                return false;
            }

            //Check if there are too many arguments.
            if (args.length > 1) {
                sender.sendMessage("You gave too many arguments.");
                return false;
            }

            @SuppressWarnings("deprecation") OfflinePlayer target = getServer().getOfflinePlayer(args[0]);

            //Check if a player with the given name exists.
            if (target == null) {
                sender.sendMessage("Could not find " + args[0] + ".");
                return true;
            }

            if (invertedPermission.contains(target.getUniqueId())) { //Player is on list. Take off.
                invertedPermission.remove(target.getUniqueId());

                //Take out of players.yml as well.
                ArrayList<String> uuids = new ArrayList<>(Arrays.asList(customConfig.getString("players").split(",")));
                for (int i = 0; i < uuids.size(); i++) {
                    uuids.set(i, uuids.get(i).trim());
                }
                uuids.remove(target.getUniqueId().toString());
                StringBuilder newUUIDs = new StringBuilder("");
                for (int i = 0; i < uuids.size(); i++) {
                    if (i != uuids.size()-1) {
                        newUUIDs.append(uuids.get(i).trim()).append(",");
                    } else {
                        newUUIDs.append(uuids.get(i).trim());
                    }
                }
                customConfig.set("players", newUUIDs.toString());
                saveCustomYml(customConfig, customYml);
                if (customConfig.getString("players").contains("!")) {
                    customConfig.set("players", null);
                    saveCustomYml(customConfig, customYml);
                }

                //Inform sender of success.
                sender.sendMessage(args[0] + " was successfully removed from the list!");

            } else { //Player isn't on list. Put on.
                invertedPermission.add(target.getUniqueId());

                //Put in players.yml as well.
                String uuids = customConfig.getString("players");
                if (uuids != null && !uuids.equals("")) {
                    uuids = uuids + "," + target.getUniqueId().toString();
                } else {
                    uuids = target.getUniqueId().toString();
                }
                customConfig.set("players", uuids);
                saveCustomYml(customConfig, customYml);

                //Inform sender of success.
                sender.sendMessage(args[0] + " was successfully added to the list!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("toggleallgamblingrights")) {

            //Check if sender is OP.
            if (!sender.isOp()) {
                sender.sendMessage("You must be OP to use this command.");
                return false;
            }

            //Check if no arguments were specified.
            if (args.length > 0) {
                sender.sendMessage("Please use the right format.");
                return false;
            }

            invertedPermission = new ArrayList<>();
            ArrayList<String> uuids = new ArrayList<>(Arrays.asList(customConfig.getString("players").split(",")));
            StringBuilder newUUIDs = new StringBuilder();
            OfflinePlayer[] players = getServer().getOfflinePlayers();

            //Add all players who have ever played on the server and weren't on the list, to the list.
            for (int i = 0; i < players.length; i++) {

                //Check if player was already on the list.
                if (!uuids.contains(players[i].getUniqueId().toString())) {
                    invertedPermission.add(players[i].getUniqueId());
                    if (i != players.length-1) {
                        newUUIDs.append(players[i].getUniqueId().toString()).append(",");
                    } else {
                        newUUIDs.append(players[i].getUniqueId().toString());
                    }
                }
            }

            //Save new list to config.
            customConfig.set("players", newUUIDs.toString());
            saveCustomYml(customConfig, customYml);
            if (customConfig.getString("players").contains("!")) {
                customConfig.set("players", null);
                saveCustomYml(customConfig, customYml);
            }

            //Alert sender of success.
            sender.sendMessage("Reversing all gambling permissions was successful!");
            return true;
        }

        return false;
    }

    /**
     * Checks if a string represents a positive integer with 2 digits.
     * @param str String to check
     * @return True if str represents an integer, false if it doesn't.
     */
    private boolean isInteger(String str) {
        //Check if the length of the string is higher than 0.
        int length = str.length();
        if (length == 0) {
            return false;
        }

        //Check if all the digits are integers in and of themselves.
        int checkNonZeroDigits = 0;
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            if (c != '0' || checkNonZeroDigits > 0) {
                checkNonZeroDigits++;
            }
            if (checkNonZeroDigits == maxDigits+1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Does the gambling.
     * @return An integer representing win-state: 0 is a loss, 1 is a win, 2 is a jackpot.
     */
    private int gamble() {
        double n = Math.random()*100;
        if (n < jackpotpercent) {
            return 2;
        } else {
            n -= jackpotpercent;
        }
        if (n < winpercent) {
            return 1;
        }
        return 0;
    }

    /**
     * Redefines maxDigits to match current max.
     */
    private void updateMaxDigits() {
        maxDigits = (int)(Math.log10(max)+1);
    }

    /**
     * Saves a custom .yml file.
     * @param ymlConfig The configuration to save.
     * @param ymlFile The file to save the configuration to.
     */
    private void saveCustomYml(FileConfiguration ymlConfig, File ymlFile) {
        try {
            ymlConfig.save(ymlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
