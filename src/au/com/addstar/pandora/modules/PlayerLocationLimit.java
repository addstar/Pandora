package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class PlayerLocationLimit implements Module, Listener {
	private boolean Debug = false;
	private MasterPlugin mPlugin;
	private File mFile;
	private FileConfiguration mConfig;
	private HashMap<World, Integer[]> WorldLimits = new HashMap<World, Integer[]>();

	@Override
	public void onEnable() {
		loadConfig();

		// Disable event listeners if there are no worlds configured
		if (WorldLimits.size() == 0) {
			System.out.println("[PlayerLocationLimit] No worlds configured, disabling event listeners.");
			PlayerMoveEvent.getHandlerList().unregister(this);
			PlayerTeleportEvent.getHandlerList().unregister(this);
		}
	}

	@Override
	public void onDisable() {
		mConfig = null;
		mFile = null;
		WorldLimits.clear();
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		mPlugin = plugin;
	}

	private boolean loadConfig() {
		// Load the config
		try
		{
			mFile = new File(mPlugin.getDataFolder(), "PlayerLocationLimit.yml");
			if (!mFile.exists())
				mPlugin.saveResource("PlayerLocationLimit.yml", false);

			mConfig = YamlConfiguration.loadConfiguration(mFile);
			if (mFile.exists())
				mConfig.load(mFile);

			Debug = mConfig.getBoolean("debug", false);

			if ((mConfig != null) && (mConfig.isConfigurationSection("worlds"))) {
				Set<String> worlds = (Set<String>) mConfig.getConfigurationSection("worlds").getKeys(false);
				System.out.println("Loading world configs...");
				for (String wname : worlds) {
					System.out.println("World: " + wname);
					World world = Bukkit.getWorld(wname);
					if (world != null) {
						ConfigurationSection wsection = mConfig.getConfigurationSection("worlds." + wname);
						int minY = wsection.getInt("minY", 0);
						int maxY = wsection.getInt("maxY", 255);
						WorldLimits.put(world, new Integer[] {minY, maxY});
					} else {
						System.out.println("WARNING: Unknown World \"" + wname + "\"!");
					}
				}

				if (Debug) {
					System.out.println("==============================================");
					for (Map.Entry<World, Integer[]> entry : WorldLimits.entrySet()) {
						World w = entry.getKey();
						System.out.println("World: " + w.getName());
						System.out.println("  minY: " + entry.getValue()[0]);
						System.out.println("  maxY: " + entry.getValue()[1]);
					}
					System.out.println("==============================================");
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void checkMovePerms(Cancellable event, Player player, Location loc) {
		if ((loc != null) && (WorldLimits.containsKey(loc.getWorld()))) {
			if (!player.hasPermission("pandora.playerlocationlimit.bypass")) {
				// Check limits for this world
				Integer[] limits = WorldLimits.get(loc.getWorld());
				if ((loc.getBlockY() < limits[0]) || (loc.getBlockY() > limits[1])) {	// [0] = minY, [1] = maxY
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "[LocationLimit] You are not allowed to travel beyond this point");
				}
			}
		}
	}	

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerMove(PlayerMoveEvent event) {
		checkMovePerms(event, event.getPlayer(), event.getTo());
	}		

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		checkMovePerms(event, event.getPlayer(), event.getTo());
	} 	
}