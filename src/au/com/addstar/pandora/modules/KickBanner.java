package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

public class KickBanner implements Module, Listener {
    private HashMap<String, KickInfo> mKicks = new HashMap<>();

    private Config mConfig;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onKick(PlayerKickEvent event) {
        if (event.getPlayer().isBanned())
            mKicks.remove(event.getPlayer().getName());

        if (!shouldCount(event))
            return;

        KickInfo info = mKicks.get(event.getPlayer().getName());
        if (info == null) {
            info = new KickInfo();
            mKicks.put(event.getPlayer().getName(), info);
        }

        if (System.currentTimeMillis() - info.lastKick > mConfig.kickTimeout)
            info.count = 0;

        info.lastKick = System.currentTimeMillis();
        ++info.count;

        if (info.count >= mConfig.threshold) {
            event.setCancelled(true);
            tempBan(event.getPlayer());
        }
    }

    private boolean shouldCount(PlayerKickEvent event) {
        return event.getReason().contains("Reason:");
    }

    private void tempBan(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("dtb %s %s", player.getName(), mConfig.banTime));
    }

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
        mConfig = new Config(new File(plugin.getDataFolder(), "KickBanner.yml"));
    }

    private static class KickInfo {
        public int count = 0;
        public long lastKick = 0;
    }

    private static class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The number of kicks in a row the user needs before being banned")
        public int threshold = 5;

        @ConfigField(name = "kick-timeout", comment = "A date diff. The time after the last kick, that the count resets")
        private String kickTimeoutStr = "10m";
        public long kickTimeout;

        @ConfigField(name = "ban-time", comment = "A date diff. The time the user is banned for after being kicked <threshold> times")
        public String banTime = "30m";

        @Override
        protected void onPostLoad() throws InvalidConfigurationException {
            if (threshold <= 0)
                throw new InvalidConfigurationException("Threshold must be greater than 0");

            kickTimeout = Utilities.parseDateDiff(kickTimeoutStr);

            if (kickTimeout <= 0)
                throw new InvalidConfigurationException("Bad kick-timeout");

            if (Utilities.parseDateDiff(banTime) <= 0)
                throw new InvalidConfigurationException("Bad ban-time");

            banTime = banTime.replaceAll(" ", "");
        }
    }
}
