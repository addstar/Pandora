package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.util.ItemUtil;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 13/09/2015.
 */
public class ItemMetaReporter implements Module, CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        Player sender;
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("&cPlayer only command.");
            return false;
        } else {
            sender = (Player) commandSender;
        }

        ItemStack item = sender.getInventory().getItemInMainHand();
        sender.sendMessage(ChatColor.GOLD + "Item Name: " + ChatColor.RED + Utilities.getName(item));
        sender.sendMessage(ChatColor.GOLD + "Item Type: " + ChatColor.RED + item.getType().toString());
        String mcName = Lookup.findMinecraftNameByItem(item.getType());
        if (mcName != null)
            sender.sendMessage(ChatColor.GOLD + "Minecraft Name: " + ChatColor.RED + mcName);
        ItemMeta imeta = item.getItemMeta();
        if (imeta instanceof Damageable) {
            int maxDura = item.getType().getMaxDurability();
            int uses = maxDura + 1 - ((Damageable) imeta).getDamage();
            sender.sendMessage(ChatColor.GOLD + "Durability: " + ChatColor.RED + ((Damageable) imeta).getDamage() + " / " + (maxDura + 1) + " (" + uses + " uses)");
        }
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            StringBuilder msg = new StringBuilder(" **Potion** \n");
            PotionMeta meta = (PotionMeta) imeta;
            if (meta != null) {
                msg.append(" Main type: ");
                if (meta.getCustomEffects().get(0) != null) {
                    msg.append(meta.getCustomEffects().get(0).getType().getName());
                    msg.append("  Strength: ")
                            .append(meta.getCustomEffects().get(0).getAmplifier()).append(" Dur Mod: ").append(meta.getCustomEffects().
                            get(0).getDuration());
                    if (item.getType() == Material.SPLASH_POTION) msg.append(" Splash Potion: YES");
                    if (item.getType() == Material.LINGERING_POTION) msg.append(" Lingering Potion: YES");
                } else {
                    msg.append("  Custom Potion - could not be cast to a real potion.");
                }
                if (meta.getCustomEffects() != null) {
                    for (PotionEffect e : meta.getCustomEffects()) {
                        msg.append("  Subtype: ").append(e.getType().toString()).append(" Strength: ").append(e.getAmplifier()).append(" Duration: ").append(e.getDuration());
                    }
                }
            }

            sender.sendMessage(ChatColor.GOLD + msg.toString());

        }

        if (item.hasItemMeta()) {
            if (imeta.hasLore()) {
                for (String slore : imeta.getLore()) {
                    sender.sendMessage(ChatColor.GOLD + "Lore: " + slore);
                }
            }
            if (imeta.hasDisplayName()) sender.sendMessage(ChatColor.GOLD + "DisplayName: " + imeta.getDisplayName());
            if (imeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : imeta.getEnchants().entrySet()) {
                    sender.sendMessage(ChatColor.GOLD + "Enchantment: " + entry.toString() + " Level: " + entry.getValue());
                }
            }
            Set<ItemFlag> flags = imeta.getItemFlags();
            if (flags != null && flags.size() > 0) {
                StringBuilder msg = new StringBuilder("Flags: ");
                for (ItemFlag flag : imeta.getItemFlags()) {
                    msg.append(flag.toString());
                    msg.append(" ");
                }
                sender.sendMessage(ChatColor.GOLD + msg.toString());
            }
            if (imeta instanceof PotionMeta) {
                PotionMeta pmeta = (PotionMeta) imeta;
                StringBuilder msg = new StringBuilder();
                for (PotionEffect e : pmeta.getCustomEffects()) {
                    msg.append("  Meta Subtype: ").append(e.getType().toString()).append(" Strength: ").append(e.getAmplifier()).append(" Duration: ").append(e.getDuration());
                }
                sender.sendMessage(msg.toString());
            }

            sender.sendMessage(ChatColor.GOLD + "  Raw META: " + imeta.toString()); //raw imeta data dump
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("nbt")) {
                String nbtString =  ItemUtil.getItemNbtString(item);
                sender.sendMessage(ChatColor.RED + "NBT Tag: " + ChatColor.GOLD + nbtString);
            }
        }

        return true;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        plugin.getCommand("itemmeta").setExecutor(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
