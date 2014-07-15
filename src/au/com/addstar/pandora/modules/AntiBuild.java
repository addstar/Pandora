package au.com.addstar.pandora.modules;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class AntiBuild implements Module, Listener {
	
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
	
	private void checkBuildPerms(Cancellable event, Player player) {
		if (!player.hasPermission("pandora.antibuild.bypass")) {
			event.setCancelled(true);	
			player.sendMessage(ChatColor.RED + "[AntiBuild] You do not have permission to build here");
		}
	}	
		
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		checkBuildPerms(event, event.getPlayer());	
	}		
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		checkBuildPerms(event, event.getPlayer());			        
	} 	
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onHangingBreak(HangingBreakByEntityEvent event) {	
		Entity entity = event.getRemover();		
		if ((entity instanceof Player)) {		   
			checkBuildPerms(event, (Player) entity);
		}	        
	} 	
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		checkBuildPerms(event, event.getPlayer());
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerPickUpItem(PlayerPickupItemEvent event) {
		checkBuildPerms(event, event.getPlayer());
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		checkBuildPerms(event, event.getPlayer());
	}

}