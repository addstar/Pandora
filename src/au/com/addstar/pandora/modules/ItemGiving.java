package au.com.addstar.pandora.modules;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

public class ItemGiving implements Module, CommandExecutor, TabCompleter
{
	@Override
	public void onEnable()
	{
	}

	@Override
	public void onDisable()
	{
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		plugin.getCommand("item").setExecutor(this);
		plugin.getCommand("item").setTabCompleter(this);
		
		plugin.getCommand("give").setExecutor(this);
		plugin.getCommand("give").setTabCompleter(this);
		
		plugin.getCommand("giveall").setExecutor(this);
		plugin.getCommand("giveall").setTabCompleter(this);
		
		plugin.getCommand("giveallworld").setExecutor(this);
		plugin.getCommand("giveallworld").setTabCompleter(this);
	}

	@Override
    public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
    {
	    return null;
    }
	
	private int addItem(Player player, ItemStack item)
	{
		int added = item.getAmount();
		HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
		if(!leftover.isEmpty())
			added -= leftover.get(0).getAmount();
		
		if(added > 0)
			player.updateInventory();
		
		return added;
	}

	private boolean onItem(CommandSender sender, String[] args)
	{
		if(args.length < 1)
			return false;
		
		if(!(sender instanceof Player))
			return false;
		
		ItemStack item = Utilities.getItem(args, 0);
		
		Player player = (Player)sender;
		int added = addItem(player, item);

		String name = StringTranslator.getName(item);
		if(name.equals("Unknown"))
			name = item.getType().name().toLowerCase() + ":" + item.getDurability();
		
		if(added > 0)
			sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name);
		else
			sender.sendMessage(ChatColor.RED + "Unable to give " + ChatColor.GOLD + name + ChatColor.RED + ". There is no room for it in your inventory");
		
		return true;
	}
	
	private boolean onGive(CommandSender sender, String[] args)
	{
		if(args.length < 2)
			return false;
		
		Player destination = Bukkit.getPlayer(args[0]);
		if(destination == null)
			throw new IllegalArgumentException("Unknown player " + args[0]);
		
		ItemStack item = Utilities.getItem(args, 1);
		
		int added = addItem(destination, item);

		String senderName = sender.getName();
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();
		else
			senderName = "Server";

		String name = StringTranslator.getName(item);
		if(name.equals("Unknown"))
			name = item.getType().name().toLowerCase() + ":" + item.getDurability();
		
		if(added > 0){
			sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + destination.getDisplayName());
			destination.sendMessage(ChatColor.RED + senderName + ChatColor.GOLD + " has given you " + ChatColor.RED + added + ChatColor.GOLD + " of " + ChatColor.RED + name);
		} else {
			sender.sendMessage(ChatColor.RED + "Unable to give " + ChatColor.GOLD + name + ChatColor.RED + ". There is no room for it in " + destination.getDisplayName() + "'s inventory");
		}
		
		return true;
	}
	
	private boolean onGiveAll(CommandSender sender, String[] args)
	{
		if(args.length < 1)
			return false;
		
		ItemStack item = Utilities.getItem(args, 0);
		
		String senderName = sender.getName();
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();
		else
			senderName = "Server";
		
		String name = StringTranslator.getName(item);
		if(name.equals("Unknown"))
			name = item.getType().name().toLowerCase() + ":" + item.getDurability();
		sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + "everyone");
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			if(!player.hasPermission("pandora.giveall.receive"))
				continue;
			
			int added = addItem(player, item.clone());
			if(added == 0)
				sender.sendMessage(ChatColor.RED + player.getDisplayName() + "'s inventory was full");
			else
				player.sendMessage(ChatColor.RED + senderName + ChatColor.GOLD + " has given everyone " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name);
		}
		
		return true;
	}
	
	private boolean onGiveAllWorld(CommandSender sender, String[] args)
	{
		if(args.length < 2)
			return false;
		
		World world = Bukkit.getWorld(args[0]);
		if (world == null && sender instanceof Player)
		{
			if (args[0].equalsIgnoreCase("this"))
				world = ((Player)sender).getWorld();
		}
		
		if (world == null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown world " + args[0]);
			return true;
		}
		
		ItemStack item = Utilities.getItem(args, 1);
		
		String senderName = sender.getName();
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();
		else
			senderName = "Server";
		
		String name = StringTranslator.getName(item);
		if(name.equals("Unknown"))
			name = item.getType().name().toLowerCase() + ":" + item.getDurability();
		sender.sendMessage(ChatColor.GOLD + "Giving " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name + ChatColor.GOLD + " to " + ChatColor.RED + "everyone" + ChatColor.GOLD + " in " + ChatColor.RED + world.getName());
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			if(!player.hasPermission("pandora.giveall.receive"))
				continue;
			
			if(player.getWorld() != world)
				continue;
			
			int added = addItem(player, item.clone());
			if(added == 0)
				sender.sendMessage(ChatColor.RED + player.getDisplayName() + "'s inventory was full");
			else
				player.sendMessage(ChatColor.RED + senderName + ChatColor.GOLD + " has given everyone " + ChatColor.RED + item.getAmount() + ChatColor.GOLD + " of " + ChatColor.RED + name);
		}
		
		return true;
	}
	
	@Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
    {
		try
		{
			if(command.getName().equals("item"))
				return onItem(sender, args);
			else if(command.getName().equals("give"))
				return onGive(sender, args);
			else if(command.getName().equals("giveall"))
				return onGiveAll(sender, args);
			else if(command.getName().equals("giveallworld"))
				return onGiveAllWorld(sender, args);
			return false;
		}
		catch(IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return true;
		}
    }
}
