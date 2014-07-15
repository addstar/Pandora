package au.com.addstar.pandora.modules;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow.Spigot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.pauldavdesign.mineauz.minigames.events.MinigamesBroadcastEvent;

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
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);

		try
		{
			out.writeUTF("Mirror");
			out.writeUTF(mConfig.channel);
			out.writeUTF(event.getMessageWithPrefix());

			Player[] players = Bukkit.getOnlinePlayers();
			if (players.length > 0) {
				players[0].sendPluginMessage(mPlugin, "BungeeChat", stream.toByteArray());
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
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
