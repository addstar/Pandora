package au.com.addstar.pandora.modules;

import java.util.Arrays;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;
import au.com.addstar.pandora.modules.gp.GPClaimData;
import au.com.addstar.pandora.modules.gp.GPClaimManager;

public class GPExtended implements Module, CommandExecutor, TabExecutor
{
	private static GPExtended instance;
	private GPClaimManager claims;
	private MasterPlugin plugin;
	private final int maxNameSize = 20;
	
	public static GPClaimManager getClaimManager()
	{
		if (instance != null)
			return instance.claims;
		else
			return null;
	}
	
	@Override
	public void onEnable()
	{
		claims = new GPClaimManager(plugin.getDataFolder());
		Bukkit.getPluginManager().registerEvents(claims, plugin);
		
		plugin.getCommand("gpset").setExecutor(this);
		instance = this;
	}

	@Override
	public void onDisable()
	{
		instance = null;
		HandlerList.unregisterAll(claims);
		claims = null;
		plugin.getCommand("gpset").setExecutor(null);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "Only players can use this command");
			return true;
		}
		
		Player player = (Player)sender;
		
		if (args.length == 0)
		{
			sender.sendMessage("/" + label + " name <name>");
			sender.sendMessage("/" + label + " teleport");
			return true;
		}
		
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
		
		if (claim == null)
		{
			sender.sendMessage(ChatColor.RED + "You need to stand inside your claim to use this command");
			return true;
		}
		
		// Check claim permissions
		String error;
		if ((error = claim.allowGrantPermission(player)) != null)
		{
			sender.sendMessage(ChatColor.RED + error);
			return true;
		}
		
		if ((error = claim.allowEdit(player)) != null)
		{
			sender.sendMessage(ChatColor.RED + error);
			return true;
		}
		
		GPClaimData data = claims.getData(claim);
		
		if (args[0].equalsIgnoreCase("name"))
		{
			if (args.length != 2)
			{
				if (data.getName() != null)
					sender.sendMessage(ChatColor.WHITE + "This claim is named " + ChatColor.YELLOW + data.getName());
				else
					sender.sendMessage(ChatColor.WHITE + "This claim is not named.");
				
				sender.sendMessage(ChatColor.WHITE + "To change this name, use " + ChatColor.YELLOW + "/" + label + " name <newname>");
			}
			else
			{
				String name = args[1];
				
				// Check max size
				if (name.length() > maxNameSize)
				{
					sender.sendMessage(ChatColor.RED + "You may only use " + maxNameSize + " characters in your claims name");
					return true;
				}
				
				// Check valid chars
				if (!name.matches("^[a-zA-Z0-9_]+$"))
				{
					sender.sendMessage(ChatColor.RED + "You may only use the characters a-z numbers and _ in your claims name");
					return true;
				}
				
				// Check uniqueness
				List<Claim> claims = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).getClaims();
				
				for (Claim c : claims)
				{
					if (c == claim)
						continue;
					
					GPClaimData claimData = this.claims.getData(c);
					if (claimData.getName() != null && claimData.getName().equalsIgnoreCase(name))
					{
						sender.sendMessage(ChatColor.RED + "You cannot name this claim " + name + ". Another of your claims is named this");
						return true;
					}
				}
				
				// Its unique, change it
				data.setName(name);
				data.save();
				sender.sendMessage(ChatColor.GREEN + "This claim is now named " + name);
				if (sender.hasPermission("pandora.tpclaim"))
					sender.sendMessage(ChatColor.GRAY + "You can now teleport here with /tpclaim " + name + ". Others can teleport here with /tpclaim " + sender.getName() + " " + name);
			}
		}
		else if (args[0].equalsIgnoreCase("teleport"))
		{
			data.setTeleport(player.getLocation());
			data.save();
			
			sender.sendMessage(ChatColor.GREEN + "Players teleporting to this claim will now teleport here.");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Unknown option " + args[0] + ". Expected 'name' or 'teleport'");
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command cmd, String label, String[] args )
	{
		if (!(sender instanceof Player))
			return null;
		
		List<String> options = null;
		if (args.length == 0)
			options = Arrays.asList("name", "teleport");
		
		if (options != null)
			return Utilities.matchStrings(args[args.length-1], options);
		else
			return null;
	}
}
