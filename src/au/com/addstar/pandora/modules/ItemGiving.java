package au.com.addstar.pandora.modules;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.addstar.monolith.ItemMetaBuilder;
import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

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
	}

	@Override
    public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
    {
	    return null;
    }
	
	private ItemStack getItem(String[] args, int start)
	{
		MaterialDefinition def = null;
		Material mat = Lookup.findByMinecraftName(args[start]);
		int index = start;
		if(mat != null && args[index].contains(":"))
		{
			if(args.length != 1)
			{
				if(args.length < 3)
					throw new IllegalArgumentException("When using minecraft ids, you must specify both the data value, and amount too");
				
				short data = 0;
				try
				{
					data = Short.parseShort(args[index+1]);
					if(data < 0)
						throw new IllegalArgumentException("Data value for " + args[index] + " cannot be less than 0");
				}
				catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("Data value after " + args[index]);
				}
				
				index += 2;
				
				def = new MaterialDefinition(mat, data);
			}
			else
			{
				def = new MaterialDefinition(mat, (short)0);
				index = 1;
			}
		}
		else
		{
			String dataStr = null;
			if (args[index].contains(":"))
			{
				String name = args[index].split(":")[0];
				dataStr = args[index].split(":")[1];
				
				def = getMaterial(name);
			}
			else
				def = getMaterial(args[index]);
			
			if (def == null)
				throw new IllegalArgumentException("Unknown material " + args[index]);
			
			if (def.getData() < 0)
			{
				int data = 0;
				if (dataStr != null)
				{
					try
					{
						data = Integer.parseInt(dataStr);
						if (data < 0)
							throw new IllegalArgumentException("Data value cannot be less than 0");
					}
					catch(NumberFormatException e)
					{
						throw new IllegalArgumentException("Unable to parse data value " + dataStr);
					}
				}
				
				def = new MaterialDefinition(def.getMaterial(), (short)data);
			}
			
			index++;
		}
		
		// Parse amount
		int amount = def.getMaterial().getMaxStackSize();
		if(args.length > index)
		{
			try
			{
				amount = Integer.parseInt(args[index]);
				if (amount < 0)
					throw new IllegalArgumentException("Amount value cannot be less than 0");
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("Unable to parse amount value " + args[index]);
			}
			
			++index;
		}
		
		ItemStack item = def.asItemStack(amount);
		
		// Parse Meta
		if (args.length > index)
		{
			ItemMetaBuilder builder = new ItemMetaBuilder(item);
			for(int i = index; i < args.length; ++i)
			{
				String definition = args[i].replace('_', ' ');
				builder.accept(definition);
			}
			
			item.setItemMeta(builder.build());
		}
		
		return item;
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
		
		ItemStack item = getItem(args, 0);
		
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
		
		ItemStack item = getItem(args, 1);
		
		int added = addItem(destination, item);

		String senderName = sender.getName();
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();

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
		
		ItemStack item = getItem(args, 0);
		
		String senderName = sender.getName();
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();
		
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
			return false;
		}
		catch(IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return true;
		}
    }
	
	@SuppressWarnings( "deprecation" )
    private MaterialDefinition getMaterial(String name)
	{
		// Bukkit name
		Material mat = Material.getMaterial(name.toUpperCase());
		if (mat != null)
			return new MaterialDefinition(mat, (short)-1);
		
		// Id
		try
		{
			short id = Short.parseShort(name);
			mat = Material.getMaterial(id);
		}
		catch(NumberFormatException e)
		{
		}
		
		if(mat != null)
			return new MaterialDefinition(mat, (short)-1);

		// ItemDB
		return Lookup.findItemByName(name);
	}

}
