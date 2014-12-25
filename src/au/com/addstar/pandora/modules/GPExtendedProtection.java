package au.com.addstar.pandora.modules;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class GPExtendedProtection implements Module, Listener
{
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onInteractArmourStand(PlayerInteractAtEntityEvent event)
	{
		if (!GriefPrevention.instance.claimsEnabledForWorld(event.getPlayer().getWorld()))
			return;
		
		PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName());
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getRightClicked().getLocation(), true, pdata.lastClaim);
		
		if(claim != null)
		{
			String reason = claim.allowBuild(event.getPlayer());
			if (reason != null)
			{
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + reason);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onDestroyArmourStand(EntityDamageByEntityEvent event)
	{
		if (!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld()))
			return;
		
		if (!(event.getEntity() instanceof ArmorStand))
			return;
		
		Claim cached = null;
		Player player = null;
		
		if (event.getDamager() != null)
		{
			if (event.getDamager() instanceof Player)
				player = (Player)event.getDamager();
			else if (event.getDamager() instanceof Projectile)
			{
				Projectile proj = (Projectile)event.getDamager();
				if (proj.getShooter() instanceof Player)
					player = (Player)proj.getShooter();
			}
		}
		
		if (player != null)
			cached = GriefPrevention.instance.dataStore.getPlayerData(player.getName()).lastClaim;
		
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getEntity().getLocation(), false, cached);
		
		if (claim != null)
		{
			if (player == null)
				event.setCancelled(true);
			else
			{
				String reason = claim.allowBuild(player);
				if (reason != null)
				{
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + reason);
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			switch (event.getClickedBlock().getType())
			{
			case WOOD_DOOR:
			case ACACIA_DOOR:
			case SPRUCE_DOOR:
			case BIRCH_DOOR:
			case DARK_OAK_DOOR:
			case JUNGLE_DOOR:
			case FENCE_GATE:
			case ACACIA_FENCE_GATE:
			case SPRUCE_FENCE_GATE:
			case JUNGLE_FENCE_GATE:
			case BIRCH_FENCE_GATE:
			case DARK_OAK_FENCE_GATE:
				WorldConfig config = GriefPrevention.instance.getWorldCfg(player.getWorld());
				
				if (config.getWoodenDoors().Allowed(event.getClickedBlock().getLocation(), player, true, true).Denied())
					event.setCancelled(true);
				
				break;
			default:
				break;
			}
			
			if (event.hasItem() && event.getItem().getType() == Material.ARMOR_STAND)
			{
				Claim cached = GriefPrevention.instance.dataStore.getPlayerData(player.getName()).lastClaim;
				Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getClickedBlock().getLocation(), false, cached);
				if (claim == null) return;
				
				String reason = claim.allowBuild(player);
				if (reason != null)
				{
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + reason);
				}
			}
		}
	}
}
