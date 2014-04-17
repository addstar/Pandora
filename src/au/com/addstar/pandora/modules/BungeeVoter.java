package au.com.addstar.pandora.modules;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class BungeeVoter implements Module, PluginMessageListener, Listener {
	private MasterPlugin mPlugin;
	
	@Override
	public void onEnable() {
		Bukkit.getMessenger().registerIncomingPluginChannel(mPlugin, "BungeeVoter", this);
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		mPlugin = plugin;
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player sender, byte[] data) {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		
		try {
			String action = input.readUTF();
			if(channel.equals("BungeeVoter")) {
				if(action.equals("vote")) {
					// Only process votes for players online, on this server currently.
					// This ensures the vote will only ever be processed by one server.
					// Note: This makes it impossible to vote offline (even for admins)
					String name = input.readUTF();
					Player player = Bukkit.getPlayerExact(name);
					if (player != null) {
						// Player is online and on this server
						String address = input.readUTF();
						String timestamp = input.readUTF();
						String service = input.readUTF();

						// Build vote to send
						Vote vote = new Vote();
						vote.setUsername(player.getName());
						vote.setAddress(address);
						vote.setTimeStamp(timestamp);
						vote.setServiceName(service);

						// Send the votifier event
						VotifierEvent event = new VotifierEvent(vote);
						Bukkit.getPluginManager().callEvent(event);

						mPlugin.getLogger().log(Level.INFO, "[BungeeVoter] Received vote record -> Vote ("
								+ "from:" + service + " "
								+ "username:" + name + " "
								+ "address:" + address + " "
								+ "timeStamp:" + timestamp
								+ ")"
						);
					}
				} else {
					// Dont know how to handle this data
					mPlugin.getLogger().log(Level.INFO, "Unknown vote type!");
					mPlugin.getLogger().log(Level.INFO, "DATA: " + data.toString());
				}
			}
		}
		catch(IOException e) {
			mPlugin.getLogger().log(Level.INFO, "Unable to process vote!");
			mPlugin.getLogger().log(Level.INFO, "DATA: " + data.toString());
			e.printStackTrace();
		}
	}
}