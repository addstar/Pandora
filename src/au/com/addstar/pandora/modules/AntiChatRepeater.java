package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitTask;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;

public class AntiChatRepeater implements Module, Listener {
    private WeakHashMap<Player, Entry<String, Long>> mLastChat = new WeakHashMap<>();
    private WeakHashMap<Player, Integer> mRepeatCount = new WeakHashMap<>();

    private Config mConfig;
    private MasterPlugin mPlugin;

    private BukkitTask mTask = null;

    private void sendFakeChat(Player player, String message, String format) {
        message = String.format(format, player.getDisplayName(), message);
        player.sendMessage(message);
    }

    private boolean isCommandAllowed(String cmdString) {
        String[] parts = cmdString.split(" ");
        if (parts.length == 0)
            return true;

        String name = parts[0].substring(1);
        Command cmd = Bukkit.getPluginCommand(name);

        if (cmd != null)
            name = cmd.getName();

        if (mConfig.commands.contains(name.toLowerCase()))
            return mConfig.isWhitelist;
        else
            return !mConfig.isWhitelist;
    }

    private boolean isRepeat(Player player, String message) {
        synchronized (mLastChat) {
            if (!mLastChat.containsKey(player))
                return false;

            Entry<String, Long> entry = mLastChat.get(player);

            if (!entry.getKey().equalsIgnoreCase(message))
                return false;

            return (System.currentTimeMillis() - entry.getValue()) < mConfig.timeout;
        }
    }

    private void increaseRepeat(Player player) {
        synchronized (mRepeatCount) {
            Integer count = mRepeatCount.get(player);

            if (count == null) {
                mRepeatCount.put(player, 1);
                return;
            }

            ++count;
            mRepeatCount.put(player, count);
        }
    }

    private void clearRepeat(Player player) {
        synchronized (mRepeatCount) {
            Integer count = mRepeatCount.remove(player);

            if (count != null && count > 1) {
                String message;
                synchronized (mLastChat) {
                    message = mLastChat.get(player).getKey();
                }

                System.out.println(String.format("%s repeated \"%s\" %d times", player.getName(), message, count));

            }
        }
    }

    private void setLast(Player player, String message) {
        synchronized (mLastChat) {
            mLastChat.put(player, new AbstractMap.SimpleEntry<>(message, System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer().hasPermission("pandora.chatrepeat.bypass"))
            return;

        if (isRepeat(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            increaseRepeat(event.getPlayer());
            sendFakeChat(event.getPlayer(), event.getMessage(), event.getFormat());
        } else
            clearRepeat(event.getPlayer());

        setLast(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("pandora.chatrepeat.bypass") || isCommandAllowed(event.getMessage()))
            return;

        if (isRepeat(event.getPlayer(), event.getMessage())) {
            increaseRepeat(event.getPlayer());
            event.setCancelled(true);
        } else
            clearRepeat(event.getPlayer());

        setLast(event.getPlayer(), event.getMessage());
    }

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        mTask = Bukkit.getScheduler().runTaskTimer(mPlugin, new RepeatNotifier(), 20L, 20L);
    }

    @Override
    public void onDisable() {
        mTask.cancel();
        mTask = null;
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "AntiChatRepeater.yml"));
        mPlugin = plugin;
    }

    private static class Config extends AutoConfig {
        Config(File file) {
            super(file);
        }

        @ConfigField
        public boolean isWhitelist = false;

        @ConfigField(comment = "Use just the name the command (without the /) in lowercase.\nDont bother with aliases, aliases are resolved to the real command before being matched against these commands.")
        public HashSet<String> commands = new HashSet<>();

        @ConfigField(name = "timeout", comment = "The time specified as a date diff, after which the same message/command can be repeated.")
        private String timeoutStr = "10s";

        public long timeout;

        @Override
        protected void onPostLoad() throws InvalidConfigurationException {
            timeout = Utilities.parseDateDiff(timeoutStr);
            if (timeout <= 0) {
                timeout = 10000;
                throw new InvalidConfigurationException("Invalid timeout value \"" + timeoutStr + "\"");
            }
        }
    }

    private class RepeatNotifier implements Runnable {
        @Override
        public void run() {
            synchronized (mRepeatCount) {
                Iterator<Entry<Player, Integer>> it = mRepeatCount.entrySet().iterator();
                Entry<String, Long> lastRepeat;

                while (it.hasNext()) {
                    Entry<Player, Integer> player = it.next();

                    if (player.getValue() <= 1)
                        continue;

                    synchronized (mLastChat) {
                        lastRepeat = mLastChat.get(player.getKey());
                    }

                    if ((System.currentTimeMillis() - lastRepeat.getValue()) < mConfig.timeout)
                        continue;

                    System.out.println(String.format("%s repeated \"%s\" %d times", player.getKey().getName(), lastRepeat.getKey(), player.getValue()));

                    it.remove();
                }
            }
        }
    }
}
