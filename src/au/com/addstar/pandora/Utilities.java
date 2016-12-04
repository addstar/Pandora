package au.com.addstar.pandora;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.monolith.MonoSpawnEgg;
import au.com.addstar.monolith.Monolith;
import au.com.addstar.monolith.lookup.EntityDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import au.com.addstar.monolith.ItemMetaBuilder;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;

public class Utilities
{
	public static long parseDateDiff(String dateDiff)
	{
		if(dateDiff == null)
			return 0;
		
		Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
		dateDiff = dateDiff.toLowerCase();
		
		Matcher m = dateDiffPattern.matcher(dateDiff);
		
		if(m.matches())
		{
			int years,months,weeks,days,hours,minutes,seconds;
			boolean negative;
			
			if(m.group(1) != null)
				negative = (m.group(1).compareTo("-") == 0);
			else
				negative = false;

			if(m.group(2) != null)
				years = Integer.parseInt(m.group(2));
			else
				years = 0;
			
			if(m.group(3) != null)
				months = Integer.parseInt(m.group(3));
			else
				months = 0;
			
			if(m.group(4) != null)
				weeks = Integer.parseInt(m.group(4));
			else
				weeks = 0;
			
			if(m.group(5) != null)
				days = Integer.parseInt(m.group(5));
			else
				days = 0;
			
			if(m.group(6) != null)
				hours = Integer.parseInt(m.group(6));
			else
				hours = 0;
			
			if(m.group(7) != null)
				minutes = Integer.parseInt(m.group(7));
			else
				minutes = 0;
			
			if(m.group(8) != null)
				seconds = Integer.parseInt(m.group(8));
			else
				seconds = 0;
			
			// Now calculate the time
			long time = 0;
			time += seconds * 1000L;
			time += minutes * 60000L;
			time += hours * 3600000L;
			time += days * 72000000L;
			time += weeks * 504000000L;
			time += months * 2191500000L;
			time += years * 26298000000L;
			
			if(negative)
				time *= -1;
			
			return time;
		}
		
		return 0;
	}
	
	public static boolean safeTeleport(Player player, Location loc)
	{
		int horRange = 30;
		
		double closestDist = Double.MAX_VALUE;
		Location closest = null;
		
		for(int y = 0; y < loc.getWorld().getMaxHeight(); ++y)
		{
			for(int x = loc.getBlockX() - horRange; x < loc.getBlockX() + horRange; ++x)
			{
				for(int z = loc.getBlockZ() - horRange; z < loc.getBlockZ() + horRange; ++z)
				{
					for(int i = 0; i < 2; ++i)
					{
						int yy = loc.getBlockY();
						
						if(i == 0)
						{
							yy -= y;
							if(yy < 0)
								continue;
						}
						else
						{
							yy += y;
							if(yy >= loc.getWorld().getMaxHeight())
								continue;
						}
	
						Location l = new Location(loc.getWorld(), x, yy, z);
						double dist = loc.distanceSquared(l);
						
						if(dist < closestDist && isSafeLocation(l))
						{
							closest = l;
							closestDist = dist;
						}
					}
				}
			}
			
			if(y*y > closestDist)
				break;
		}
		
		if(closest == null)
			return false;
		
		closest.setPitch(loc.getPitch());
		closest.setYaw(loc.getYaw());
		
		return player.teleport(closest.add(0.5, 0, 0.5));
	}
	
	public static boolean isSafeLocation(Location loc)
	{
		Block feet = loc.getBlock();
		Block ground = feet.getRelative(BlockFace.DOWN);
		Block head = feet.getRelative(BlockFace.UP);
		
		return (isSafe(feet) && isSafe(head) && (head.getType() != Material.WATER && head.getType() != Material.STATIONARY_WATER) && ground.getType().isSolid());
	}
	
	private static boolean isSafe(Block block)
	{
		switch(block.getType())
		{
		case AIR:
		case SUGAR_CANE_BLOCK:
		case WATER:
		case STATIONARY_WATER:
		case LONG_GRASS:
		case CROPS:
		case CARROT:
		case POTATO:
		case RED_MUSHROOM:
		case RED_ROSE:
		case BROWN_MUSHROOM:
		case YELLOW_FLOWER:
		case DEAD_BUSH:
		case SIGN_POST:
		case WALL_SIGN:
			return true;
		default:
			return false;
		}
	}
	
	public static void adjustEventHandlerPosition(HandlerList list, Listener listener, String beforePlugin)
	{
		Plugin plugin = Bukkit.getPluginManager().getPlugin(beforePlugin);
		if(plugin == null || !plugin.isEnabled())
			return;
		
		ArrayList<RegisteredListener> theirs = new ArrayList<>();
		RegisteredListener mine = null;
		
		for(RegisteredListener regListener : list.getRegisteredListeners())
		{
			if(regListener.getListener() == listener)
				mine = regListener;
			if(regListener.getPlugin().equals(plugin))
				theirs.add(regListener);
		}
		
		if(mine == null)
			return;
		
		list.unregister(mine);
		for(RegisteredListener regListener : theirs)
			list.unregister(regListener);
		
		// Register in the order we want them in
		list.register(mine);
		list.registerAll(theirs);
		list.bake();
		
		MasterPlugin.getInstance().getLogger().info("NOTE: Listener " + listener + " injected before that of " + beforePlugin + " listener");
	}
	
	public static List<String> matchStrings(String str, Collection<String> values)
	{
		str = str.toLowerCase();
		ArrayList<String> matches = new ArrayList<>();
		
		for(String value : values)
		{
			if(value.toLowerCase().startsWith(str))
				matches.add(value);
		}
		
		if(matches.isEmpty())
			return null;
		return matches;
	}
	
	@SuppressWarnings( "deprecation" )
    public static MaterialDefinition getMaterial(String name)
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
	
	public static ItemStack getItem(String[] args, int start) throws IllegalArgumentException
	{
		MaterialDefinition def = null;
		EntityDefinition edef = null;
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
				{try {
					try {
						data = Integer.parseInt(dataStr);
						if (data < 0)
							throw new IllegalArgumentException("Data value cannot be less than 0");
					} catch (NumberFormatException e) {
						if(def.getMaterial() == Material.MONSTER_EGG){
							String type = dataStr;
							edef = Lookup.findEntityByName(type);
						}else{
							throw new IllegalArgumentException("Unable to parse data value " + dataStr);

						}
					}
				}catch (IllegalArgumentException e){
					e.printStackTrace();
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
		if (def.getMaterial() == Material.MONSTER_EGG && edef != null){
			MonoSpawnEgg egg = new MonoSpawnEgg(item);
			egg.setMonoSpawnedType(edef.getType());
			item = egg.getItem();
		}
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
}
