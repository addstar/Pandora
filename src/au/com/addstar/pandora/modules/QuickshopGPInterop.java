package au.com.addstar.pandora.modules;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maxgamer.QuickShop.Shop.ShopCreateEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class QuickshopGPInterop implements Module, Listener
{
	@EventHandler(priority=EventPriority.HIGH)
	private void onShopCreate(ShopCreateEvent event)
	{
		if(event.getPlayer().hasPermission("quickshop.allowinclaims"))
			return;
		
		PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName());
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getShop().getLocation(), true, pdata.lastClaim);
		
		if(claim != null)
		{
			String reason = claim.allowBuild(event.getPlayer());
			
			if(reason != null)
			{
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + reason);
			}
		}
	}
	
	@Override
	public void onEnable() {}

	@Override
	public void onDisable()	{}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

}
