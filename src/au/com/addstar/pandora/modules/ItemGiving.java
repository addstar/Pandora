package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.monolith.util.Messenger;
import au.com.addstar.monolith.util.kyori.adventure.text.TextComponent;
import au.com.addstar.monolith.util.kyori.adventure.text.format.NamedTextColor;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
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
        plugin.getCommand("item").setExecutor(this);
        plugin.getCommand("item").setTabCompleter(this);

        plugin.getCommand("give").setExecutor(this);
        plugin.getCommand("give").setTabCompleter(this);

        plugin.getCommand("giveall").setExecutor(this);
        plugin.getCommand("giveall").setTabCompleter(this);

        plugin.getCommand("giveallworld").setExecutor(this);
        plugin.getCommand("giveallworld").setTabCompleter(this);
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
        if (args.length < 1 || !(sender instanceof Player)) {
            return false;
        }
        ItemStack item = Utilities.getItem(args, 0);

        Player player = (Player) sender;
        int added = addItem(player, item);

        String name = StringTranslator.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }
        TextComponent component;
        if (added > 0) {
            component = TextComponent.builder().color(NamedTextColor.GOLD)
                  .append(TextComponent.of("Giving"))
                  .append(TextComponent.of(added).color(NamedTextColor.RED))
                  .append(TextComponent.of("of"))
                  .append(TextComponent.of(name).color(NamedTextColor.RED))
                  .build();
        } else {
            component = TextComponent.builder().color(NamedTextColor.GOLD)
                  .append(TextComponent.of("Unable to give "))
                  .append(TextComponent.of(name).color(NamedTextColor.RED))
                  .append(TextComponent.of("There is no room for it in your inventory."))
                  .build();
        }
        Messenger.sendMessage(component, sender);
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
        String senderName;
        if (sender instanceof Player) {
            senderName = ((Player) sender).getDisplayName();
        } else {
            senderName = "Server";
        }
        String name = StringTranslator.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }
        TextComponent component;
        if (added > 0) {
            component = TextComponent.builder().color(NamedTextColor.GOLD)
                  .append(TextComponent.of("Giving"))
                  .append(TextComponent.of(added).color(NamedTextColor.RED))
                  .append(TextComponent.of("of"))
                  .append(TextComponent.of(name).color(NamedTextColor.RED))
                  .append(TextComponent.of("to"))
                  .append(TextComponent.of(destination.getDisplayName()).color(NamedTextColor.RED))
                  .build();
            Messenger.sendMessage(TextComponent.builder().color(NamedTextColor.GOLD)
                  .append(TextComponent.of(senderName).color(NamedTextColor.RED))
                  .append(TextComponent.of(" has given you "))
                  .append(TextComponent.of(added).color(NamedTextColor.RED))
                  .append(TextComponent.of("of"))
                  .append(TextComponent.of(name).color(NamedTextColor.RED))
                  .build(), destination);
        } else {
            component = TextComponent.builder().color(NamedTextColor.GOLD)
                  .append(TextComponent.of("Unable to give "))
                  .append(TextComponent.of(name).color(NamedTextColor.RED))
                  .append(TextComponent.of(". There is no room for it in"))
                  .append(TextComponent.of(destination.getDisplayName()).color(NamedTextColor.RED))
                  .append(TextComponent.of("'s inventory"))
                  .build();
        }
        Messenger.sendMessage(component, sender);
        return true;
    }

    private boolean onGiveAll(CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }
        ItemStack item = Utilities.getItem(args, 0);
        String senderName;
        if (sender instanceof Player) {
            senderName = ((Player) sender).getDisplayName();
        } else {
            senderName = "Server";
        }

        String name = StringTranslator.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }
        Messenger.sendMessage(TextComponent.builder().color(NamedTextColor.GOLD)
              .append(TextComponent.of("Giving"))
              .append(TextComponent.of(item.getAmount()).color(NamedTextColor.RED))
              .append(TextComponent.of(" of "))
              .append(TextComponent.of(name).color(NamedTextColor.RED))
              .append(TextComponent.of(" to "))
              .append(TextComponent.of("everyone").color(NamedTextColor.RED))
              .build(), sender);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("pandora.giveall.receive")) {
                continue;
            }
            int added = addItem(player, item.clone());
            if (added == 0) {
                Messenger.sendMessage(
                      TextComponent.of(player.getDisplayName() + "'s inventory was full")
                            .color(NamedTextColor.RED), sender);
            } else {
                Messenger.sendMessage(
                      TextComponent.builder().color(NamedTextColor.GOLD)
                            .append(TextComponent.of(senderName).color(NamedTextColor.GOLD))
                            .append(TextComponent.of(" has given everyone "))
                            .append(TextComponent.of(item.getAmount()).color(NamedTextColor.RED))
                            .append(TextComponent.of("of"))
                            .append(TextComponent.of(name).color(NamedTextColor.RED))
                            .build(), player);
            }
        }
        return true;
    }

    private boolean onGiveAllWorld(final CommandSender sender, final String[] args) {
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
                Messenger.sendMessage(
                      TextComponent.builder()
                            .content("Unknown world(s) " + worldName + ". Ignoring")
                            .color(NamedTextColor.RED)
                            .build(),
                      sender);
                continue;
            }

            if (bad) {
                badWorlds.add(world);
            } else {
                goodWorlds.add(world);
            }
        }

        if (goodWorlds.isEmpty() && badWorlds.isEmpty()) {
            Messenger.sendMessage(
                  TextComponent.builder().content("Unknown world(s) " + args[0]).color(NamedTextColor.RED).build(),
                  sender);
            return true;
        }

        ItemStack item = Utilities.getItem(args, 1);

        String senderName;
        if (sender instanceof Player) {
            senderName = ((Player) sender).getDisplayName();
        } else {
            senderName = "Server";
        }

        String name = StringTranslator.getName(item);
        if (name.equals("Unknown")) {
            name = item.getType().name().toLowerCase();
        }
        Messenger.sendMessage(TextComponent.builder().color(NamedTextColor.GOLD)
              .append(TextComponent.of("Giving"))
              .append(TextComponent.of(item.getAmount()).color(NamedTextColor.RED))
              .append(TextComponent.of(" of "))
              .append(TextComponent.of(name).color(NamedTextColor.RED))
              .append(TextComponent.of(" to "))
              .append(TextComponent.of("everyone").color(NamedTextColor.RED))
              .append(TextComponent.of(" in specified worlds"))
              .build(), sender);


        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("pandora.giveall.receive")) {
                continue;
            }
            if (goodWorlds.isEmpty()) {
                if (badWorlds.contains(player.getWorld())) {
                    continue;
                }
            } else {
                if (!goodWorlds.contains(player.getWorld())) { // whitelist
                    continue;
                }
                if (badWorlds.contains(player.getWorld())) { // With blacklist
                    continue;
                }
            }

            int added = addItem(player, item.clone());
            if (added == 0) {
                Messenger.sendMessage(
                      TextComponent.builder()
                            .content(player.getDisplayName() + "'s inventory was full")
                            .color(NamedTextColor.RED)
                            .build(),
                      sender);
            } else {
                Messenger.sendMessage(
                      TextComponent.builder().color(NamedTextColor.GOLD)
                            .append(TextComponent.of(senderName).color(NamedTextColor.GOLD))
                            .append(TextComponent.of(" has given everyone "))
                            .append(TextComponent.of(item.getAmount()).color(NamedTextColor.RED))
                            .append(TextComponent.of("of"))
                            .append(TextComponent.of(name).color(NamedTextColor.RED))
                            .build(), player);
            }
        }

        return true;
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
            Messenger.sendMessage(
                  TextComponent.builder()
                        .content(e.getMessage())
                        .color(NamedTextColor.GOLD)
                        .build(), sender);
            return true;
        }
    }
}
