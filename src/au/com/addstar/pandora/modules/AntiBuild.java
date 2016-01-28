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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class AntiBuild implements Module, Listener {
	private boolean Debug = false;
	private MasterPlugin mPlugin;
	private File mFile;
	private FileConfiguration mConfig;
	private HashMap<World, Integer[]> WorldLimits = new HashMap<World, Integer[]>();

	@Override
	public void onEnable() {
		loadConfig();
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
			mFile = new File(mPlugin.getDataFolder(), "AntiBuild.yml");
			if (!mFile.exists())
				mPlugin.saveResource("AntiBuild.yml", false);

			mConfig = YamlConfiguration.loadConfiguration(mFile);
			if (mFile.exists())
				mConfig.load(mFile);

			Debug = mConfig.getBoolean("debug", false);

			if ((mConfig != null) && (mConfig.isConfigurationSection("worlds"))) {
				Set<String> worlds = (Set<String>) mConfig.getConfigurationSection("worlds").getKeys(false);
				System.out.println("Loading world configs...");
				for (String wname : worlds) {
					// Grab all the settings for this minigame
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

	private void checkBuildPerms(Cancellable event, Player player, Location loc) {
		if (!player.hasPermission("pandora.antibuild.bypass")) {
			// Player has no build perms for this world
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "[AntiBuild] You do not have permission to build here");
		}
		else if ((loc != null) && (WorldLimits.containsKey(loc.getWorld()))) {
			if (!player.hasPermission("pandora.antibuild.limits.bypass")) {
				// Check build limits for this world
				Integer[] limits = WorldLimits.get(loc.getWorld());
				if ((loc.getBlockY() < limits[0]) || (loc.getBlockY() > limits[1])) {	// [0] = minY, [1] = maxY
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "[AntiBuild] You are not allowed to build at this level");
				}
			}
		}
	}	
		
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		checkBuildPerms(event, event.getPlayer(), event.getBlockPlaced().getLocation());
	}		
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		checkBuildPerms(event, event.getPlayer(), event.getBlock().getLocation());
	} 	
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onHangingBreak(HangingBreakByEntityEvent event) {	
		Entity entity = event.getRemover();		
		if ((entity instanceof Player)) {		   
			checkBuildPerms(event, (Player) entity, event.getEntity().getLocation());
		}	        
	} 	
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		checkBuildPerms(event, event.getPlayer(), null);
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerPickUpItem(PlayerPickupItemEvent event) {
		checkBuildPerms(event, event.getPlayer(), null);
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		checkBuildPerms(event, event.getPlayer(), null);
	}

}