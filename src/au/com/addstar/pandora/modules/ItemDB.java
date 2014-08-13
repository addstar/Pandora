package au.com.addstar.pandora.modules;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class ItemDB implements Module, CommandExecutor
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
		plugin.getCommand("itemdb").setExecutor(this);
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
	
	@SuppressWarnings( "deprecation" )
    @Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
    {
		if (args.length > 2)
			return false;
		
		MaterialDefinition def = null;
		if (args.length == 0)
		{
			if(!(sender instanceof Player))
				return false;
			
			Player player = (Player)sender;
			if(player.getItemInHand() != null)
				def = MaterialDefinition.from(player.getItemInHand());
			else
				def = new MaterialDefinition(Material.AIR, (short) 0);
		}
		else 
		{
			Material mat = Lookup.findByMinecraftName(args[0]);
			if(mat != null)
			{
				short data = 0;
				if(args.length == 2)
				{
					try
					{
						data = Short.parseShort(args[1]);
						if(data < 0)
						{
							sender.sendMessage(ChatColor.RED + "Data value for " + args[0] + " cannot be less than 0");
							return true;
						}
					}
					catch(NumberFormatException e)
					{
						sender.sendMessage(ChatColor.RED + "Expected nothing or data value after " + args[0]);
						return true;
					}
				}
				def = new MaterialDefinition(mat, data);
			}
			else
			{
				String dataStr = null;
				if (args[0].contains(":"))
				{
					if(args.length > 1)
						return false;
					
					String name = args[0].split(":")[0];
					dataStr = args[0].split(":")[1];
					
					def = getMaterial(name);
				}
				else
				{
					if(args.length == 2)
						dataStr = args[1];
					
					def = getMaterial(args[0]);
				}
				
				if (def == null)
				{
					sender.sendMessage(ChatColor.RED + "Unknown material " + args[0]);
					return true;
				}
				
				if (def.getData() < 0)
				{
					int data = 0;
					if (dataStr != null)
					{
						try
						{
							data = Integer.parseInt(dataStr);
							if (data < 0)
							{
								sender.sendMessage(ChatColor.RED + "Data value cannot be less than 0");
								return true;
							}
						}
						catch(NumberFormatException e)
						{
							sender.sendMessage(ChatColor.RED + "Unable to parse data value " + dataStr);
							return true;
						}
					}
					
					def = new MaterialDefinition(def.getMaterial(), (short)data);
				}
			}
		}
		
		ItemStack item = def.asItemStack(1);
		String properName = StringTranslator.getName(item);
		
		sender.sendMessage(ChatColor.GOLD + "Item: " + ChatColor.RED + properName + ChatColor.GOLD + " - " + ChatColor.RED + def.getMaterial().getId() + ":" + def.getData());
		sender.sendMessage(ChatColor.GOLD + "Bukkit Name: " + ChatColor.RED + def.getMaterial().name());
		String mcName = Lookup.findMinecraftNameByItem(def.getMaterial());
		if(mcName != null)
			sender.sendMessage(ChatColor.GOLD + "Minecraft Name: " + ChatColor.RED + mcName);
		
		int maxDura = item.getType().getMaxDurability();
		int uses = maxDura + 1 - item.getDurability();
		if(maxDura > 0)
			sender.sendMessage(ChatColor.GOLD + "Durability: " + ChatColor.RED + item.getDurability()+ " / " + (maxDura+1) + " (" + uses + " uses)");
		
		Set<String> names;
		if(maxDura > 0)
			names = Lookup.findNameByItem(new MaterialDefinition(item.getType(), (short)0));
		else
			names = Lookup.findNameByItem(def);
		
		if(!names.isEmpty())
			sender.sendMessage(ChatColor.GOLD + "Item short names: " + ChatColor.WHITE + StringUtils.join(names, ", "));
		
		return true;
    }

}
