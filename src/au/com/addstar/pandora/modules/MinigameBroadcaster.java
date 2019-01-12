package au.com.addstar.pandora.modules;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.mineauz.minigames.events.MinigamesBroadcastEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;

public class MinigameBroadcaster implements Module, Listener
{
	private MasterPlugin mPlugin;
	
	private Config mConfig;

	private boolean bungeechatenabled = false;
	
	@Override
	public void onEnable()
	{
		if(mConfig.load())
			mConfig.save();

		bungeechatenabled = mPlugin.registerBungeeChat();
		if (!bungeechatenabled) mPlugin.getLogger().warning("BungeeChat is NOT enabled! Cross-server messages will be disabled.");
	}

	@Override
	public void onDisable()
	{
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
		if(bungeechatenabled)BungeeChat.mirrorChat(event.getMessageWithPrefix(), mConfig.channel);
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
