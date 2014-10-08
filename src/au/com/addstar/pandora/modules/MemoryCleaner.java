package au.com.addstar.pandora.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class MemoryCleaner implements Module, Listener
{
	private ArrayList<Definition> mPlayerCleanup;
	private Logger mLogger;
	private File mConfigLocation;
	private Plugin mPlugin;
	
	@Override
	public void onEnable()
	{
		mPlayerCleanup = new ArrayList<Definition>();
		
		loadConfig();
	}

	@Override
	public void onDisable()
	{
		mPlayerCleanup.clear();
		mPlayerCleanup = null;
	}
	
	public void loadConfig()
	{
		if (!mConfigLocation.exists())
			mPlugin.saveResource("MemoryCleaner.txt", false);
		BufferedReader reader = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(mConfigLocation));
			
			List<Definition> defList = null;
			
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				if (line.startsWith("[") && line.endsWith("]"))
				{
					line = line.substring(1,line.length()-1);
					if (line.equalsIgnoreCase("player"))
						defList = mPlayerCleanup;
					else
						throw new IllegalArgumentException("Found unknown definition section " + line + " in MemoryCleaner config");
					
					continue;
				}
				
				if (defList == null)
					throw new IllegalArgumentException("Found definition '" + line + "' without being in a section in MemoryCleaner config");
				
				String[] parts = line.split(":");
				if (parts.length != 2)
					throw new IllegalArgumentException("Error in definition '" + line + "'. Is not in the format of <plugin>:<path> in MemoryCleaner config");
				
				String plugin = parts[0];
				defList.add(new Definition(plugin, line));
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			try
			{
				if (reader != null)
					reader.close();
			}
			catch(IOException e)
			{
			}
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event)
	{
		final Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				runList(mPlayerCleanup, player);
			}
		}, 2L);
	}
	
	private void runList(List<Definition> definitions, Object object)
	{
		for(Definition def : definitions)
		{
			try
			{
				Object collection = def.getValue();
				removeFrom(object, collection);
			}
			catch(IllegalArgumentException e)
			{
				// Definition had an error
				mLogger.info("[MemCleaner] Error in definition " + def.path + ": " + e.getMessage());
			}
			catch(IllegalStateException e)
			{
				// Plugin wasnt loaded. Do nothing
			}
		}
	}
	
	private void removeFrom(Object object, Object collection)
	{
		if (collection instanceof Map<?,?>)
			((Map<?,?>)collection).remove(object);
		else if (collection instanceof Collection<?>)
			((Collection<?>)collection).remove(object);
	}
	
	@Override
	public void setPandoraInstance( MasterPlugin plugin ) 
	{
		mPlugin = plugin;
		mLogger = plugin.getLogger();
		mConfigLocation = new File(plugin.getDataFolder(), "MemoryCleaner.txt");
	}

	private static Field[] parsePath(String path, boolean pluginRoot)
	{
		String[] parts = path.split(":");
		if (parts.length != 2)
			throw new IllegalArgumentException("Unknown path format '" + path + "'");
		
		String[] names = parts[1].split(",");
		try
		{
			Class<?> holder;
			if (pluginRoot)
			{
				Plugin plugin = Bukkit.getPluginManager().getPlugin(parts[0]);
				if (plugin == null)
					throw new IllegalStateException("Unknown plugin " + parts[0]);
				
				holder = plugin.getClass();
			}
			else
				holder = Class.forName(parts[0]);
			
			Field[] fields = new Field[names.length];
			for(int i = 0; i < names.length; ++i)
			{
				String fieldName = names[i];
				if (fieldName.trim().isEmpty())
					throw new IllegalArgumentException("Illegal field name: '" + fieldName + "'");
				
				try
				{
					Field field = holder.getDeclaredField(fieldName);
					holder = field.getType();
					field.setAccessible(true);
					fields[i] = field;
				}
				catch(NoSuchFieldException e)
				{
					throw new IllegalArgumentException("Unknown field " + fieldName + " in " + holder.getName() + " declared as " + StringUtils.join(fields, ",", 0, i+1));
				}
			}
			
			return fields;
		}
		catch(ClassNotFoundException e)
		{
			throw new IllegalArgumentException("Unknown class " + parts[0]);
		}
	}
	
	private static class Definition
	{
		public final String plugin;
		public final String path;
		private Field[] mFields;
		
		public Definition(String plugin, String path)
		{
			this.plugin = plugin;
			this.path = path;
		}
		
		public Object getValue()
		{
			Plugin root = Bukkit.getPluginManager().getPlugin(plugin);
			if (root == null)
				return null;
			
			if (mFields == null)
				mFields = parsePath(path, true);
			
			try
			{
				Object obj = root;
				for(Field field : mFields)
					obj = field.get(obj);
				
				return obj;
			}
			catch(IllegalAccessException e)
			{
				// Shouldnt happen
				throw new AssertionError();
			}
		}
	}
}
