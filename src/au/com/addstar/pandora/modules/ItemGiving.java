package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ItemGiving implements Module, CommandExecutor, TabCompleter {



    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        Objects.requireNonNull(plugin.getCommand("item")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("item")).setTabCompleter(this);

        Objects.requireNonNull(plugin.getCommand("give")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("give")).setTabCompleter(this);

        Objects.requireNonNull(plugin.getCommand("giveall")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("giveall")).setTabCompleter(this);

        Objects.requireNonNull(plugin.getCommand("giveallworld")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("giveallworld")).setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return null;
    }

    private int addItem(Player player, ItemStack item) {
        int added = item.getAmount();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            added -= leftover.get(0).getAmount();
        }

        if (added > 0) {
            player.updateInventory();
        }

        return added;
    }

    private boolean onItem(CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        ItemStack item = Utilities.getItem(args, 0);

        Player player = (Player) sender;
        int added = addItem(player, item);

        String name = Utilities.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }

        if (added > 0) {
            sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name);
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to give " + ChatColor.GOLD + name + ChatColor.RED + ". There is no room for it in your inventory");
        }

        return true;
    }

    private boolean onGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        Player destination = Bukkit.getPlayer(args[0]);
        if (destination == null) {
            throw new IllegalArgumentException("Unknown player " + args[0]);
        }

        ItemStack item = Utilities.getItem(args, 1);

        int added = addItem(destination, item);

        String senderName = getSenderName(sender);
        String name = getItemName(item);
        if (added > 0) {
            sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + destination.getDisplayName());
            destination.sendMessage(ChatColor.RED + senderName + ChatColor.GOLD + " has given you " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name);
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to give " + ChatColor.GOLD + name + ChatColor.RED + ". There is no room for it in " + destination.getDisplayName() + "'s inventory");
        }

        return true;
    }

    private String getSenderName(final CommandSender sender) {
        String senderName = sender.getName();
        if (sender instanceof Player) {
            senderName = ((Player) sender).getDisplayName();
        } else {
            senderName = "Server";
        }
        return senderName;
    }

    private String getItemName(ItemStack item) {
        String name = Utilities.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }
        return name;
    }

    private boolean onGiveAll(CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        ItemStack item = Utilities.getItem(args, 0);

        String name = getItemName(item);
        sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + "everyone");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("pandora.giveall.receive")) {
                continue;
            }
            sendItemThenMessage(sender, player, item);
        }

        return true;
    }

    private boolean onGiveAllWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        String[] worldNames = args[0].split(",");
        Set<World> goodWorlds = Sets.newHashSet();
        Set<World> badWorlds = Sets.newHashSet();

        for (String worldName : worldNames) {
            boolean bad = false;
            if (worldName.startsWith("-")) {
                bad = true;
                worldName = worldName.substring(1);
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null && sender instanceof Player) {
                if (worldName.equalsIgnoreCase("this")) {
                    world = ((Player) sender).getWorld();
                }
            }

            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Unknown world " + worldName + ". Ignoring");
                continue;
            }

            if (bad) {
                badWorlds.add(world);
            } else {
                goodWorlds.add(world);
            }
        }

        if (goodWorlds.isEmpty() && badWorlds.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown world(s) " + args[0]);
            return true;
        }

        ItemStack item = Utilities.getItem(args, 1);

        String name = getItemName(item);
        sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + "everyone" + ChatColor.GOLD + " in specified worlds");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("pandora.giveall.receive")) {
                continue;
            }

            if (goodWorlds.isEmpty()) // Blacklist only
            {
                if (badWorlds.contains(player.getWorld())) {
                    continue;
                }
            } else {
                if (!goodWorlds.contains(player.getWorld())) // whitelist
                {
                    continue;
                }

                if (badWorlds.contains(player.getWorld())) // With blacklist
                {
                    continue;
                }
            }
            sendItemThenMessage(sender, player, item);
        }

        return true;
    }

    private void sendItemThenMessage(final CommandSender sender, final Player player, final ItemStack item) {
        int added = addItem(player, item.clone());
        if (added == 0) {
            sender.sendMessage(ChatColor.RED + player.getDisplayName() + "'s inventory was full");
        } else {
            player.sendMessage(ChatColor.RED + getSenderName(sender) + ChatColor.GOLD + " has given everyone "
                  + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + getItemName(item));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            switch (command.getName()) {
                case "item":
                    return onItem(sender, args);
                case "give":
                    return onGive(sender, args);
                case "giveall":
                    return onGiveAll(sender, args);
                case "giveallworld":
                    return onGiveAllWorld(sender, args);
            }
            return false;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return true;
        }
    }
}