package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AntiSpawnerChange implements Module, Listener {
    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    }

    @EventHandler(ignoreCancelled = true)
    private void onSpawnerChange(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem()) {
            Block block = event.getClickedBlock();
            ItemStack item = event.getItem();

            if (eggs.contains(item.getType()) && block.getType() == Material.SPAWNER) {
                if (!event.getPlayer().hasPermission("pandora.spawner.change"))
                    event.setUseInteractedBlock(Result.DENY);
            }
        }
    }

    private static List<Material> eggs = new ArrayList<>();

    static {
        eggs.add(Material.ELDER_GUARDIAN_SPAWN_EGG);
        eggs.add(Material.BAT_SPAWN_EGG);
        eggs.add(Material.BLAZE_SPAWN_EGG);
        eggs.add(Material.CAVE_SPIDER_SPAWN_EGG);
        eggs.add(Material.CHICKEN_SPAWN_EGG);
        eggs.add(Material.COD_SPAWN_EGG);
        eggs.add(Material.COW_SPAWN_EGG);
        eggs.add(Material.COW_SPAWN_EGG);
        eggs.add(Material.CREEPER_SPAWN_EGG);
        eggs.add(Material.DOLPHIN_SPAWN_EGG);
        eggs.add(Material.DONKEY_SPAWN_EGG);
        eggs.add(Material.DROWNED_SPAWN_EGG);
        eggs.add(Material.ENDERMAN_SPAWN_EGG);
        eggs.add(Material.ENDERMITE_SPAWN_EGG);
        eggs.add(Material.EVOKER_SPAWN_EGG);
        eggs.add(Material.GHAST_SPAWN_EGG);
        eggs.add(Material.GUARDIAN_SPAWN_EGG);
        eggs.add(Material.HORSE_SPAWN_EGG);
        eggs.add(Material.HUSK_SPAWN_EGG);
        eggs.add(Material.LLAMA_SPAWN_EGG);
        eggs.add(Material.MAGMA_CUBE_SPAWN_EGG);
        eggs.add(Material.MOOSHROOM_SPAWN_EGG);
        eggs.add(Material.MULE_SPAWN_EGG);
        eggs.add(Material.OCELOT_SPAWN_EGG);
        eggs.add(Material.BLAZE_SPAWN_EGG);
        eggs.add(Material.PHANTOM_SPAWN_EGG);
        eggs.add(Material.PARROT_SPAWN_EGG);
        eggs.add(Material.PIG_SPAWN_EGG);
        eggs.add(Material.POLAR_BEAR_SPAWN_EGG);
        eggs.add(Material.PUFFERFISH_SPAWN_EGG);
        eggs.add(Material.PILLAGER_SPAWN_EGG);
        eggs.add(Material.RABBIT_SPAWN_EGG);
        eggs.add(Material.SALMON_SPAWN_EGG);
        eggs.add(Material.SHEEP_SPAWN_EGG);
        eggs.add(Material.SHULKER_SPAWN_EGG);
        eggs.add(Material.SILVERFISH_SPAWN_EGG);
        eggs.add(Material.SKELETON_HORSE_SPAWN_EGG);
        eggs.add(Material.SKELETON_SPAWN_EGG);
        eggs.add(Material.SLIME_SPAWN_EGG);
        eggs.add(Material.SPIDER_SPAWN_EGG);
        eggs.add(Material.SQUID_SPAWN_EGG);
        eggs.add(Material.STRAY_SPAWN_EGG);
        eggs.add(Material.TROPICAL_FISH_SPAWN_EGG);
        eggs.add(Material.TURTLE_SPAWN_EGG);
        eggs.add(Material.VEX_SPAWN_EGG);
        eggs.add(Material.VILLAGER_SPAWN_EGG);
        eggs.add(Material.VINDICATOR_SPAWN_EGG);
        eggs.add(Material.WITCH_SPAWN_EGG);
        eggs.add(Material.WITHER_SKELETON_SPAWN_EGG);
        eggs.add(Material.WOLF_SPAWN_EGG);
        eggs.add(Material.ZOMBIE_HORSE_SPAWN_EGG);
        eggs.add(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG);
        eggs.add(Material.ZOMBIE_SPAWN_EGG);
        eggs.add(Material.ZOMBIE_VILLAGER_SPAWN_EGG);
        eggs.add(Material.FOX_SPAWN_EGG);
        eggs.add(Material.HOGLIN_SPAWN_EGG);
        eggs.add(Material.PIGLIN_SPAWN_EGG);
        eggs.add(Material.STRIDER_SPAWN_EGG);
        eggs.add(Material.WANDERING_TRADER_SPAWN_EGG);
        eggs.add(Material.BEE_SPAWN_EGG);
        eggs.add(Material.COD_SPAWN_EGG);
        eggs.add(Material.RAVAGER_SPAWN_EGG);
        eggs.add(Material.ZOGLIN_SPAWN_EGG);
    }

}
