package io.github.Mrluigifan102;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Gambling extends JavaPlugin {
	int dblpercent;
	int losepercent;
	int jackpotpercent;

	public void onEnable() {
		saveDefaultConfig();
		dblpercent = getConfig().getInt("dblpercent");
		losepercent = getConfig().getInt("dblpercent");
		jackpotpercent = getConfig().getInt("jackpotpercent");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("gamble")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command is for players only!");
				return false;
			}
			Player player = (Player) sender;
			@SuppressWarnings("deprecation")
			ItemStack item = player.getItemInHand();
			Inventory inv = player.getInventory();

			// Check if there's an argument, and if that argument is a
			// positive integer.
			if (args.length == 1) {
				if (isInteger(args[0])) {
					int n = Integer.parseInt(args[0]);

					// Check if the specified number falls in the allowed range.
					if (n < 33) {

						// Check if player has the right amount of the required
						// item.
						if (inv.containsAtLeast(new ItemStack(item.getType()), n)) {
							// Add chat colors
							player.sendMessage("Feeling lucky, are we? Then let's gamble!");

							// The gambling minigame.
							switch (gamble()) {
							case 0:
								player.sendMessage("Whoops, you lost. Better luck next time.");
								inv.remove(new ItemStack(Material.COAL, n));
								break;
							case 1:
								player.sendMessage("You are a winner!");
								inv.addItem(new ItemStack(Material.COAL, n));
								break;
							case 2:
								player.sendMessage("Congratulations! You've won the jackpot!");
								inv.addItem(new ItemStack(Material.COAL, n * 4));
								break;
							default:
								player.sendMessage("Oh. I'm sorry. Something went wrong.");
								player.sendMessage(
										"Please check if your inventory's the same, and contact an admin if it isn't.");
							}
							return true;

						}
						// The player didn't have enough of the item in
						// their inventory.
						player.sendMessage("You actually need to have " + n + " " + item.getType().toString()
								+ " in your inventory to gamble that many.");
						return false;

					}
					// A number higher than the maximum was specified
					player.sendMessage("I can't let you gamble with more than 32 items.");
					return false;
				}
			}
			player.sendMessage("Please use the correct format.");
			return false;
		}
		return false;
	}

	/**
	 * Checks if a string represents a positive integer with 2 digits.
	 * 
	 * @param str
	 *            String to check
	 * @return True if str represents an integer, false if it doesn't.
	 */
	private boolean isInteger(String str) {
		// Check if the length of the string is higher than 0.
		int length = str.length();
		if (length == 0) {
			return false;
		}
		// Checks if all the digits are integers in and of themselves.
		int checkNonZeroDigits = 0;
		for (int i = 0; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
			if (c != '0' || checkNonZeroDigits > 0) {
				checkNonZeroDigits++;
			}
			if (checkNonZeroDigits == 3) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Does the gambling.
	 * 
	 * @return An integer representing win-state: 0 is a loss, 1 is a win, 2 is
	 *         a jackpot.
	 */
	private int gamble() {
		int jackpot = 10;
		int win = 45;
		int n = (int) Math.floor(Math.random() * 100);
		if (n < jackpot) {
			return 2;
		} else {
			n -= jackpot;
		}
		if (n < win) {
			return 1;
		}
		return 0;
	}
