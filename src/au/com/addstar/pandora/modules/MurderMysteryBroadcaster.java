package au.com.addstar.pandora.modules;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import plugily.projects.murdermystery.api.events.game.MMGameStateChangeEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaState;

import java.io.File;

public class MurderMysteryBroadcaster implements Module, Listener {
    private MasterPlugin mPlugin;

    private Config mConfig;
    private boolean bungeechatenabled = false;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        bungeechatenabled = mPlugin.registerBungeeChat();
        if (!bungeechatenabled)
            mPlugin.getLogger().warning("BungeeChat is NOT enabled! Cross-server messages will be disabled.");
    }

    @Override
    public void onDisable() {
        bungeechatenabled = false;
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "MurderMysteryBroadcaster.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMurderMysteryStateChange(MMGameStateChangeEvent event) {
        Arena arena = event.getArena();
        ArenaState state = event.getArenaState();
        String mapname = arena.getMapName();
        String msg;

        if (state == ArenaState.ENDING) {
            if (arena.aliveMurderer() > 0) {
                // Only murder alive, announce murderer win
                msg = mConfig.murdererWin
                        .replaceAll("%MAPNAME%", mapname);
            } else {
                // Innocents are still alive
                msg = mConfig.innocentWin
                        .replaceAll("%MAPNAME%", mapname);
            }

            // Local server broadcast
            Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));

            // Broadcast the message to other servers
            if ((bungeechatenabled) && (!msg.isEmpty()))
                BungeeChat.mirrorChat(ChatColor.translateAlternateColorCodes('&', msg), mConfig.channel);
        }
    }

    private static class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The bungee chat channel to broadcast on. Default is '~BC' (the reserved broadcast channel)")
        public String channel = "~BC";

        @ConfigField(comment = "The broadcast message when the murderer wins")
        public String murdererWin = "&4[&cMurderMystery&4]&b The &cmurderer &bhas won &a%MAPNAME%&b!";

        @ConfigField(comment = "The broadcast message when innoccents win")
        public String innocentWin = "&4[&cMurderMystery&4]&b The &ainnocents &bhave won &a%MAPNAME%&b!";
    }
}