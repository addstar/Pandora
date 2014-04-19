package au.com.addstar.pandora.modules;

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
import au.com.addstar.bc.BungeeChat;
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
		if(event.getPlayer().hasPermission("vanish.vanish"))
		{
			final Player player = event.getPlayer();
			if(!mVanish.getManager().isVanished(player))
				mVanish.getManager().toggleVanishQuiet(player, false);
			
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
								{
									if(((Byte)status) == 1)
										mVanish.getManager().getAnnounceManipulator().addToDelayedAnnounce(player.getName());
									else
										mVanish.getManager().getAnnounceManipulator().dropDelayedAnnounce(player.getName());
								}
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
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onVanishChange(VanishStatusChangeEvent event)
	{
		final Player player = event.getPlayer();
		final boolean isOnline = mVanish.getManager().getAnnounceManipulator().getFakeOnlineStatus(player.getName());
		
		BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "isVanished", (byte)(event.isVanishing() ? 1 : 0));
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
					}
					else
					{
						// Fake Quit
						BungeeChat.mirrorChat(ChatColor.YELLOW + player.getName() + " left the game.", "BCast");
					}
					
					BungeeChat.getSyncManager().setPlayerProperty(player.getName(), "isFakeJoin", (byte)(finalOnline ? 1 : 0));
				}
				
			}
		});
	}
}
