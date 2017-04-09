package au.com.addstar.pandora.modules;

import java.io.*;

import java.util.*;

import com.codisimus.plugins.phatloots.events.PrePlayerLootEvent;
import com.codisimus.plugins.phatloots.PhatLoot;

import org.apache.commons.lang.StringUtils;
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

    final private long MAX_TRACKING_TIME_DAYS = 30;

    /**
     * Map of player name and loot name to a list of recent loot times
     */
    private Map<String, SortedSet<Long>> mPlayerLootHistory;

    /**
     * Map of player name and loot name to the most recent loot time
     */
    private Map<String, Long> mPlayerLootMostRecent;

    /**
     * Task for saving data to disk periodically
     */
    private BukkitTask mTask = null;

    /**
     * File for persisting data
     */
    private File mPlayerLootAccessFile;

    /**
     * Yaml configuration class for persisting data
     */
    FileConfiguration mPlayerLootAccessConfig;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        Bukkit.getMessenger().registerOutgoingPluginChannel(mPlugin, "BungeeChat");

        mPlayerLootHistory = new HashMap<>();

        mPlayerLootMostRecent = new HashMap<>();

        mPlayerLootAccessFile = new File(mPlugin.getDataFolder(), ACCESS_TIMES_FILE);

        try {
            // Create PhatLootsAccessTimes.yml if it does not yet exist
            validateAccessTimesFileExists();
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }

        mPlayerLootAccessConfig = new YamlConfiguration();

        // Populate mPlayerLootHistory and mPlayerLootMostRecent from disk
        loadData();

        mTask = Bukkit.getScheduler().runTaskTimer(mPlugin, new PhatLootsAccessTracker(),
                AUTO_SAVE_INTERVAL_SECONDS * 20, AUTO_SAVE_INTERVAL_SECONDS * 20);
    }

    @Override
    public void onDisable() {

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(mPlugin, "BungeeChat");

        mTask.cancel();

        // Save mPlayerLootHistory and mPlayerLootMostRecent to disk
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
                if (mConfig.debug)
                    System.out.println("[DEBUG] " + p.getName() + " has pandora.lootcooldown.bypass" +
                            "; allowing loot " + lootDescription);
            } else {

                boolean allowLoot;
                if (mConfig.max_loots > 1) {
                    allowLoot = checkLootMulti(p, playerLootKey, lootDescription, event);
                } else {
                    allowLoot = checkLootSingle(p, playerLootKey, lootDescription, event);
                }

                if (!allowLoot)
                    return;

            }

            // Store the current access time for this player/loot combo
            mPlayerLootMostRecent.put(playerLootKey, System.currentTimeMillis());

            // Update the loot history
            synchronized (mPlayerLootHistory) {
                if (mPlayerLootHistory.containsKey(playerLootKey)) {
                    SortedSet<Long> lootHistory = mPlayerLootHistory.get(playerLootKey);
                    lootHistory.add(System.currentTimeMillis());

                    int lootsToTrack = mConfig.max_loots > 1 ? mConfig.max_loots : 5;

                    if (lootHistory.size() > lootsToTrack) {
                        while (lootHistory.size() > lootsToTrack) {
                            lootHistory.remove(lootHistory.first());
                        }
                    }

                } else {
                    SortedSet<Long> lootHistory = new TreeSet<>();
                    lootHistory.add(System.currentTimeMillis());
                    mPlayerLootHistory.put(playerLootKey, lootHistory);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    private boolean checkLootMulti(Player p, String playerLootKey, String lootDescription, PrePlayerLootEvent lootEvent) {

        // Obtain the list of recent loot times by this player for this PhatLoot category (for any chest associated with the PhatLoot)
        SortedSet<Long> lootHistory = mPlayerLootHistory.getOrDefault(playerLootKey, new TreeSet<>());

        if (lootHistory.size() <= 0) {
            if (mConfig.debug)
                System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                        "; not previously accessed");
            return true;

        }

        // Check whether too many loots have occurred within the window
        // The window is based on loot_expiration_minutes in the config coupled with loot_expiration_interval

        Double lootExpirationMinutes = Math.max(mConfig.loot_expiration_minutes, 0);
        int expirationInterval = Math.max(mConfig.loot_expiration_interval, 0);

        if (expirationInterval > 0) {
            // The seed for Random is the oldest access time in lootHistory
            Random rand = new Random(lootHistory.first());

            // Compute the random shift to apply
            // If expirationInterval is 60, randomShift will be a value between -30 and 30
            int randomShift = rand.nextInt(expirationInterval) - expirationInterval / 2;

            // Adjust the loot expiration length using the random shift
            lootExpirationMinutes += randomShift;
        }

        Long thresholdMillis = System.currentTimeMillis() - Math.round(lootExpirationMinutes) * 60 * 1000;

        // Remove expired loot times (looted before thresholdMillis)
        int countRemoved = 0;
        while (lootHistory.size() > 0 && lootHistory.first() < thresholdMillis) {
            lootHistory.remove(lootHistory.first());
            countRemoved += 1;
        }

        if (countRemoved > 0 && mConfig.debug)
            System.out.println("[DEBUG] Removed " + countRemoved + " expired loot history items for " + p.getName());

        int maxLoots = mConfig.max_loots;
        if (maxLoots < 1)
            maxLoots = 1;

        if (lootHistory.size() < maxLoots) {
            if (mConfig.debug)
                System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                        "; " + lootHistory.size() + " / " + maxLoots + " allowed loots " +
                        "in a " + Long.toString(Math.round(lootExpirationMinutes)) + " minute span");
            return true;
        }

        // Compute the time at which the oldest loot in lootHistory will be removed
        double minutesRemaining = (lootHistory.first() - thresholdMillis) / 1000.0 / 60;

        if (minutesRemaining < 0) {
            if (mConfig.debug)
                System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                        "; " + lootHistory.size() + " = " + maxLoots + " allowed loots " +
                        "but minutesRemaining is negative (" + String.format("%.1f", minutesRemaining) + ")");
            return true;
        }

        // Player cannot access the loot chest yet

        String minutesTextPrecise;
        if (minutesRemaining > 2)
            minutesTextPrecise = Long.toString(Math.round(minutesRemaining));
        else
            minutesTextPrecise = String.format("%.1f", minutesRemaining);


        if (mConfig.debug) {
            System.out.println("[DEBUG] " + p.getName() + " denied loot " + lootDescription +
                    "; " + lootHistory.size() + " / " + maxLoots + " allowed loots " +
                    "in a " + Long.toString(Math.round(lootExpirationMinutes)) + " minute span; " +
                    "new loot available in " + minutesTextPrecise + " minutes");
        }

        // Round minutesRemaining to the nearest 5, 15, or 30 minute interval
        // This is done so the user does not know the exact time remaining
        Long minutesRemainingApproximate = getApproximateMinutesRemaining(minutesRemaining);

        // %LOOT_WINDOW% will show the user the rolling loot window length, in minutes
        // Do not use lootExpirationMinutes since that includes the randomized shift value
        String lootWindowString = Long.toString(Math.round(mConfig.loot_expiration_minutes));

        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                mConfig.multi_loot_too_soon
                        .replace("%MAX_LOOTS%", Integer.toString(maxLoots))
                        .replace("%LOOT_WINDOW%", lootWindowString)
                        .replace("%MINUTES%", Long.toString(minutesRemainingApproximate))));

        lootEvent.setCancelled(true);
        p.closeInventory();
        return false;

    }

    private boolean checkLootSingle(Player p, String playerLootKey, String lootDescription, PrePlayerLootEvent lootEvent) {

        // Check the last time the player accessed this PhatLoot category (for any chest associated with the PhatLoot)

        long lastLootMillis = mPlayerLootMostRecent.getOrDefault(playerLootKey, 0L);

        if (lastLootMillis <= 0) {
            if (mConfig.debug)
                System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                        "; not previously accessed");

            return true;

        }
        double elapsedTimeMinutes = (System.currentTimeMillis() - lastLootMillis) / 1000.0 / 60;

        // Determine the cooldown for this loot
        // The time is based on loot_expiration_minutes in the config coupled with loot_expiration_interval

        Double lootExpirationMinutes = mConfig.loot_expiration_minutes;
        int expirationInterval = mConfig.loot_expiration_interval;

        if (expirationInterval > 0) {
            Random rand = new Random(lastLootMillis);

            // Compute the random shift to apply
            // If expirationInterval is 60, randomShift will be a value between -30 and 30
            int randomShift = rand.nextInt(expirationInterval) - expirationInterval / 2;

            // Adjust the loot expiration length using the random shift
            lootExpirationMinutes += randomShift;
        }

        if (elapsedTimeMinutes < lootExpirationMinutes) {

            // Player cannot access the loot chest yet

            double minutesRemaining = lootExpirationMinutes - elapsedTimeMinutes;

            String minutesText;

            if (minutesRemaining > 2)
                minutesText = Long.toString(Math.round(minutesRemaining));
            else
                minutesText = String.format("%.1f", minutesRemaining);

            if (mConfig.debug) {
                System.out.println("[DEBUG] " + p.getName() + " denied loot " + lootDescription +
                        "; available in " + minutesText + " minutes");
            }

            // Round minutesRemaining to the nearest 5, 15, or 30 minute interval
            // This is done so the user does not know the exact time remaining
            Long minutesRemainingApproximate = getApproximateMinutesRemaining(minutesRemaining);

            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    mConfig.loot_too_soon.replace("%MINUTES%", Long.toString(minutesRemainingApproximate))));

            lootEvent.setCancelled(true);
            p.closeInventory();
            return false;
        }

        if (mConfig.debug)
            System.out.println("[DEBUG] " + p.getName() + " allowed loot " + lootDescription +
                    "; last accessed " + String.format("%.1f", elapsedTimeMinutes) + " minutes ago");

        return true;
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

    private Long getApproximateMinutesRemaining(double minutesRemaining) {

        // Round minutesRemaining to the nearest 5, 15, or 30 minute interval

        if (minutesRemaining < 15) {
            return 5 * Math.round(Math.ceil(minutesRemaining / 5.0));
        } else if (minutesRemaining < 60) {
            return 15 * Math.round(minutesRemaining / 15.0);
        } else {
            return 30 * Math.round(minutesRemaining / 30.0);
        }
    }

    public void loadData() {
        try {

            mPlayerLootAccessConfig.load(mPlayerLootAccessFile);

            loadAccessHistory();
            loadAccessTimes();

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    private void loadAccessHistory() {

        ConfigurationSection accessHistorySection = mPlayerLootAccessConfig.getConfigurationSection("AccessHistory");

        int playerCount = 0;

        synchronized (mPlayerLootHistory) {
            for (String key : accessHistorySection.getKeys(false)) {

                String accessHistorySaved = accessHistorySection.getString(key);

                if (accessHistorySaved.equals(null) || accessHistorySaved.equals(""))
                    continue;

                SortedSet<Long> accessHistory = new TreeSet<>();

                for (String s : accessHistorySaved.split(","))
                    accessHistory.add(Long.parseLong(s));

                mPlayerLootHistory.put(key, accessHistory);

                playerCount++;
            }
        }

        if (mConfig.debug)
            System.out.println("[DEBUG] Access history loaded from " + mPlayerLootAccessFile.getPath() +
                    "; count=" + playerCount);

    }

    private void loadAccessTimes() {

        ConfigurationSection accessTimesSection = mPlayerLootAccessConfig.getConfigurationSection("AccessTimes");

        int playerCount = 0;

        synchronized (mPlayerLootMostRecent) {
            for (String key : accessTimesSection.getKeys(false)) {

                Long lastAccessTimeSaved = accessTimesSection.getLong(key);
                Long lastAccessTimeMemory = mPlayerLootMostRecent.getOrDefault(key, 0L);

                mPlayerLootMostRecent.put(key, Math.max(lastAccessTimeSaved, lastAccessTimeMemory));

                playerCount++;
            }
        }

        if (mConfig.debug)
            System.out.println("[DEBUG] Access times loaded from " + mPlayerLootAccessFile.getPath() +
                    "; count=" + playerCount);

    }

    public void saveData() {
        try {
            Long saveThresholdMillis = System.currentTimeMillis() - MAX_TRACKING_TIME_DAYS * 86400L * 1000L;

            synchronized (mPlayerLootHistory) {

                // Convert each list of access times in mPlayerLootHistory to a comma-separated list

                Map<String, String> lootHistoryToSave = new HashMap<>();

                for (String key : mPlayerLootHistory.keySet()) {
                    SortedSet<Long> historyItemSet = mPlayerLootHistory.get(key);

                    if (historyItemSet.isEmpty() || historyItemSet.last() < saveThresholdMillis) {
                        // All loot access for this player was over MAX_TRACKING_TIME_DAYS ago
                        // Do not persist to disk
                        continue;
                    }

                    String historyItems = StringUtils.join(historyItemSet, ',');
                    lootHistoryToSave.put(key, historyItems);
                }

                mPlayerLootAccessConfig.createSection("AccessHistory", lootHistoryToSave);
            }

            synchronized (mPlayerLootMostRecent) {

                Map<String, Long> lootTimesToSave = new HashMap<>();

                for (String key : mPlayerLootMostRecent.keySet()) {
                    Long accessTime = mPlayerLootMostRecent.get(key);
                    if (accessTime < saveThresholdMillis) {
                        // Most recent loot access for this player was over MAX_TRACKING_TIME_DAYS ago
                        // Do not persist to disk
                        continue;
                    }
                    lootTimesToSave.put(key, accessTime);
                }

                mPlayerLootAccessConfig.createSection("AccessTimes", lootTimesToSave);
            }

            mPlayerLootAccessConfig.save(mPlayerLootAccessFile);

            if (mConfig.debug)
                System.out.println("[DEBUG] Access info saved to " + mPlayerLootAccessFile.getPath() +
                        "; count=" + mPlayerLootMostRecent.size());

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

        @ConfigField(comment = "The message players see when they need to wait longer to access a loot chest.\n" +
                "Optionally include placeholder %MINUTES% to state the additional minutes they must wait")
        public String loot_too_soon = "&cSorry, you must wait %MINUTES% minutes to access this loot chest.";

        @ConfigField(comment = "The message players see when they need to wait longer to access a loot chest, provided max_loots is greater than 1.\n" +
                "Available placeholders:\n" +
                " %MAX_LOOTS%   - the max chests that may be accessed\n" +
                " %LOOT_WINDOW% - the rolling loot_expiration_minutes window\n" +
                " %MINUTES%     - the additional minutes they must wait")
        public String multi_loot_too_soon = "&cYou have accessed %MAX_LOOTS% chests of this loot type " +
                "in a %LOOT_WINDOW% minute interval; wait approximately %MINUTES% minutes to access this loot chest";     // or "please try again later"

        @ConfigField(comment = "The time, in minutes, that a loot access event expires\n" +
                "Tracked on a per loot level, not a per chest level")
        public double loot_expiration_minutes = 360;

        @ConfigField(comment = "The time, in minutes, that loot expiration can vary based on a random seed,\n" +
                "as determined by each player's recent loot times (0 for no random values)")
        public int loot_expiration_interval = 60;

        @ConfigField(comment = "The maximum number of loot access events allowed within loot_expiration_minutes")
        public int max_loots = 5;
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