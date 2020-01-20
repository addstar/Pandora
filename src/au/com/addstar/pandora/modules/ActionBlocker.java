package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AbstractModule;
import au.com.addstar.pandora.MasterPlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.RegisteredListener;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created for the AddstarMC Project. Created by Narimm on 10/01/2019.
 */
public class ActionBlocker extends AbstractModule implements Listener {

    private File mFile;
    private MasterPlugin mPlugin;
    private FileConfiguration mConfig;
    private HashMap<World, Integer[]> WorldLimits = new HashMap<>();
    private HashMap<World, List<String>> blockedEvents = new HashMap<>();
    private List<String> allEvents = new ArrayList<>();

    @Override
    public void onEnable() {
        loadConfig();
        RegisteredListener registeredListener = new RegisteredListener(this, (listener, event) -> {
            try {
                listener.getClass().getDeclaredMethod("onAction", Event.class).invoke(listener, event);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            }
        }, EventPriority.NORMAL, mPlugin, false);
        EntityInteractEvent.getHandlerList().register(registeredListener);
        for (String eventName : allEvents) {
            try {
                Class clazz = Class.forName(eventName);
                if (clazz.isAssignableFrom(EntityEvent.class)) {
                    HandlerList list = getHandlerList(clazz);
                    list.register(registeredListener);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        mPlugin = null;
        WorldLimits.clear();
        blockedEvents.clear();

    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    public boolean disableListener() {
        if (WorldLimits.isEmpty() && blockedEvents.isEmpty()) return true;
        debugLog("Listener is disabled...");
        return false;
    }

    private boolean loadConfig() {
        try {
            mFile = new File(mPlugin.getDataFolder(), "ActionBlocker.yml");
            if (!mFile.exists())
                mPlugin.saveResource("ActionBlocker.yml", false);
            mConfig = YamlConfiguration.loadConfiguration(mFile);
            if (mFile.exists())
                mConfig.load(mFile);

            debug = mConfig.getBoolean("debug", false);

            if ((mConfig != null) && (mConfig.isConfigurationSection("worlds"))) {
                Set<String> worlds = mConfig.getConfigurationSection("worlds").getKeys(false);
                debugLog("Loading world configs...");
                for (String wname : worlds) {
                    log("World: " + wname);
                    World world = Bukkit.getWorld(wname);
                    if (world != null) {
                        ConfigurationSection wsection = mConfig.getConfigurationSection("worlds." + wname);
                        int minY = wsection.getInt("minY", 0);
                        int maxY = wsection.getInt("maxY", 255);
                        List<String> events = wsection.getStringList("actions");
                        blockedEvents.put(world, events);
                        for (String e : events) {
                            boolean exists = false;
                            for (String a : allEvents) {
                                if (a.equals(e)) exists = true;
                            }
                            if (!exists) allEvents.add(e);
                        }
                        WorldLimits.put(world, new Integer[]{minY, maxY});
                    } else {
                        log("WARNING: Unknown World \"" + wname + "\"!");
                    }
                }
                debugLog("==============================================");
                if (debug) {
                    for (Map.Entry<World, Integer[]> entry : WorldLimits.entrySet()) {
                        World w = entry.getKey();
                        debugLog("World: " + w.getName());
                        debugLog("  minY: " + entry.getValue()[0]);
                        debugLog("  maxY: " + entry.getValue()[1]);
                    }
                }
                debugLog("==============================================");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void onAction(Event e) {
        if (!(e instanceof EntityEvent)) return;
        EntityEvent event = (EntityEvent) e;
        if (!blockedEvents.containsKey(event.getEntity().getWorld())) return;
        World world = event.getEntity().getWorld();
        debugLog(event.getClass().getName() + " is being checked for world " + world.getName());
        if (!(event.getEntity().getLocation().getY() > WorldLimits.get(world)[1]) || !(event.getEntity().getLocation().getY() < WorldLimits.get(world)[0])) {
            debugLog(event.getClass().getName() + " was inside world  Y limits");
            return;
        }
        debugLog(event.getClass().getName() + " is being checked...");
        List<String> events = blockedEvents.get(event.getEntity().getWorld());
        if (events.contains(event.getClass().getName())) {
            if (event instanceof Cancellable) {
                ((Cancellable) event).setCancelled(true);
                debugLog(event.getClass().getName() + " was cancelled by Action Blocker");
                return;
            } else {
                debugLog("Event:" + event.getClass().getName() + " does not implement cancellable and could not be cancelled...");
                return;
            }
        }
    }

    private static HandlerList getHandlerList(Class<? extends Event> clazz) {
        while (clazz.getSuperclass() != null && Event.class.isAssignableFrom(clazz.getSuperclass())) {
            try {
                Method method = clazz.getDeclaredMethod("getHandlerList");
                method.setAccessible(true);
                return (HandlerList) method.invoke(null);
            } catch (NoSuchMethodException var2) {
                clazz = clazz.getSuperclass().asSubclass(Event.class);
            } catch (Exception var3) {
                throw new IllegalPluginAccessException(var3.getMessage());
            }
        }

        throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName());
    }
}


