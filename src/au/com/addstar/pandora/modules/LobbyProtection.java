package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

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
		boolean inventoryLock = false;
		ItemStack[] inventory;
		ItemStack[] armour;
		boolean clearInventory;
		boolean clearArmour;
		int heldSlot;
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
	
	private void applyInventoryOptions(Player player,  ProtOpts opts) {
		if (opts.clearInventory)
			player.getInventory().clear();
		
		if (opts.clearArmour)
			player.getInventory().setArmorContents(new ItemStack[4]);
		
		if (opts.heldSlot >= 0 && opts.heldSlot < 9)
			player.getInventory().setHeldItemSlot(opts.heldSlot);
		
		if (opts.inventory != null) {
			for (int slot = 0; slot < opts.inventory.length; ++slot) {
				ItemStack item = opts.inventory[slot];
				if (item != null) {
					player.getInventory().setItem(slot, item.clone());
				}
			}
		}
		
		if (opts.armour != null) {
			for (int slot = 0; slot < opts.armour.length; ++slot) {
				ItemStack item = opts.armour[slot];
				if (item != null) {
					item = item.clone();
					switch (slot) {
					case 0: // helmet
						player.getEquipment().setHelmet(item);
						break;
					case 1: // chestplate
						player.getEquipment().setChestplate(item);
						break;
					case 2: // leggings
						player.getEquipment().setLeggings(item);
						break;
					case 3: // boots
						player.getEquipment().setBoots(item);
						break;
					}
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	public void onPlayerSpawn(PlayerJoinEvent e)	{
		ProtOpts opts = protworlds.get(e.getPlayer().getWorld());
		if (opts == null)
			return;
		
		applyInventoryOptions(e.getPlayer(), opts);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent e)	{
		ProtOpts opts = protworlds.get(e.getPlayer().getWorld());
		if (opts == null)
			return;
		
		applyInventoryOptions(e.getPlayer(), opts);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerSpawn(PlayerRespawnEvent e)	{
		ProtOpts opts = protworlds.get(e.getPlayer().getWorld());
		if (opts == null)
			return;
		
		applyInventoryOptions(e.getPlayer(), opts);
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
		
		if (mConfig.getConfigurationSection("worlds") == null)
			return true;

		Set<String> worlds = (Set<String>) mConfig.getConfigurationSection("worlds").getKeys(false);
		for (String w : worlds) {
			World world = Bukkit.getWorld(w);
			if (world == null) {
				mPlugin.getLogger().warning("[LobbyProtection] Invalid world \"" + w + "\"!");
				continue;
			}
			
			ConfigurationSection worldSection = mConfig.getConfigurationSection("worlds." + w);
			
			ProtOpts opts = new ProtOpts();

			// Prevent inventory interaction (lock inventory)
			opts.inventoryLock = worldSection.getBoolean("lockinv", false);
			
			// Inventory options
			opts.clearInventory = worldSection.getBoolean("clearinv", false);
			opts.clearArmour = worldSection.getBoolean("cleararmour", false);
			if (worldSection.isConfigurationSection("inventory")) {
				ItemStack[] inventory = new ItemStack[36];
				ItemStack[] armour = new ItemStack[4];
				ConfigurationSection invSection = worldSection.getConfigurationSection("inventory");
				
				opts.heldSlot = invSection.getInt("held", -1);
				// Hotbar, first 9 slots of inventory
				if (invSection.contains("hotbar")) {
					int index = 0;
					for (String def : invSection.getStringList("hotbar")) {
						if (index >= 9)	{
							mPlugin.getLogger().warning("[LobbyProtection] Too many items specified for hotbar in world " + w);
							break;
						}
						
						ItemStack item = null;
						try	{
							if (!def.equals("-"))
								item = Utilities.getItem(def.split(" "), 0);
						} catch (IllegalArgumentException e) {
							mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for hotbar in world " + w + " '" + def + "': " + e.getMessage());
						}
						inventory[index++] = item;
					}
				}
				
				// Main inventory, 27 slots of inventory skipping the first 9.
				if (invSection.contains("main")) {
					int index = 0;
					for (String def : invSection.getStringList("main"))	{
						if (index >= 27) {
							mPlugin.getLogger().warning("[LobbyProtection] Too many items specified for main inventory in world " + w);
							break;
						}
						
						ItemStack item = null;
						try	{
							// Skip a row
							if (def.equals("--")) {
								index += 9 - (index % 9);
								continue;
							}
							
							if (!def.equals("-"))
								item = Utilities.getItem(def.split(" "), 0);
						} catch (IllegalArgumentException e) {
							mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for main inventory in world " + w + " '" + def + "': " + e.getMessage());
						}
						
						inventory[9 + index++] = item;
					}
				}
				
				// Armour
				if (invSection.isString("helmet")) {
					String def = invSection.getString("helmet");
					try	{
						armour[0] = Utilities.getItem(def.split(" "), 0);
					} catch (IllegalArgumentException e) {
						mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for helmet in world " + w + " '" + def + "': " + e.getMessage());
					}
				}
				if (invSection.isString("chestplate")) {
					String def = invSection.getString("chestplate");
					try	{
						armour[1] = Utilities.getItem(def.split(" "), 0);
					} catch (IllegalArgumentException e) {
						mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for chestplate in world " + w + " '" + def + "': " + e.getMessage());
					}
				}
				if (invSection.isString("leggings")) {
					String def = invSection.getString("leggings");
					try	{
						armour[2] = Utilities.getItem(def.split(" "), 0);
					} catch (IllegalArgumentException e) {
						mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for leggings in world " + w + " '" + def + "': " + e.getMessage());
					}
				}
				if (invSection.isString("boots")) {
					String def = invSection.getString("boots");
					try	{
						armour[3] = Utilities.getItem(def.split(" "), 0);
					} catch (IllegalArgumentException e) {
						mPlugin.getLogger().warning("[LobbyProtection] Invalid item definition for boots in world " + w + " '" + def + "': " + e.getMessage());
					}
				}
				
				opts.inventory = inventory;
				opts.armour = armour;
			}
			

			// Get cause/action params
			for (String c : worldSection.getKeys(false)) {
				String a = worldSection.getString(c).toUpperCase();
				if (c.equals("clearinv") || c.equals("cleararmour") || c.equals("inventory")) {
					continue;
				}
				
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
			}
			protworlds.put(world, opts);
		}

		if (Debug) {
			System.out.println("==============================================");
			for (Map.Entry<World, ProtOpts> entry : protworlds.entrySet()) {
				System.out.println("World: " + entry.getKey().getName());
				ProtOpts opts = entry.getValue();
				System.out.println("  lockinv: " + opts.inventoryLock);
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

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if ((e.getInventory().getType() != InventoryType.PLAYER) && (e.getInventory().getType() != InventoryType.CRAFTING))
			return;

		if (protworlds.isEmpty() || !protworlds.containsKey(e.getWhoClicked().getWorld()))
			return;

		Player p = (Player) e.getWhoClicked();
		ProtOpts opts = protworlds.get(e.getWhoClicked().getWorld());
		if (!p.hasPermission("pandora.invlock.bypass") && (opts != null) && (opts.inventoryLock)) {
			if (Debug) mPlugin.getLogger().info("[LobbyProtection] Cancelling InventoryDragEvent for " + e.getWhoClicked().getName());
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if ((e.getInventory().getType() != InventoryType.PLAYER) && (e.getInventory().getType() != InventoryType.CRAFTING))
			return;

		if (protworlds.isEmpty() || !protworlds.containsKey(e.getWhoClicked().getWorld()))
			return;

		Player p = (Player) e.getWhoClicked();
		ProtOpts opts = protworlds.get(e.getWhoClicked().getWorld());
		if (!p.hasPermission("pandora.invlock.bypass") && (opts != null) && (opts.inventoryLock)) {
			if (Debug) mPlugin.getLogger().info("[LobbyProtection] Cancelling InventoryClickEvent for " + e.getWhoClicked().getName());
			e.setCancelled(true);
		}
	}
}