package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class LobbyProtection implements Module, Listener {
	
	private MasterPlugin mPlugin;
	private File mFile;
	private FileConfiguration mConfig;
	private HashMap<World, ProtOpts> protworlds = new HashMap<World, ProtOpts>();
	private boolean Debug = false;

	public static enum ProtAction {
		IGNORE,
		CANCEL,
		SPAWN,
		KILL;
	}

	public class ProtOpts {
		ProtAction defaultOpt = ProtAction.IGNORE;
		HashMap<EntityDamageEvent.DamageCause, ProtAction> causeAction = new HashMap<EntityDamageEvent.DamageCause, ProtAction>();
	}

	@Override
	public void onEnable() {
		loadConfig();

	}

	@Override
	public void onDisable() {
		mConfig = null;
		mFile = null;
		protworlds.clear();
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player))
			return;
		
		if (protworlds.isEmpty() || !protworlds.containsKey(e.getEntity().getWorld()))
			return;

		ProtOpts opts = protworlds.get(e.getEntity().getWorld());
		ProtAction action = opts.causeAction.get(e.getCause());
		if (action == null) {
			action = opts.defaultOpt;
		}
		
		switch (action) {
			case CANCEL:
				if (Debug) mPlugin.getLogger().info("[LobbyProtection] Cancelling damage: " + e.getCause());
				e.setCancelled(true);
				e.setDamage(0.0f);
				break;
			case SPAWN:
				if (Debug) mPlugin.getLogger().info("[LobbyProtection] Spawning player");
				e.setCancelled(true);
				final Player ps = (Player) e.getEntity();
				Location pos = Bukkit.getServer().getWorlds().get(0).getSpawnLocation();
				if (ps.teleport(pos)) {
					ps.setVelocity(new Vector(0, 0, 0));
					ps.setHealth(20.0f);
					ps.setSaturation(20.0f);

					// The best way to prevent lingering damage effects from still being applied after the teleport 
					Bukkit.getScheduler().runTask(mPlugin, new Runnable() {
						@Override
						public void run() {
							ps.setFireTicks(0);
							ps.setNoDamageTicks(40);
							ps.setHealth(20.0f);
							ps.setSaturation(20.0f);
							ps.setFallDistance(0);
							ps.setVelocity(new Vector(0, 0, 0));
						}
					});
				} else {
					mPlugin.getLogger().warning("[LobbyProtection] Spawn teleport failed for " + ps.getName() + "!");
				}
				
				break;
			case KILL:
				if (Debug) mPlugin.getLogger().warning("[LobbyProtection] Killing player");
				e.setDamage(10000000.0f);
				break;
			case IGNORE:
				if (Debug) mPlugin.getLogger().warning("[LobbyProtection] Ignoring damage");
				break;
			default:
				break;
		}
	}

	private boolean loadConfig() {
		mFile = new File(mPlugin.getDataFolder(), "LobbyProtection.yml");
		mConfig = YamlConfiguration.loadConfiguration(mFile);
		
		Debug = mConfig.getBoolean("debug", false);
		
		Set<String> worlds = (Set<String>) mConfig.getConfigurationSection("worlds").getKeys(false);
		for (String w : worlds) {
			World world = Bukkit.getWorld(w);
			if (world == null) {
				mPlugin.getLogger().warning("[LobbyProtection] Invalid world \"" + w + "\"!");
				continue;
			}
			
			ProtOpts opts = new ProtOpts(); 
			Set<String> causes = (Set<String>) mConfig.getConfigurationSection("worlds." + w).getKeys(false);
			for (String c : causes) {
				String a = mConfig.getString("worlds." + w + "." + c).toUpperCase();
				
				ProtAction action;
				try {
					action = ProtAction.valueOf(a);
				}
				catch(Exception e) {
					mPlugin.getLogger().warning("[LobbyProtection] Invalid action \"" + a + "\" on cause \"" + c + "\"!");
					continue;
				}
				
				if (c.equalsIgnoreCase("DEFAULT")) {
					opts.defaultOpt = action;
				} else {
					EntityDamageEvent.DamageCause cause;
					try {
						cause = EntityDamageEvent.DamageCause.valueOf(c.toUpperCase());
					}
					catch(Exception e) {
						mPlugin.getLogger().warning("[LobbyProtection] Invalid cause \"" + c + "\"!");
						continue;
					}
					opts.causeAction.put(cause, action);
				}
				protworlds.put(world, opts);
			}
		}

		if (Debug) {
			System.out.println("==============================================");
			for (Map.Entry<World, ProtOpts> entry : protworlds.entrySet()) {
				System.out.println("World: " + entry.getKey().getName());
				ProtOpts opts = entry.getValue();
				System.out.println("  default: " + opts.defaultOpt);
				for (Map.Entry<EntityDamageEvent.DamageCause, ProtAction> opt : opts.causeAction.entrySet()) {
					ProtAction action = opt.getValue();
					EntityDamageEvent.DamageCause cause = opt.getKey();
					System.out.println("  " + cause + ": " + action);
				}
			}
			System.out.println("==============================================");
		}
		
		return false;
	}

}