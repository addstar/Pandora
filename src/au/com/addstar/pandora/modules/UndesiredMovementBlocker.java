package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import net.minecraft.server.v1_13_R2.DataWatcher;
import net.minecraft.server.v1_13_R2.EntityFishingHook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UndesiredMovementBlocker implements Module, Listener, PacketListener {
	private Plugin plugin;
	private Cache<Integer, FishHook> hooks;
	
	private Map<Player, Scoreboard> knownScoreboards;
	
	@Override
	public void onEnable() {
		hooks = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.SECONDS).build();
		
		knownScoreboards = Maps.newHashMap();
		
		ProtocolLibrary.getProtocolManager().addPacketListener(this);
	}

	@Override
	public void onDisable() {
		ProtocolLibrary.getProtocolManager().removePacketListener(this);
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	private void onPlayerLeave(PlayerQuitEvent event) {
		knownScoreboards.remove(event.getPlayer());
	}
	
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event) {
		checkPlayerScoreboard(event.getPlayer());
	}
	
	@EventHandler
	private void onMove(PlayerMoveEvent event) {
		checkPlayerScoreboard(event.getPlayer());
	}
	
	/**
	 * Handles preventing and allowing players from being pushed
	 * @param player The player to check
	 */
	private void checkPlayerScoreboard(Player player) {
		Scoreboard known = knownScoreboards.get(player);
		
		if (player.hasPermission("pandora.movement.nopush")) {
			if (known == null || known != player.getScoreboard()) {
				known = player.getScoreboard();
				knownScoreboards.put(player, known);
				sendFakeTeam(player);
			}
		} else {
			if (known != null) {
				deleteFakeTeam(player);
				knownScoreboards.put(player, null);
			}
		}
	}
	
	private static final String FakeTeamName = "\01NOCOLLIDE";
	
	/**
	 * Tells the client that they are part of a team turning off player collision
	 * @param player The player to send to
	 */
	@SuppressWarnings("unchecked")
	private void sendFakeTeam(Player player) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
		packet.getStrings().write(0, FakeTeamName);
		packet.getIntegers().write(1, 0); // Set create mode
		packet.getStrings().write(5, "never"); // Push type
		
		Collection<String> names = packet.getSpecificModifier(Collection.class).read(0);
		names.add(player.getName());
		
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			// Should never happen unless packet changes
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Removes the fake team from the client allowing them to collide again
	 * @param player The player to send to
	 */
	private void deleteFakeTeam(Player player) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
		packet.getStrings().write(0, FakeTeamName);
		packet.getIntegers().write(1, 1); // Set remove mode
		
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			// Should never happen unless packet changes
			throw new AssertionError(e);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onHooked(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof FishHook)) {
			return;
		}
		
		final FishHook hook = (FishHook)event.getEntity();
		if (hook.getShooter() instanceof Player) {
			// Check if they can override hooking players
			if (((Player)hook.getShooter()).hasPermission("pandora.movement.override.hook")) {
				return;
			}
		}
		
		// Remove the fish hook if it hooks into a player
		Bukkit.getScheduler().runTaskLater(plugin, () -> removeFishHookIfNeeded(hook), 0);
		
		// Mark this hook to be checked at a packet level
		hooks.put(hook.getEntityId(), hook);
	}
	
	/**
	 * Removes the fishing hook if it hooked a player.
	 * Note. Since this applies after the hook has been done,
	 * quick players can still pull the player. This is
	 * why the packet level blocking is done
	 * @param entity The fishing hook
	 */
	private void removeFishHookIfNeeded(FishHook entity) {
		EntityFishingHook hook = (EntityFishingHook)(((CraftEntity)entity).getHandle());
		
		if (hook.hooked == null) {
			return;
		}
		
		Entity hookedEnt = hook.hooked.getBukkitEntity();
		if (hookedEnt instanceof HumanEntity) {
			HumanEntity player = (HumanEntity)hookedEnt;
			
			// Can this player be hooked?
			if (!(player instanceof Player) || player.hasPermission("pandora.movement.nohook")) {
				hook.hooked = null;
				hook.die();
			}
		}
	}


	/**
	 * This method guarantees that players can't be hooked by
	 * blocking it at the packet level.
	 * @param event the PacketEvent
	 */
	@Override
	public void onPacketSending(PacketEvent event) {
		//requires updating to the hooked entity id field on http://wiki.vg/Entities#FishingHook
		final int HOOK_ENTITYID_FIELD = 6;
		
		if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
			return;
		}
		
		PacketContainer container = event.getPacket();
		// Is this a fish hook?
		int entId = container.getIntegers().read(0);
		FishHook hook = hooks.getIfPresent(entId);
		
		if (hook == null) {
			return;
		}
		
		// See if this hook has hooked something
		List<?> dataWatcherItems = container.getSpecificModifier(List.class).read(0);
		Iterator<?> it = dataWatcherItems.iterator();
		while (it.hasNext()) {
			DataWatcher.Item<?> item = (DataWatcher.Item<?>)it.next();
			if (item.a().a() == HOOK_ENTITYID_FIELD) {
				// This hook has hooked something
				//.b() returns the actual item we are watching....
				int targetId = (Integer)item.b() - 1;
				HumanEntity hookedPlayer = getPlayer(event.getPlayer().getWorld(), targetId);
				
				if (hookedPlayer != null) {
					// It hooked a player
					// Can this player be hooked?
					if (!(hookedPlayer instanceof Player) || hookedPlayer.hasPermission("pandora.movement.nohook")) {
						// Remove hook status
						it.remove();
					}
				}
			}
		}
		
		if (dataWatcherItems.isEmpty()) {
			event.setCancelled(true);
		}
	}
	
	private HumanEntity getPlayer(World world, int id) {
		for (HumanEntity player : world.getEntitiesByClass(HumanEntity.class)) {
			if (player.getEntityId() == id) {
				return player;
			}
		}
		
		return null;
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
	}

	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).lowest().types(PacketType.Play.Server.ENTITY_METADATA).build();
	}

	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}

