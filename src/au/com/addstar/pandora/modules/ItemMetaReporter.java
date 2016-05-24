package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
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
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("&cPlayer only command.");
        }
        Player sender = (Player) commandSender;

        ItemStack item = sender.getInventory().getItemInMainHand();
        MaterialDefinition def = MaterialDefinition.from(item);
        sender.sendMessage(ChatColor.GOLD +"Item Name: " +ChatColor.RED+ StringTranslator.getName(item)+ ChatColor.RED + def.getMaterial().getId() + ":" + def.getData());;
        sender.sendMessage(ChatColor.GOLD +"Item Type: "+ ChatColor.RED + item.getType().toString());
        MaterialData mdata = item.getData();
        String mcName = Lookup.findMinecraftNameByItem(item.getType());
        if(mcName != null)
            sender.sendMessage(ChatColor.GOLD + "Minecraft Name: " + ChatColor.RED + mcName);
        sender.sendMessage(ChatColor.GOLD +"Bukkit Name: "+ ChatColor.RED+ mdata.getItemType());
        Short dur = item.getDurability();
        Short maxdur = item.getType().getMaxDurability();
        if (maxdur > 0){
            sender.sendMessage(ChatColor.GOLD + " Durability: "+ ChatColor.RED + (maxdur-dur) + " / " + maxdur);
        }
        if(item.getType() == Material.POTION){
            StringBuilder msg = new StringBuilder(" **Potion** \n");
            Potion potion = Potion.fromItemStack(item);
            if (potion != null) {
                msg.append(" Main type: ");
                if (potion.getType() != null) {
                    msg.append(potion.getType().getEffectType());
                    msg.append("  Strength: ")
                            .append(potion.getLevel()).append(" Dur Mod: ").append(potion.getType().getEffectType()
                            .getDurationModifier()).append("");
                    if (potion.isSplash()) msg.append(" Splash Potion: YES");
                } else {
                    msg.append("  Custom Potion - could not be cast to a real potion.");
                }
            }
            for(PotionEffect e : potion.getEffects()) {
                msg.append( "  Subtype: ").append(e.getType().toString()).append(" Strength: ").append(e.getAmplifier()).append(" Duration: ").append(e.getDuration());
            }
            sender.sendMessage(ChatColor.GOLD + msg.toString());

        }

        if(item.hasItemMeta()){
            ItemMeta imeta = item.getItemMeta();
            if(imeta.hasLore()) {
                for (String slore : imeta.getLore()) {
                     sender.sendMessage(ChatColor.GOLD+"Lore: " + slore);
                }
            }
            if (imeta.hasDisplayName()) sender.sendMessage(ChatColor.GOLD +"DisplayName: "+imeta.getDisplayName());
            if (imeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : imeta.getEnchants().entrySet()) {
                    sender.sendMessage(ChatColor.GOLD +"Enchantment: " + entry.getKey().getName() + " Level: " + entry.getValue());
                }
            }
            Set<ItemFlag> flags = imeta.getItemFlags();
            if(flags != null && flags.size() > 0){
                StringBuilder msg = new StringBuilder("Flags: ");
                for(ItemFlag flag : imeta.getItemFlags()) {
                    msg.append(flag.toString());
                    msg.append(" ");
                }
                sender.sendMessage(ChatColor.GOLD + msg.toString());
            }
            if(imeta instanceof PotionMeta){
                PotionMeta pmeta = (PotionMeta) imeta;
                StringBuilder msg = new StringBuilder();
                for(PotionEffect e : pmeta.getCustomEffects()) {
                     msg.append("  Meta Subtype: ").append(e.getType().toString()).append(" Strength: ").append(e.getAmplifier()).append(" Duration: ").append(e.getDuration());
                }
                sender.sendMessage(msg.toString());
            }

            sender.sendMessage(ChatColor.GOLD  +"  Raw META: " + imeta.toString()); //raw imeta data dump
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
