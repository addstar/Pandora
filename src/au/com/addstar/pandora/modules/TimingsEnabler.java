package au.com.addstar.pandora.modules;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.TimedRegisteredListener;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

public class TimingsEnabler implements Module, CommandExecutor, TabCompleter
{
	private Field mExecutorField;
	private MasterPlugin mPlugin;
	
	@Override
	public void onEnable()
	{
		try
		{
			mExecutorField = RegisteredListener.class.getDeclaredField("executor");
			mExecutorField.setAccessible(true);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		mPlugin.getCommand("ptimings").setExecutor(this);
		mPlugin.getCommand("ptimings").setTabCompleter(this);
	}

	@Override
	public void onDisable()
	{
		mPlugin.getCommand("ptimings").setExecutor(mPlugin);
		mPlugin.getCommand("ptimings").setTabCompleter(mPlugin);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}
	
	private EventExecutor getExecutor(RegisteredListener listener)
	{
		try
		{
			return (EventExecutor)mExecutorField.get(listener);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1)
			return Utilities.matchStrings(args[0], Arrays.asList("on", "off", "fulloff"));
		return null;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length != 1)
			return false;
		
		if(args[0].equalsIgnoreCase("on"))
		{
			((SimplePluginManager)Bukkit.getPluginManager()).useTimings(true);
			
			for (HandlerList handlerList : HandlerList.getHandlerLists()) 
            {
				LinkedList<RegisteredListener> toRegister = new LinkedList<RegisteredListener>();
                for (RegisteredListener listener : handlerList.getRegisteredListeners()) 
                {
                    if (listener instanceof TimedRegisteredListener)
                        ((TimedRegisteredListener)listener).reset();
                    else
                    {
                    	toRegister.add(new TimedRegisteredListener(listener.getListener(), getExecutor(listener), listener.getPriority(), listener.getPlugin(), listener.isIgnoringCancelled()));
                    	handlerList.unregister(listener);
                    }
                }
                handlerList.registerAll(toRegister);
                handlerList.bake();
            }
			
			sender.sendMessage( "Enabled Timings" );
		}
		else if(args[0].equalsIgnoreCase("off"))
		{
			((SimplePluginManager)Bukkit.getPluginManager()).useTimings(false);
			sender.sendMessage( "Disabled Timings" );
		}
		else if(args[0].equalsIgnoreCase("fulloff"))
		{
			((SimplePluginManager)Bukkit.getPluginManager()).useTimings(false);
			
			for (HandlerList handlerList : HandlerList.getHandlerLists()) 
            {
				LinkedList<RegisteredListener> toRegister = new LinkedList<RegisteredListener>();
                for (RegisteredListener listener : handlerList.getRegisteredListeners()) 
                {
                    if (listener instanceof TimedRegisteredListener)
                    {
                        ((TimedRegisteredListener)listener).reset();
                        handlerList.unregister(listener);
                        toRegister.add(new RegisteredListener(listener.getListener(), getExecutor(listener), listener.getPriority(), listener.getPlugin(), listener.isIgnoringCancelled()));
                    }
                }
                handlerList.registerAll(toRegister);
                handlerList.bake();
            }
			
			sender.sendMessage( "Disabled Timings" );
		}
		else
			return false;
		
        return true;
	}
}
