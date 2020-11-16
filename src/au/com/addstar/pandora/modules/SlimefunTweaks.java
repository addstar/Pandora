package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SlimefunTweaks implements Module, Listener {

    private MasterPlugin mPlugin;
    private Config mConfig;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "SlimefunTweaks.yml"));
    }

    public void disenchantEvent(AutoDisenchantEvent event) {
        ItemStack stack = event.getItem();
        for (Map.Entry<Enchantment, Integer> enchant : stack.getEnchantments().entrySet()) {
            if (enchant.getValue() > mConfig.maxDisenchantLevel) {
                event.setCancelled(true);
                mPlugin.getLogger().warning("Slimefun disenchanting denied: " + enchant.getKey());
                break;
            }
        }
        if (!event.isCancelled()) {
            mPlugin.getLogger().warning("Slimefun disenchanting allowed.");
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Max allowed enchant level for the Slimefun Disenchanter")
        public int maxDisenchantLevel = 6;
    }
}