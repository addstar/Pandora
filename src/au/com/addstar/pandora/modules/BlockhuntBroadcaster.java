package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import nl.Steffion.BlockHunt.Arena.ArenaState;
import nl.Steffion.BlockHunt.Events.EndArenaEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.chatcontrol.api.ChatControlAPI;

import java.io.File;

public class BlockhuntBroadcaster implements Module, Listener {
    private MasterPlugin mPlugin;

    private Config mConfig;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "BlockhuntBroadcast.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBHEndArenaEvent(EndArenaEvent event) {
        // If game ended due to players leaving.. Don't broadcast!
        // (only broadcast when the game ends normally)
        if (event.getArena().playersInArena.size() < event.getArena().minPlayers) return;

        // Only broadcast if the event was triggered while arena was "in game"
        if (!event.getArena().gameState.equals(ArenaState.INGAME)) return;

        String msg;
        if (event.getLosers().size() == 0) {
            // Seekers win
            msg = mConfig.message
                    .replaceAll("%WINTEAM%", "Seekers")
                    .replaceAll("%LOSETEAM%", "Hiders")
                    .replaceAll("%ARENA%", event.getArena().arenaName);
        } else {
            // Hiders win
            msg = mConfig.message
                    .replaceAll("%WINTEAM%", "Hiders")
                    .replaceAll("%LOSETEAM%", "Seekers")
                    .replaceAll("%ARENA%", event.getArena().arenaName);
        }
        ChatControlAPI.sendMessage(mConfig.channel, ChatColor.translateAlternateColorCodes('&', msg));
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String channel = "GamesBCast";

        @ConfigField(comment = "The broadcast message when seekers win")
        public String message = "&9[BlockHunt]&b The &e%WINTEAM%&b have won in &e%ARENA%&b!";
    }
}
