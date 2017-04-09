package au.com.addstar.pandora.modules;

import java.io.*;

import java.util.HashMap;
import java.util.Map;

import com.codisimus.plugins.phatloots.events.PrePlayerLootEvent;
import com.codisimus.plugins.phatloots.PhatLoot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class PhatLootsHelper implements Module, Listener {
    private MasterPlugin mPlugin;
    private Config mConfig;

    final private String ACCESS_TIMES_FILE = "PhatLootsAccessTimes.yml";

    final private long AUTO_SAVE_INTERVAL_SECONDS = 900;

    private Map<String, Long> mPlayerLootMap;

    private BukkitTask mTask = null;

    private File mPlayerLootAccessFile;
    FileConfiguration mPlayerLootAccessHistory;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        Bukkit.getMessenger().registerOutgoingPluginChannel(mPlugin, "BungeeChat");

        mPlayerLootMap = new HashMap<>();

        mPlayerLootAccessFile = new File(mPlugin.getDataFolder(), ACCESS_TIMES_FILE);

        try {
            // Create PhatLootsAccessTimes.yml if it does not yet exist
            validateAccessTimesFileExists();
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }

        mPlayerLootAccessHistory = new YamlConfiguration();

        loadData();

        mTask = Bukkit.getScheduler().runTaskTimer(mPlugin, new PhatLootsAccessTracker(),
                AUTO_SAVE_INTERVAL_SECONDS * 20, AUTO_SAVE_INTERVAL_SECONDS * 20);
    }

    @Override
    public void onDisable() {

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(mPlugin, "BungeeChat");

        mTask.cancel();

        // Save mPlayerLootMap to disk
        saveData();
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "PhatLootsHelper.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrePlayerLootEvent(PrePlayerLootEvent event) {

        try {
            Player p = event.getLooter();
            PhatLoot loot = event.getPhatLoot();

            Location location = event.getChest().getBlock().getLocation();
            String chestLocation = (int) location.getX() + " " + (int) location.getY() + " " + (int) location.getZ();
            String lootDescription = loot.name + " at " + chestLocation;

            String playerLootKey = p.getName() + "_" + loot.name;

            if (p.hasPermission("pandora.lootcooldown.bypass")) {
                System.out.println("[DEBUG] " + p.getName() + " has pandora.lootcooldown.bypass" +
                        "; allowing loot " + lootDescription);
            } else {

                // Check the last time the player accessed this PhatLoot category (for any chest associated with the PhatLoot)
                long lastLootMillis = mPlayerLootMap.getOrDefault(playerLootKey, 0L);

                if (lastLootMillis > 0) {
                    double elapsedTimeMinutes = (System.currentTimeMillis() - lastLootMillis) / 1000.0 / 60;

                    Double lootCoolDownMinutes = mConfig.loot_cooldown_minutes;
                    if (elapsedTimeMinutes < lootCoolDownMinutes) {

                        double minutesRemaining = lootCoolDownMinutes - elapsedTimeMinutes;

                        String minutesText;

                        if (minutesRemaining > 2)
                            minutesText = Long.toString(Math.round(minutesRemaining));
                        else
                            minutesText = String.format("%.1f", lootCoolDownMinutes - elapsedTimeMinutes);

                        if (mConfig.debug) {
                            System.out.println("[DEBUG] " + p.getName() + " denied loot " + lootDescription +
                                    "; available in " + minutesText + " minutes");
                        }

                        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mConfig.loot_too_soon.replace("%MINUTES%", minutesText)));

                        event.setCancelled(true);
                        p.closeInventory();
                        return;
                    }

                    if (mConfig.debug)
                        System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                                "; last accessed " + String.format("%.1f", elapsedTimeMinutes) + " minutes ago");

                } else {
                    if (mConfig.debug)
                        System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                                "; not previously accessed");

                }

            }

            // Store the current access time for this player/loot combo
            mPlayerLootMap.put(playerLootKey, System.currentTimeMillis());

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    public void loadData() {
        try {

            mPlayerLootAccessHistory.load(mPlayerLootAccessFile);

            ConfigurationSection accessTimesSection = mPlayerLootAccessHistory.getConfigurationSection("AccessTimes");

            int playerCount = 0;

            for (String key : accessTimesSection.getKeys(false)) {

                Long lastAccessTimeSaved = accessTimesSection.getLong(key);
                Long lastAccessTimeMemory = mPlayerLootMap.getOrDefault(key, 0L);

                if (mConfig.debug && playerCount < 10) {
                    System.out.println("[DEBUG] LoadData" +
                            ", key=" + key +
                            ", saveTime=" + lastAccessTimeSaved);
                }

                mPlayerLootMap.put(key, Math.max(lastAccessTimeSaved, lastAccessTimeMemory));

                playerCount++;
            }

            if (mConfig.debug)
                System.out.println("[DEBUG] Access times loaded from " + mPlayerLootAccessFile.getPath() +
                        "; count=" + playerCount);

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    public void saveData() {
        try {
            mPlayerLootAccessHistory.createSection("AccessTimes", mPlayerLootMap);

            mPlayerLootAccessHistory.save(mPlayerLootAccessFile);

            if (mConfig.debug)
                System.out.println("[DEBUG] Access times saved to " + mPlayerLootAccessFile.getPath() +
                        "; count=" + mPlayerLootMap.size());

        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }

    private void validateAccessTimesFileExists() throws Exception {
        if (!mPlayerLootAccessFile.exists()) {
            mPlayerLootAccessFile.getParentFile().mkdirs();
            copy(mPlugin.getResource(ACCESS_TIMES_FILE), mPlayerLootAccessFile);
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Enable debug messages")
        public boolean debug = false;

        @ConfigField(comment = "The message players see when they need to wait longer to access a loot chest. " +
                " Optionally include placeholder %MINUTES% to inform the user additional minutes they must wait")
        public String loot_too_soon = "&cSorry, you must wait %MINUTES% minutes to access this loot chest.";

        @ConfigField(comment = "The time, in minutes, that a player must wait to access a specific PhatLoot " +
                "(tracked on a per loot level, not a per chest level)")
        public double loot_cooldown_minutes = 60;
    }

    private class PhatLootsAccessTracker implements Runnable {
        @Override
        public void run() {
            if (mConfig.debug)
                System.out.println("[DEBUG] Saving cached data since " + AUTO_SAVE_INTERVAL_SECONDS + " seconds elapsed");

            saveData();
        }
    }
}