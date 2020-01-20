package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class ItemDB implements Module, CommandExecutor {
    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        plugin.getCommand("itemdb").setExecutor(this);
    }

    @SuppressWarnings("deprecation")
    private Material getMaterial(String name) {
        // Bukkit name
        Material mat = Material.getMaterial(name.toUpperCase());
        if (mat != null)
            return mat;
        //ItemDb
        return Lookup.findItemByName(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 2)
            return false;

        Material def = null;
        if (args.length == 0) {
            if (!(sender instanceof Player))
                return false;

            Player player = (Player) sender;
            if (player.getInventory().getItemInMainHand() != null) {
                Set<String> names = Lookup.findNameByItem(player.getInventory().getItemInMainHand().getType());
                def = player.getInventory().getItemInMainHand().getType();
            } else
                def = Material.AIR;
        } else {
            def = Lookup.findByMinecraftName(args[0]);

        }
        if (def == null) {
            sender.sendMessage(ChatColor.RED + "Unknown material " + args[0]);
            return true;
        }
        ItemStack item = new ItemStack(def, 1);
        sender.sendMessage(ChatColor.GOLD + "Item: " + ChatColor.RED + item.getType().getKey() + " : " + item.getType().name());

        String mcName = Lookup.findMinecraftNameByItem(item.getType());
        sender.sendMessage(ChatColor.GOLD + "Minecraft Name: " + ChatColor.RED + mcName);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            int maxDura = item.getType().getMaxDurability();
            int uses = maxDura + 1 - ((Damageable) meta).getDamage();
            sender.sendMessage(ChatColor.GOLD + "Durability: " + ChatColor.RED + ((Damageable) meta).getDamage() + " / " + (maxDura + 1) + " (" + uses + " uses)");
        }
        Set<String> names = Lookup.findNameByItem(def);
        if (!names.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "Item short names: " + ChatColor.WHITE + StringUtils.join(names, ", "));

        return true;
    }

}
