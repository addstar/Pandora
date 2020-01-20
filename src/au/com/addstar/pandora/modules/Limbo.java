package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import au.com.addstar.pandora.ConfigField;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.io.File;

public class Limbo implements Module, Listener
{
	private MasterPlugin mPlugin;
    private Config mConfig;
    private Location spawnLoc;
    private World spawnWorld;

	@Override
	public void onEnable() {
		if(mConfig.load())
			mConfig.save();

		if (mConfig.enabled) {
			spawnWorld = Bukkit.getWorld(mConfig.limboworld);
			spawnLoc = new Location(spawnWorld, 0, 65, 0);
		}
	}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mConfig = new Config(new File(plugin.getDataFolder(), "Limbo.yml"));
		mPlugin = plugin;
	}

	@EventHandler(priority= EventPriority.LOW, ignoreCancelled=true)
	public void onPlayerSpawn(PlayerSpawnLocationEvent e) {
		if (mConfig.enabled) {
			hideOtherPlayers(e.getPlayer());
			e.setSpawnLocation(spawnLoc);
		}
	}

	private void hideOtherPlayers(Player p) {
		// Make all other players hidden from this player (others become invisible)
		for (Player other : Bukkit.getOnlinePlayers()) {
			other.hidePlayer(mPlugin, p);
		}
	}

	private class Config extends AutoConfig {
		public Config(File file) {
			super(file);
		}

		@ConfigField(comment="Should Limbo be enabled on this server?")
		public Boolean enabled = false;

		@ConfigField(comment="The name of the limbo world")
		public String limboworld = "limbo";
	}
}