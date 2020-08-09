package au.com.addstar.pandora.modules;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.monolith.util.Messenger;
import au.com.addstar.monolith.util.kyori.adventure.text.Component;
import au.com.addstar.monolith.util.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.plajer.buildbattle.api.event.game.BBGameEndEvent;
import pl.plajer.buildbattle.arena.ArenaState;
import pl.plajer.buildbattle.arena.impl.BaseArena;
import pl.plajer.buildbattle.arena.managers.plots.PlotManager;

import java.io.File;
import java.util.List;

public class BuildBattleBroadcaster implements Module, Listener {
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
        mConfig = new Config(new File(plugin.getDataFolder(), "BuildBattleBroadcaster.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBBEndGame(BBGameEndEvent event) {
        BaseArena arena = event.getArena();
        String mapname = arena.getMapName();
        ArenaState state = arena.getArenaState();
        PlotManager pm = arena.getPlotManager();
        List<Player> players = arena.getPlayers();
        int numPlayers = players.size();
        int highestPoints = 0;
        String winner = "";

        // Loop through each player checking their score to find the highest one
        // TODO: Fix condition where more than one player wins (eg. tied scores)
        //       Currently only the first tied player checked will be announced as winner
        for (Player p : players) {
            int points = pm.getPlot(p).getPoints();
            if (points > highestPoints) {
                // Keep track of who has the highest points
                winner = p.getDisplayName();
                highestPoints = points;
            }
        }

        // Ensure at least one person has points
        if (highestPoints > 0) {
            // Build the message to broadcast
            String msg = mConfig.winmsg
                  .replaceAll("%MAPNAME%", mapname)
                  .replaceAll("%PLAYER%", winner)
                  .replaceAll("%THEME%", arena.getTheme())
                  .replaceAll("%SCORE%", String.valueOf(highestPoints));
            Component message = Messenger.parseString(msg);
            // Local server broadcast
            Messenger.sendMessageAll(message);
            // Broadcast the message to other servers
            if ((bungeechatenabled) && (msg != null) && (!msg.isEmpty())) {
                BungeeChat.mirrorChat(LegacyComponentSerializer.legacySection().serialize(message), mConfig.channel);
            }
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The bungee chat channel to broadcast on. Default is '~BC' (the reserved broadcast channel)")
        public String channel = "~BC";

        @ConfigField(comment = "The broadcast message for winner of the game")
        public String winmsg = "<gold>[<yellow>BuildBattle</yellow>] <aqua>%PLAYER%</aqua> won theme <aqua>%THEME%</aqua>";
    }
}