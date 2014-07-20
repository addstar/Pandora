package au.com.addstar.pandora.modules;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.pauldavdesign.mineauz.minigames.events.MinigamesBroadcastEvent;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class MinigameBroadcaster implements Module, Listener
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
		mConfig = new Config(new File(plugin.getDataFolder(), "MinigameBroadcast.yml"));
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onMinigameBroadcast(MinigamesBroadcastEvent event)
	{
		BungeeChat.mirrorChat(event.getMessageWithPrefix(), mConfig.channel);
	}
	
	private class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField(comment="The bungee chat channel to broadcast on. Default is '~BC' (the reserved broadcast channel)")
		public String channel = "~BC";
	}
}
