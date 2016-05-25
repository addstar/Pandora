package au.com.addstar.pandora.modules;

import java.io.File;

import nl.Steffion.BlockHunt.Arena.ArenaState;
import nl.Steffion.BlockHunt.Events.EndArenaEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class BlockhuntBroadcaster implements Module, Listener
{
	private MasterPlugin mPlugin;
	
	private Config mConfig;
	
	@Override
	public void onEnable()
	{
		if(mConfig.load())
			mConfig.save();
		
		Bukkit.getMessenger().registerOutgoingPluginChannel(mPlugin, "BungeeChat");
	}

	@Override
	public void onDisable()
	{
		Bukkit.getMessenger().unregisterOutgoingPluginChannel(mPlugin, "BungeeChat");
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mConfig = new Config(new File(plugin.getDataFolder(), "BlockhuntBroadcast.yml"));
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBHEndArenaEvent(EndArenaEvent event)
	{
		// If game ended due to players leaving.. Don't broadcast!
		// (only broadcast when the game ends normally)
		if (event.getArena().playersInArena.size() < event.getArena().minPlayers) return;

		// Only broadcast if the event was triggered while arena was "in game"
		if (!event.getArena().gameState.equals(ArenaState.INGAME)) return;

		String msg;
		if (event.getLosers().size() == 0) {
			// Seekers win
			msg = mConfig.message
					.replaceAll("%WINTEAM%", "Seekers")
					.replaceAll("%LOSETEAM%", "Hiders")
					.replaceAll("%ARENA%", event.getArena().arenaName);
		} else {
			// Hiders win
			msg = mConfig.message
					.replaceAll("%WINTEAM%", "Hiders")
					.replaceAll("%LOSETEAM%", "Seekers")
					.replaceAll("%ARENA%", event.getArena().arenaName);
		}
		BungeeChat.mirrorChat(ChatColor.translateAlternateColorCodes('&', msg), mConfig.channel);
	}
	
	private class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField(comment="The bungee chat channel to broadcast on. Default is '~BC' (the reserved broadcast channel)")
		public String channel = "~BC";

		@ConfigField(comment="The broadcast message when seekers win")
		public String message = "&9[BlockHunt]&b The &e%WINTEAM%&b have won in &e%ARENA%&b!";
	}
}
