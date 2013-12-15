package au.com.addstar.pandora.modules;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimBeforeCreateEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class GPWGInterop implements Module, Listener
{
	private WorldGuardPlugin mWorldGuard;
	
	private boolean hasIntersections(Claim claim)
	{
		RegionManager manager = mWorldGuard.getRegionManager(claim.getLesserBoundaryCorner().getWorld());
		
		if(manager == null)
			return false;
		
		ProtectedRegion checkRegion = new ProtectedCuboidRegion("blah", new BlockVector(claim.getLesserBoundaryCorner().getX(), claim.getLesserBoundaryCorner().getY(), claim.getLesserBoundaryCorner().getZ()), new BlockVector(claim.getGreaterBoundaryCorner().getX(), claim.getGreaterBoundaryCorner().getY(), claim.getGreaterBoundaryCorner().getZ()));
		ApplicableRegionSet set = manager.getApplicableRegions(checkRegion);

		return set.size() > 0;
		
	}
	
	@EventHandler(ignoreCancelled=true)
	private void onClaim(ClaimResizeEvent event)
	{
		if(GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName()).ignoreClaims)
			return;
		
		RegionManager manager = mWorldGuard.getRegionManager(event.getNewGreaterBoundaryCorner().getWorld());
		
		if(manager == null)
			return;
		
		ProtectedRegion checkRegion = new ProtectedCuboidRegion("blah", new BlockVector(event.getNewLesserBoundaryCorner().getX(), event.getNewLesserBoundaryCorner().getY(), event.getNewLesserBoundaryCorner().getZ()), new BlockVector(event.getNewGreaterBoundaryCorner().getX(), event.getNewGreaterBoundaryCorner().getY(), event.getNewGreaterBoundaryCorner().getZ()));
		ApplicableRegionSet set = manager.getApplicableRegions(checkRegion);
		if(set.size() > 0)
		{
			event.setCancelled(true);
			event.getResizer().sendMessage(ChatColor.RED + "You cannot resize the claim to here");
		}
		
	}
	
	@EventHandler(ignoreCancelled=true)
	private void onClaim(ClaimBeforeCreateEvent event)
	{
		if(GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName()).ignoreClaims)
			return;
		
		if(hasIntersections(event.getClaim()))
		{
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot create a claim here");
		}
	}
	
	@Override
	public void onEnable()
	{
		mWorldGuard = (WorldGuardPlugin)Bukkit.getPluginManager().getPlugin("WorldGuard");
	}

	@Override
	public void onDisable()
	{
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
	}

}
