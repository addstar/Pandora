package au.com.addstar.pandora.modules;

import java.util.List;

import net.minecraft.server.v1_7_R1.Block;
import net.minecraft.server.v1_7_R1.ChatComponentText;
import net.minecraft.server.v1_7_R1.MojangsonParser;
import net.minecraft.server.v1_7_R1.NBTBase;
import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.minecraft.server.v1_7_R1.TileEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.command.CraftBlockCommandSender;
import org.bukkit.entity.Player;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class CommandBlockHelpers implements Module
{
	private MasterPlugin mPlugin;
	@Override
	public void onEnable()
	{
		SetBlockCommand command = new SetBlockCommand();
		mPlugin.getCommand("setblock").setExecutor(command);
		mPlugin.getCommand("setblock").setTabCompleter(command);
	}

	@Override
	public void onDisable()
	{
		mPlugin.getCommand("setblock").setExecutor(null);
		mPlugin.getCommand("setblock").setTabCompleter(null);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}
	
	public class SetBlockCommand implements CommandExecutor, TabCompleter
	{

		@Override
		public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
		{
			return null;
		}

		private Location getLocation(CommandSender sender)
		{
			if(sender instanceof Player)
				return ((Player)sender).getLocation();
			if(sender instanceof BlockCommandSender)
				return ((BlockCommandSender)sender).getBlock().getLocation();
			return null;
		}
		
		private double parseCoord(String string, double coord) throws NumberFormatException
		{
			boolean relative = string.startsWith("~");
			
			if(!relative)
				coord = 0;
			
			if (!relative || string.length() > 1) 
			{
                if (relative)
                    string = string.substring(1);
                
                coord += Double.parseDouble(string);
            }
			
			return coord;
		}
		
		@SuppressWarnings( "deprecation" )
		private Material parseMaterial(String string)
		{
			if (Block.REGISTRY.b(string))
	            return Material.getMaterial(Block.b((Block)Block.REGISTRY.a(string)));
	        else 
	        {
	            try 
	            {
	                int i = Integer.parseInt(string);
	                
	                return Material.getMaterial(i);
	            } 
	            catch (NumberFormatException numberformatexception) 
	            {
	            }

	            return null;
	        }
		}
		
		public void sendMessage(CommandSender sender, String message)
		{
			if(sender instanceof BlockCommandSender)
				((CraftBlockCommandSender)sender).getTileEntity().sendMessage(new ChatComponentText(message));
			else
				sender.sendMessage(message);
		}
		
		@SuppressWarnings( "deprecation" )
		@Override
		public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
		{
			Location loc = getLocation(sender);
			if(loc == null)
				return true;
			
			if (args.length >= 4) 
			{
				loc.setX(parseCoord(args[0], loc.getX()));
				loc.setY(parseCoord(args[1], loc.getY()));
				loc.setZ(parseCoord(args[2], loc.getZ()));
	            
	            Material type = parseMaterial(args[3]);
	            if(type == null)
	            {
	            	sendMessage(sender, "Cannot find type " + args[3]);
	            	return true;
	            }
	            
	            int data = 0;

	            if (args.length >= 5) 
	            {
	            	try
	            	{
	            		data = Integer.parseInt(args[4]);
	            		if(data < 0 || data > 15)
	            		{
	            			sendMessage(sender, "Data value out of range");
	            			return true;
	            		}
	            	}
	            	catch(NumberFormatException e)
	            	{
	            		sendMessage(sender, "Data should be a number 0-15");
	            		return true;
	            	}
	            }

                NBTTagCompound tag = new NBTTagCompound();
                boolean hasTag = false;

                if (args.length >= 7) 
                {
                	String tagString = "";
                	for(int i = 6; i < args.length; ++i)
                	{
                		if(!tagString.isEmpty())
                			tagString += " ";
                		tagString += args[i];
                	}
                			
                    try 
                    {
                        NBTBase nbtbase = MojangsonParser.a(tagString);

                        if (!(nbtbase instanceof NBTTagCompound)) 
                        {
                        	sendMessage(sender, "Not a valid tag");
                            return true;
                        }

                        tag = (NBTTagCompound) nbtbase;
                        hasTag = true;
                    } 
                    catch (Exception e) 
                    {
                    	sendMessage(sender, "Error in tag: " + e.getMessage());
                        return true;
                    }
                }

                if (args.length >= 6) 
                {
                    if (args[5].equals("destroy"))
                        loc.getBlock().setType(Material.AIR);
                    else if (args[5].equals("keep") && !loc.getBlock().isEmpty())
                    {
                    	sendMessage(sender, "No change");
                    	return true;
                    }
                    else if(!args[5].equals("replace"))
                    {
                    	sendMessage(sender, "Unknown oldBlockHanding value" + args[5]);
                    	return true;
                    }
                }

                
                if (loc.getBlock().setTypeIdAndData(type.getId(), (byte)data, true))
                {
                    if (hasTag) 
                    {
                    	TileEntity tile = ((CraftWorld)loc.getWorld()).getTileEntityAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    	if(tile != null)
                    	{
                    		tag.setInt("x", loc.getBlockX());
                    		tag.setInt("y", loc.getBlockY());
                    		tag.setInt("z", loc.getBlockZ());
                    		tile.a(tag);
                    	}
                    }

                	sendMessage(sender, "Block placed");
                }
                else
                	sendMessage(sender, "No change");
                return true;
	        } 
			else
			{
				if(sender instanceof BlockCommandSender)
					sendMessage(sender, "Usage: /setblock <x> <y> <z> <tilename> [datavalue] [oldblockHandling] [dataTag]");
				return false;
			}
		}
		
	}
}
