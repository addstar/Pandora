package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public class LagSpikeDetector implements Module, Listener {
    private MasterPlugin mPlugin;
    private Config mConfig;
    private BukkitTask ticktask = null;
    private long lastTickTime;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        if (mConfig.enabled) StartTimer();
    }

    @Override
    public void onDisable() {
        StopTimer();
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "LagSpikeDetector.yml"));
        mPlugin = plugin;
    }

    private void StartTimer() {
        // Stop timer task if it's already running
        if (ticktask != null) {
            StopTimer();
        }

        // Start a new timer task
        mPlugin.getLogger().info("Starting Lag Spike Detector task...");
        lastTickTime = 0;
        ticktask = Bukkit.getScheduler().runTaskTimer(mPlugin, new TickCheckTask(), mConfig.startDelay, mConfig.tickFrequency);
    }

    private void StopTimer() {
        // Cancel/clear tick task if running
        if (ticktask != null) {
            mPlugin.getLogger().info("Stopping Lag Spike Detector task...");
            ticktask.cancel();
            ticktask = null;
        }
    }

    private class TickCheckTask implements Runnable {
        @Override
        public void run() {
            // First time in the loop just track the timestamp
            if (lastTickTime == 0) {
                lastTickTime = System.currentTimeMillis();
                return;
            }

            // Measure the time since last tick
            long diff = System.currentTimeMillis() - lastTickTime;

            if (mConfig.debug)
                mPlugin.getLogger().info("[DEBUG] Task timer took " + diff + "ms");

            // Output message if time diff is above warning/critical thresholds
            if (diff > mConfig.critThreshold) {
                mPlugin.getLogger().warning("[LagSpikeDetector] Critical: Last tick took " + diff + "ms to complete");
            } else if (diff > mConfig.warnThreshold) {
                mPlugin.getLogger().warning("[LagSpikeDetector] Warning: Last tick took " + diff + "ms to complete");
            }

            // Always reset tick time to now
            lastTickTime = System.currentTimeMillis();
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Should this module be enabled by default?")
        public boolean enabled = false;

        @ConfigField(comment = "Enable debug mode")
        public boolean debug = false;

        @ConfigField(comment = "Tick frequency")
        public int tickFrequency = 100;

        @ConfigField(comment = "Start delay")
        public int startDelay = 100;

        @ConfigField(comment = "Trigger warning message when a tick takes longer than this time (in milliseconds)")
        public int warnThreshold = 500;

        @ConfigField(comment = "Trigger critical message when a tick takes longer than this time (in milliseconds)")
        public int critThreshold = 1500;
    }
}