package au.com.addstar.pandora.modules;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.kitteh.vanish.VanishPlugin;
import org.kitteh.vanish.event.VanishStatusChangeEvent;

import au.com.addstar.bc.AFKChangeEvent;
import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ProxyJoinEvent;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class VanishXServer implements Module, Listener
{
	private Plugin mPlugin;
	private VanishPlugin mVanish;
	
	@Override
	public void onEnable()
	{
		mVanish = (VanishPlugin)Bukkit.getPluginManager().getPlugin("VanishNoPacket");
	}

	@Override
	public void onDisable()
	{
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}
	
	private void unvanishIfNeeded(final Player player)
	{
		BungeeChat.getSyncManager().getPlayerPropertyAsync(player.getName(), "isVanished", new IMethodCallback<Object>()
		{
			@Override
			public void onFinished( Object isVanished )
			{
				if(!(isVanished instanceof Byte) || ((Byte)isVanished) == 0)
				{
					if(mVanish.getManager().isVanished(player))
						mVanish.getManager().toggleVanishQuiet(player, false);
				}
			}
			
			@Override
			public void onError( String type, String message )
			{
				System.err.println("[VanishXServer] Remote call exception. " + type + ": " + message);
			}
		});
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		final Player player = event.getPlayer();
		if(player.hasPermission("vanish.vanish"))
		{
			if(!mVanish.getManager().isVanished(player))
				mVanish.getManager().toggleVanishQuiet(player, false);
			
			Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
			{
				@Override
				public void run()
				{
					BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "TL:group:vanish", true);
				}
			}, 2);
			
			if(event.getPlayer().hasPermission("vanish.silentjoin.global"))
			{
				Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
				{
					@Override
					public void run()
					{
						BungeeChat.getSyncManager().getPlayerPropertyAsync(player.getName(), "hasJoined", new IMethodCallback<Object>()
						{
							@Override
							public void onFinished( Object hasJoined )
							{
								String broadcastMessage = ChatColor.DARK_AQUA + player.getName() + " has joined vanished and silently";
								if(!(hasJoined instanceof Byte) || ((Byte)hasJoined) == 0)
								{
									player.sendMessage(ChatColor.DARK_AQUA + "You have joined vanished, To appear: /vanish");
									Bukkit.broadcast(broadcastMessage, "vanish.silentjoin.global");
									BungeeChat.mirrorChat(broadcastMessage, "VanishNotify");
									mVanish.getManager().getAnnounceManipulator().addToDelayedAnnounce(player.getName());
									BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "hasQuitMessage", (byte)0);
								}
								else
									unvanishIfNeeded(player);
							}
							
							@Override
							public void onError( String type, String message )
							{
								System.err.println("[VanishXServer] Remote call exception. " + type + ": " + message);
							}
						});
						
						BungeeChat.getSyncManager().getPlayerPropertyAsync(player.getName(), "isFakeJoin", new IMethodCallback<Object>()
						{
							@Override
							public void onFinished( Object status )
							{
								if(status instanceof Byte)
									setFakeOnlineStatus(player, ((Byte)status) != 0);
								else
									setFakeOnlineStatus(player, false);
							}
							
							@Override
							public void onError( String type, String message )
							{
								System.err.println("[VanishXServer] Remote call exception. " + type + ": " + message);
							}
						});
						BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "hasJoined", (byte)1);
					}
				}, 2L);
			}
			else
			{
				Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
				{
					@Override
					public void run()
					{
						unvanishIfNeeded(player);
					}
				}, 2L);
			}
		}
		
		Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				if(player.hasPermission("vanish.see"))
					BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "TL:see:vanish", true);
				else
					BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "TL:see:vanish", null);
			}
		}, 2L);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	private void onProxyJoin(ProxyJoinEvent event)
	{
		if(event.getPlayer().hasPermission("vanish.silentjoin.global"))
			event.setJoinMessage(null);
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onVanishChange(VanishStatusChangeEvent event)
	{
		final Player player = event.getPlayer();
		final boolean isOnline = mVanish.getManager().getAnnounceManipulator().getFakeOnlineStatus(player.getName());
		
		BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "isVanished", (byte)(event.isVanishing() ? 1 : 0));
		if(event.isVanishing())
			BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "TL:group:vanish", event.isVanishing());
		else
			BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "TL:group:vanish", null);
		
		Bukkit.getScheduler().runTask(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				boolean finalOnline = mVanish.getManager().getAnnounceManipulator().getFakeOnlineStatus(player.getName());

				if(isOnline != finalOnline)
				{
					if(finalOnline)
					{
						// Fake Join
						BungeeChat.mirrorChat(ChatColor.YELLOW + player.getName() + " joined the game.", "BCast");
						BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "hasQuitMessage", (byte)1);
					}
					else
					{
						// Fake Quit
						BungeeChat.mirrorChat(ChatColor.YELLOW + player.getName() + " left the game.", "BCast");
						BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "hasQuitMessage", (byte)0);
					}
					
					BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "isFakeJoin", (byte)(finalOnline ? 1 : 0));
				}
				
			}
		});
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onAFKChange(AFKChangeEvent event)
	{
		if(mVanish.getManager().isVanished(event.getPlayer()))
			event.setCancelled(true);
	}
	
	private Map<String, Boolean> playerOnlineStatus = null;
	
	@SuppressWarnings( "unchecked" )
	private void setFakeOnlineStatus(Player player, boolean state)
	{
		if(playerOnlineStatus == null)
		{
			try
			{
				Field field = mVanish.getManager().getAnnounceManipulator().getClass().getDeclaredField("playerOnlineStatus");
				field.setAccessible(true);
				playerOnlineStatus = (Map<String, Boolean>)field.get(mVanish.getManager().getAnnounceManipulator());
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		playerOnlineStatus.put(player.getName(), state);
	}
}
