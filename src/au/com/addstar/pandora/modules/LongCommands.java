package au.com.addstar.pandora.modules;

import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class LongCommands implements Module, Listener
{
	private WeakHashMap<Player, String> mCommands = new WeakHashMap<Player, String>();
	public static String longCommandKey = "\\";
	private Plugin mPlugin;
	
	private void executeCommand(final Player player, final String command)
	{
		Bukkit.getScheduler().runTask(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				player.chat(command);
			}
		});
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=false)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		if(!event.getPlayer().hasPermission("pandora.longcommands"))
			return;
		
		String existing = null;
		
		synchronized(mCommands)
		{
			existing = mCommands.get(event.getPlayer());
		}
		
		if(existing == null)
			return;

		event.setCancelled(true);
		
		if(event.getMessage().equals("-"))
		{
			synchronized(mCommands)
			{
				mCommands.remove(event.getPlayer());
			}
			
			event.getPlayer().sendMessage(ChatColor.GOLD + "Long command cancelled.");
		}
		else if(event.getMessage().endsWith(longCommandKey))
		{
			existing += event.getMessage().substring(0, event.getMessage().length() - longCommandKey.length());
			synchronized(mCommands)
			{
				mCommands.put(event.getPlayer(), existing);
			}
			
			event.getPlayer().sendMessage(ChatColor.GRAY + existing);
			event.getPlayer().sendMessage(ChatColor.GOLD + "> " + ChatColor.GRAY + "_");
		}
		else
		{
			existing += event.getMessage();
			
			synchronized(mCommands)
			{
				mCommands.remove(event.getPlayer());
			}
			
			executeCommand(event.getPlayer(), existing);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		if(!event.getPlayer().hasPermission("pandora.longcommands"))
			return;
		
		if(event.getMessage().endsWith(longCommandKey))
		{
			String command = event.getMessage().substring(0, event.getMessage().length() - longCommandKey.length());
			synchronized(mCommands)
			{
				mCommands.put(event.getPlayer(), command);
			}
			
			event.getPlayer().sendMessage(ChatColor.GRAY + command);
			event.getPlayer().sendMessage(ChatColor.GOLD + "> " + ChatColor.GRAY + "_");
			
			event.setCancelled(true);
		}
		else
		{
			boolean had = false;
			synchronized(mCommands)
			{
				had = mCommands.remove(event.getPlayer()) != null;
			}
			
			if(had)
				event.getPlayer().sendMessage(ChatColor.GOLD + "Long command cancelled.");
		}
	}
	
	
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}

}
