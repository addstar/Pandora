package au.com.addstar.pandora.modules;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.mcsg.survivalgames.api.PlayerWinEvent;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class SurvivalGamesBroadcaster implements Module, Listener
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
		mConfig = new Config(new File(plugin.getDataFolder(), "SurvivalGamesBroadcaster.yml"));
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onSurvivalGamesWin(PlayerWinEvent event)
	{
		BungeeChat.mirrorChat(event.getMessage(), mConfig.channel);
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
