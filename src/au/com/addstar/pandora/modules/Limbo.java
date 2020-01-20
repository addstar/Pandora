package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

public class Limbo implements Module, Listener {
    private MasterPlugin mPlugin;
    private Config mConfig;
    private Location spawnLoc;
    private World spawnWorld;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        if (mConfig.enabled) {
            spawnWorld = Bukkit.getWorld(mConfig.limboworld);
            spawnLoc = new Location(spawnWorld, 0.5, 65, 0.5, 180, 0);
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "Limbo.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerSpawn(PlayerSpawnLocationEvent e) {
        if (mConfig.enabled) {
            if (mConfig.debug) mPlugin.getLogger().info("[LIMBO] PlayerSpawnLocationEvent start");
            hideThisPlayer(e.getPlayer());
            hideOtherPlayers(e.getPlayer());
            e.setSpawnLocation(spawnLoc);
            e.getPlayer().setFlying(false);
            if (mConfig.debug) mPlugin.getLogger().info("[LIMBO] PlayerSpawnLocationEvent end");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (mConfig.enabled) {
            if (mConfig.debug) mPlugin.getLogger().info("[LIMBO] PlayerJoinEvent start");
            hideThisPlayer(e.getPlayer());
            hideOtherPlayers(e.getPlayer());
            if (mConfig.debug) mPlugin.getLogger().info("[LIMBO] PlayerJoinEvent end");
        }
    }

    // Make all other players hidden from this player (others become invisible)
    private void hideOtherPlayers(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!p.equals(other)) {
                if (mConfig.debug) mPlugin.getLogger().info("[LIMBO]   hideOtherPlayers: hiding " + p.getName() + " from " + other.getName());
                other.hidePlayer(mPlugin, p);
            }
        }
    }

    // Make this player hidden from all other players (player become invisible)
    private void hideThisPlayer(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!p.equals(other)) {
                if (mConfig.debug) mPlugin.getLogger().info("[LIMBO]   hideThisPlayer: hiding " + other.getName() + " from " + p.getName());
                p.hidePlayer(mPlugin, other);
            }
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Should Limbo be enabled on this server?")
        public boolean debug = false;

        @ConfigField(comment = "Should Limbo be enabled on this server?")
        public boolean enabled = false;

        @ConfigField(comment = "The name of the limbo world")
        public String limboworld = "limbo";
    }
}