package au.com.addstar.pandora.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class FlyCanceller implements Module, Listener {
    @EventHandler(ignoreCancelled = true)
    private void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        if (event.getPlayer().hasPermission("pandora.flycancel.bypass"))
            return;

        event.getPlayer().setAllowFlight(false);
        event.getPlayer().setFlying(false);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    }

}
