package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import me.evilterabite.rplace.events.player.*;
import org.dynmap.DynmapAPI;

public class RPlaceDynmap implements Module, Listener {
    private MasterPlugin mPlugin;
    private DynmapAPI dynmap;

    @Override
    public void onEnable() {
        dynmap = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("Dynmap");
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    private void updatePixel(Location l) {
        /*Bukkit.getLogger().warning("Triggering block update for: "
                + l.getWorld().getName() + " "
                + l.getBlockX() + ","
                + l.getBlockY() + ","
                + l.getBlockZ());*/
        dynmap.triggerRenderOfBlock(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPixelCreate(PlayerCreatePixelEvent e) {
        updatePixel(e.getPlayer().getLocation());
    }
}
