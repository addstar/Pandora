package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.chatcontrol.api.ChatControlAPI;
import plugily.projects.buildbattle.api.event.game.BBGameEndEvent;
import plugily.projects.buildbattle.arena.ArenaState;
import plugily.projects.buildbattle.arena.impl.BaseArena;
import plugily.projects.buildbattle.arena.managers.plots.PlotManager;

import java.io.File;
import java.util.List;

public class BuildBattleBroadcaster implements Module, Listener {
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
            String msg = ChatColor.translateAlternateColorCodes('&', mConfig.winmsg
                    .replaceAll("%MAPNAME%", mapname)
                    .replaceAll("%PLAYER%", winner)
                    .replaceAll("%THEME%", arena.getTheme())
                    .replaceAll("%SCORE%", String.valueOf(highestPoints)));

            // Local server broadcast
            Bukkit.getServer().broadcastMessage(msg);

            // Broadcast the message to other servers
            if ((msg != null) && (!msg.isEmpty()))
                ChatControlAPI.sendMessage(mConfig.channel, ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String channel = "GamesBCast";

        @ConfigField(comment = "The broadcast message for winner of the game")
        public String winmsg = "&6[&eBuildBattle&6] &b%PLAYER% &6won theme &b%THEME%";
    }
}