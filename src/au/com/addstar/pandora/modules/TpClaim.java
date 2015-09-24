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
import au.com.addstar.pandora.modules.gp.GPClaimData;

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
		
		plugin.getCommand("claimslist").setExecutor(this);
		plugin.getCommand("claimslist").setTabCompleter(this);
	}

	private void doTeleport(Player player, Claim claim, GPClaimData data)
	{
		Location location;
		if (data.getTeleport() != null)
			location = data.getTeleport();
		else
		{
			Location center = claim.getLesserBoundaryCorner().toVector().add(claim.getGreaterBoundaryCorner().toVector()).multiply(0.5).toLocation(claim.getLesserBoundaryCorner().getWorld());
			
			location = center.getWorld().getHighestBlockAt(center).getLocation();
		}
		
		if(!Utilities.safeTeleport(player, location))
			player.sendMessage(ChatColor.RED + "There is nowhere safe to teleport you");
		else
		{
			if (player.getUniqueId().equals(claim.ownerID))
				player.sendMessage(ChatColor.GREEN + "You have been teleported to your claim");
			else
				player.sendMessage(ChatColor.GREEN + "You have been teleported to " + claim.getOwnerName() + "'s claim");
		}
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if (command.getName().equals("tpclaim"))
			return onTPClaim(sender, label, args);
		else if (command.getName().equals("claimslist"))
			return onClaimsList(sender, label, args);
		else
			return false;
	}
	
	private boolean onTPClaim(CommandSender sender, String label, String[] args)
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("This command can only be called by players.");
			return true;
		}
		
		Player psender = (Player)sender;
		
		boolean canSpecifyWorld = sender.hasPermission("pandora.tpclaim.worlds");
		
		// Check syntax error
		if(args.length > 2)
		{
			if (canSpecifyWorld)
			{
				sender.sendMessage("/" + label + " <player> [<index>|<name>|<world>|<world>:<index>]");
				sender.sendMessage("or");
				sender.sendMessage("/" + label + " [<index>|<name>|<world>|<world>:<index>]");
				return true;
			}
			else
			{
				sender.sendMessage("/" + label + " <player> [<index>|<name>]");
				sender.sendMessage("or");
				sender.sendMessage("/" + label + " [<index>|<name>]");
				return true;
			}
		}
		
		// Determine the index / world and player
		World world = (canSpecifyWorld ? null : psender.getWorld());
		OfflinePlayer player = psender;
		int index = -1;
		String claimName = null;
		
		// In the case of one argument, it could either be the player name, index, world, or world index pair
		// Find out which
		boolean hasIndex = false;
		if (args.length == 1)
		{
			if (args[0].matches("^[0-9]+$") && args[0].length() < 4) // MC usernames have a minimum length of 4 chars
				hasIndex = true;
			else if (args[0].contains(":"))
				hasIndex = true;
			else if (Bukkit.getWorld(args[0]) != null)
				hasIndex = true;
		}
		else if (args.length == 2)
			hasIndex = true;
		
		// Parse index information
		if(hasIndex)
		{
			if (canSpecifyWorld)
			{
				if(args[args.length-1].contains(":"))
				{
					world = Bukkit.getWorld(args[args.length-1].split(":")[0]);
					if(world == null)
					{
						sender.sendMessage(ChatColor.RED + args[args.length-1].split(":")[0] + " is not a valid world.");
						return true;
					}
					
					try
					{
						index = Integer.parseInt(args[args.length-1].split(":")[1]);
						
						if(index <= 0)
						{
							sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
							return true;
						}
					}
					catch(NumberFormatException e)
					{
						sender.sendMessage(ChatColor.RED + args[args.length-1].split(":")[1] + " is not a valid index.");
						return true;
					}
				}
				else
				{
					world = Bukkit.getWorld(args[args.length-1]);
					claimName = args[args.length-1];
					
					if(world == null)
					{
						try
						{
							index = Integer.parseInt(args[args.length-1]);
							
							if(index <= 0)
							{
								sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
								return true;
							}
						}
						catch(NumberFormatException e)
						{
						}
					}
				}
			}
			else
			{
				try
				{
					index = Integer.parseInt(args[args.length-1]);
					
					if(index <= 0)
					{
						sender.sendMessage(ChatColor.RED + "" + index + " is not a valid index.");
						return true;
					}
				}
				catch(NumberFormatException e)
				{
					claimName = args[args.length-1];
				}
			}
		}
		
		// Parse player
		if (args.length == 2 || (args.length == 1 && !hasIndex))
		{
			player = Bukkit.getOfflinePlayer(args[0]);
			if(!player.hasPlayedBefore())
			{
				player = Bukkit.getPlayer(args[0]);
				if(player == null)
				{
					if (args.length != 1)
					{
						sender.sendMessage(ChatColor.RED + "No player by that name exists.");
						return true;
					}
					else
					{
						player = psender;
						claimName = args[0];
					}
				}
			}
		}
		
		List<Claim> claims = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).getClaims();
		
		// For players that can specify a world, I want teleport to named claim to override world listing
		if (canSpecifyWorld && world != null && claimName != null && index == -1)
		{
			List<GPClaimData> data = GPExtended.getClaimManager().getData(claims);
			for (GPClaimData d : data)
			{
				if (d.getName() != null && d.getName().equalsIgnoreCase(claimName))
				{
					// Override the world
					world = null;
					break;
				}
			}
		}
		
		// Limit by world
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
		
		List<GPClaimData> data = GPExtended.getClaimManager().getData(claims);
		
		if(claims.isEmpty())
		{
			if (player.equals(sender))
			{
				if(world == null || !canSpecifyWorld)
					sender.sendMessage(ChatColor.RED + "You have no claims.");
				else
					sender.sendMessage(ChatColor.RED + "You have no claims in " + world.getName() + ".");
			}
			else
			{
				if(world == null || !canSpecifyWorld)
					sender.sendMessage(ChatColor.RED + player.getName() + " has no claims.");
				else
					sender.sendMessage(ChatColor.RED + player.getName() + " has no claims in " + world.getName() + ".");
			}
			return true;
		}
		
		if(index > claims.size())
		{
			if (player.equals(sender))
				sender.sendMessage(ChatColor.RED + "You only have " + claims.size() + " claims.");
			else
				sender.sendMessage(ChatColor.RED + player.getName() + " only has " + claims.size() + " claims.");
			return true;
		}
		
		// Handle named claims now
		if (claimName != null)
		{
			for (GPClaimData claimData : data)
			{
				if (claimData.getName() != null && claimName.equalsIgnoreCase(claimData.getName()))
				{
					doTeleport(psender, claimData.getClaim(), claimData);
					return true;
				}
			}
			
			if (player.equals(sender))
				sender.sendMessage(ChatColor.RED + "You do not have a claim named '" + claimName + "'");
			else
				sender.sendMessage(ChatColor.RED + player.getName() + " does not have a claim named '" + claimName + "'");
			return true;
		}
		
		if(claims.size() == 1)
		{
			doTeleport(psender, claims.get(0), data.get(0));
			return true;
		}
		
		// Display all claims
		if(index == -1)
		{
			if (player.equals(sender))
			{
				if(world == null || !canSpecifyWorld)
					sender.sendMessage(ChatColor.YELLOW + "Displaying your claims:");
				else
					sender.sendMessage(ChatColor.YELLOW + "Displaying your claims in " + world.getName() + ":");
			}
			else
			{
				if(world == null || !canSpecifyWorld)
					sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + player.getName() + ":");
				else
					sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + player.getName() + " in " + world.getName() + ":");
			}
			
			sender.sendMessage(ChatColor.WHITE + ChatColor.ITALIC.toString() + " Your level of access is displayed in the square brackets []");
			if (player.equals(sender))
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&f&o Use &e/%s <number> &f&oto teleport to a claim.\n&7&o <number>&f&o is the number to the left of the claim to teleport to.", label)));
			else
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&f&o Use &e/%s %s <number> &f&oto teleport to a claim.\n&7&o <number>&f&o is the number to the left of the claim to teleport to.", label, args[0])));
			
			// Display claims
			displayClaims(sender, claims, data, player, world, canSpecifyWorld);
		}
		else
		{
			Claim claim = claims.get(index - 1);
			doTeleport(psender, claim, data.get(index - 1));
		}
		
		return true;
	}
	
	private boolean onClaimsList(CommandSender sender, String label, String[] args)
	{
		boolean canSpecifyWorld = sender.hasPermission("pandora.tpclaim.worlds");
		
		// Syntax check
		if (canSpecifyWorld && args.length > 2)
		{
			sender.sendMessage("/" + label + " [<player> [<world>]]");
			return true;
		}
		else if (!canSpecifyWorld && args.length > 1)
		{
			sender.sendMessage("/" + label + " [<player>]");
			return true;
		}
		
		OfflinePlayer target;
		World world = null;
		
		// Sender check and get player
		if (args.length == 0)
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(ChatColor.RED + "You need to be in game to list your claims");
				return true;
			}
			
			target = (Player)sender;
			
			if (!canSpecifyWorld)
				world = ((Player)sender).getWorld();
		}
		else
		{
			target = Bukkit.getOfflinePlayer(args[0]);
			if(!target.hasPlayedBefore())
			{
				target = Bukkit.getPlayer(args[0]);
				if(target == null)
				{
					sender.sendMessage(ChatColor.RED + "No player by that name exists.");
					return true;
				}
			}
		}
		
		// World
		if (args.length == 2) // Will have to have canSpecifyWorld at this point
		{
			world = Bukkit.getWorld(args[1]);
			
			if (world == null)
			{
				sender.sendMessage(ChatColor.RED + args[1] + " is not a valid world.");
				return true;
			}
		}
		
		// Get all claims
		List<Claim> claims = GriefPrevention.instance.dataStore.getPlayerData(target.getUniqueId()).getClaims();
		
		// Limit by world
		if (world != null)
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
		
		List<GPClaimData> data = GPExtended.getClaimManager().getData(claims);
		
		// Display
		displayClaims(sender, claims, data, target, world, canSpecifyWorld);
		return true;
	}
	
	private void displayClaims(CommandSender sender, List<Claim> claims, List<GPClaimData> data, OfflinePlayer target, World world, boolean canSpecifyWorld)
	{
		if (target.equals(sender))
		{
			if (world == null || !canSpecifyWorld)
				sender.sendMessage(ChatColor.YELLOW + "Displaying your claims:");
			else
				sender.sendMessage(ChatColor.YELLOW + "Displaying your claims in " + world.getName() + ":");
		}
		else
		{
			if (world == null || !canSpecifyWorld)
				sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + target.getName() + ":");
			else
				sender.sendMessage(ChatColor.YELLOW + "Displaying claims for " + target.getName() + " in " + world.getName() + ":");
		}
		
		if (sender instanceof Player)
			sender.sendMessage(ChatColor.WHITE + ChatColor.ITALIC.toString() + " Your level of access is displayed in the square brackets []");
		
		if (target.equals(sender))
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&f&o Use &e/tpclaim <number> &f&oor &e<name> &f&oto teleport to a claim.\n&7&o <number>&f&o is the number to the left of the claim to teleport to.")));
		else
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&f&o Use &e/tpclaim %s <number> &f&oor &e<name> &f&oto teleport to a claim.\n&7&o <number>&f&o is the number to the left of the claim to teleport to.", target.getName())));
		
		// Display claims
		for (int i = 0; i < claims.size(); ++i)
		{
			Claim claim = claims.get(i);
			GPClaimData d = data.get(i);
			
			Location center = claim.getLesserBoundaryCorner().toVector().add(claim.getGreaterBoundaryCorner().toVector()).multiply(0.5).toLocation(claim.getLesserBoundaryCorner().getWorld());
			
			if (sender instanceof Player)
			{
				// Access level
				String level = ChatColor.RED + "None";
				Player psender = (Player)sender;
				boolean access = claim.allowAccess(psender) == null;
				boolean trust = claim.allowBuild(psender, Material.STONE) == null;
				boolean containers = claim.allowContainers(psender) == null;
				boolean manage = claim.allowGrantPermission(psender) == null;
				
				if (access && trust && containers && manage)
					level = ChatColor.GREEN + "Full";
				else if (trust)
					level = ChatColor.YELLOW + "Trusted";
				else if (access || containers)
					level = ChatColor.GOLD + "Limited";
	
				if (d.getName() != null)
				{
					if(world == null)
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d %s&7: %s, %d, %d, %d&7 Area: %d &7[%s&7]", i+1, d.getName(), center.getWorld().getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
					else
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d %s&7: %d, %d, %d&7 Area: %d &7[%s&7]", i+1, d.getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
				}
				else
				{
					if(world == null)
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %s, %d, %d, %d&7 Area: %d &7[%s&7]", i+1, center.getWorld().getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
					else
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %d, %d, %d&7 Area: %d &7[%s&7]", i+1, center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea(), level)));
				}
			}
			else
			{
				if (d.getName() != null)
				{
					if(world == null)
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d %s&7: %s, %d, %d, %d&7 Area: %d", i+1, d.getName(), center.getWorld().getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea())));
					else
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d %s&7: %d, %d, %d&7 Area: %d", i+1, d.getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea())));
				}
				else
				{
					if(world == null)
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %s, %d, %d, %d&7 Area: %d", i+1, center.getWorld().getName(), center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea())));
					else
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6%d&7: %d, %d, %d&7 Area: %d", i+1, center.getBlockX(), center.getBlockY(), center.getBlockZ(), claim.getArea())));
				}
			}
		}
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
