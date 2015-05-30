package au.com.addstar.pandora.modules;

import java.util.ArrayList;
import java.util.List;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

public class TpClaim implements Module, CommandExecutor, TabCompleter
{
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		plugin.getCommand("tpClaim").setExecutor(this);
		plugin.getCommand("tpClaim").setTabCompleter(this);
	}

	private void doTeleport(Player player, Claim claim)
	{
		Location center = claim.getLesserBoundaryCorner().toVector().add(claim.getGreaterBoundaryCorner().toVector()).multiply(0.5).toLocation(claim.getLesserBoundaryCorner().getWorld());
		
		center = center.getWorld().getHighestBlockAt(center).getLocation();
		if(!Utilities.safeTeleport(player, center))
			player.sendMessage(ChatColor.RED + "There is nowhere safe to teleport you");
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("This command can only be called by players.");
			return true;
		}
		
		Player psender = (Player)sender;
		
		boolean canSpecifyWorld = sender.hasPermission("pandora.tpclaim.worlds");

		if(args.length != 1 && args.length != 2)
		{
			if (canSpecifyWorld)
				return false;
			else
			{
				sender.sendMessage("/" + label + " <player> [<index>]");
				return true;
			}
		}
		
		OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
		if(!player.hasPlayedBefore())
		{
			player = Bukkit.getPlayer(args[0]);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "No player by that name exists.");
				return true;
			}
		}
		
		List<Claim> claims = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).getClaims();
		
		World world = (canSpecifyWorld ? null : psender.getWorld());
		int index = -1;
		
		if(args.length == 2)
		{
			if (canSpecifyWorld)
			{
				if(args[1].contains(":"))
				{
					world = Bukkit.getWorld(args[1].split(":")[0]);
					if(world == null)
					{
						sender.sendMessage(ChatColor.RED + args[1].split(":")[0] + " is not a valid world.");
						return true;
					}
					
					try
					{
						index = Integer.parseInt(args[1].split(":")[1]);
						
						if(index <= 0)
						{
							sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
							return true;
						}
					}
					catch(NumberFormatException e)
					{
						sender.sendMessage(ChatColor.RED + args[1].split(":")[1] + " is not a valid index.");
						return true;
					}
				}
				else
				{
					world = Bukkit.getWorld(args[1]);
					
					if(world == null)
					{
						try
						{
							index = Integer.parseInt(args[1]);
							
							if(index <= 0)
							{
								sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
								return true;
							}
						}
						catch(NumberFormatException e)
						{
							sender.sendMessage(ChatColor.RED + args[1] + " is not a valid index or world.");
							return true;
						}
					}
				}
			}
			else
			{
				try
				{
					index = Integer.parseInt(args[1]);
					
					if(index <= 0)
					{
						sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
						return true;
					}
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + args[1] + " is not a valid index.");
					return true;
				}
			}
			
		}
		
		if(world != null)
		{
			final World fWorld = world;
			claims = Lists.newArrayList(Iterables.filter(claims, new Predicate<Claim>()
			{
				@Override
				public boolean apply( Claim claim )
				{
					return (claim.getLesserBoundaryCorner().getWorld().equals(fWorld));
				}
			}));
		}
		
		if(claims.isEmpty())
		{
			if(world == null || !canSpecifyWorld)
				sender.sendMessage(ChatColor.RED + player.getName() + " has no claims.");
			else
				sender.sendMessage(ChatColor.RED + player.getName() + " has no claims in " + world.getName() + ".");
			return true;
		}
		
		if(index > claims.size())
		{
			sender.sendMessage(ChatColor.RED + "There are only " + claims.size() + " claims.");
			return true;
		}
		
		if(claims.size() == 1)
		{
			doTeleport(psender, claims.get(0));
			return true;
		}
		
		if(index == -1)
		{
			if(world == null || !canSpecifyWorld)
				sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + player.getName() + ":");
			else
				sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + player.getName() + " in " + world.getName() + ":");
			
			sender.sendMessage(ChatColor.WHITE + ChatColor.ITALIC.toString() + " Your level of access is displayed in the square brackets []");
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&f&o Use &e/%s %s <number> &f&oto teleport to a claim.\n&7&o <number>&f&o is the number to the left of the claim to teleport to.", label, args[0])));
			
			// Display claims
			for(int i = 0; i < claims.size(); ++i)
			{
				Claim claim = claims.get(i);
				
				Location center = claim.getLesserBoundaryCorner().toVector().add(claim.getGreaterBoundaryCorner().toVector()).multiply(0.5).toLocation(claim.getLesserBoundaryCorner().getWorld());
				
				// Access level
				String level = ChatColor.RED + "None";
				
				boolean access = claim.allowAccess(psender) == null;
				boolean trust = claim.allowBuild(psender, Material.STONE) == null;
				boolean farmtrust = claim.allowBuild(psender, Material.CROPS) == null;
				boolean containers = claim.allowContainers(psender) == null;
				boolean manage = claim.allowGrantPermission(psender) == null;
				
				if (access && trust && containers && manage)
					level = ChatColor.GREEN + "Full";
				else if (trust)
					level = ChatColor.YELLOW + "Trusted";
				else if (farmtrust)
					level = ChatColor.YELLOW + "Farm Trust";
				else if (access || containers)
					level = ChatColor.GOLD + "Limited";

				if(world == null)
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %s, %d, %d, %d&7 Area: %d &7[%s&7]", i+1, center.getWorld().getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
				else
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %d, %d, %d&7 Area: %d &7[%s&7]", i+1, center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
			}
		}
		else
		{
			Claim claim = claims.get(index - 1);
			doTeleport(psender, claim);
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1)
		{
			ArrayList<String> playerNames;
			List<Player> players = Bukkit.matchPlayer(args[0]);
			
			playerNames = new ArrayList<String>(players.size());
			for(Player player : players)
				playerNames.add(player.getName());
			
			return playerNames;
		}
		
		return null;
	}

}
