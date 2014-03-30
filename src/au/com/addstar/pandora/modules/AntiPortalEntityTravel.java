package au.com.addstar.pandora.modules;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class AntiPortalEntityTravel implements Module, Listener {
	
	private MasterPlugin mPlugin;
	
	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		mPlugin = plugin;
	}
	
	@EventHandler
	private void playerEnterPortal(EntityPortalEvent event) {
		Location l = event.getFrom();
		if (!(event.getEntityType().isAlive())) {
			System.out.println("Blocked portal entity travel at: " + l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ());
			event.setCancelled(true);
		}
	}
}