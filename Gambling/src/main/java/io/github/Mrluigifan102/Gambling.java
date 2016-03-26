package io.github.Mrluigifan102;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Gambling extends JavaPlugin {
    private int winpercent;
    private int jackpotpercent;
    private ItemStack item;
    private int max;
    private int maxDigits;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            winpercent = getConfig().getInt("win");
        } catch (Exception e) {
            getLogger().info("Gambling: The win chance is improperly configured!");
            winpercent = 45;
        }

        try {
            jackpotpercent = getConfig().getInt("jackpot");
        } catch (Exception e) {
            getLogger().info("Gambling: The jackpot chance is improperly configured!");
            jackpotpercent = 10;
        }

        try {
            item = new ItemStack(Material.getMaterial(getConfig().getString("item")));
        } catch (Exception e) {
            getLogger().info("Gambling: The material is improperly configured!");
            item = new ItemStack(Material.GOLD_INGOT);
        }

        try {
            max = getConfig().getInt("maxItemsToGamble");
        } catch (Exception e) {
            getLogger().info("Gambling: The maximum items to gamble is improperly configured!");
            max = 32;
        }

        updateMaxDigits();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("gamble")) { //Someone sent the command gamble

            //Check if the sender is a player.
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command is for players only!");
                return false;
            }

            Player player = (Player)sender;
            Inventory inv = player.getInventory();

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
                                    item.setAmount(n*4);
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
                        player.sendMessage("You need to actually have " + n + " " + item.getType().toString() +
                                " in your inventory to gamble that many.");
                        return true;

                    }
                    //A number higher than the maximum was specified
                    player.sendMessage("I can't let you gamble with more than " + max + " " +
                            item.getType().toString() + ".");
                    return true;

                }
            }
            //The argument was incorrectly formatted.
            player.sendMessage("Please use the correct format.");
            return false;

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
}
