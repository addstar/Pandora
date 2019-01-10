package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.addstar.pandora.AbstractModule;
import au.com.addstar.pandora.MasterPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;

/**
 * Created for the AddstarMC Project. Created by Narimm on 10/01/2019.
 */
public class ActionBlocker extends AbstractModule implements Listener {
    
    private File mFile;
    private MasterPlugin mPlugin;
    private FileConfiguration mConfig;
    private HashMap<World, Integer[]> WorldLimits = new HashMap<>();
    private HashMap<World, List<String>> blockedEvents = new HashMap<>();
    
    
    
    @Override
    public void onEnable() {
        loadConfig();
    }
    
    @Override
    public void onDisable() {
    
    }
    
    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    
    }
    
    public boolean disableListener(){
        if(WorldLimits.isEmpty() && blockedEvents.isEmpty())return true;
        debugLog("Listener is disabled...");
        return false;
    }
    
    private boolean loadConfig(){
        try
        {
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
                        blockedEvents.put(world,events);
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
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    @EventHandler(priority = EventPriority.HIGH,ignoreCancelled = true)
    public void onAction(EntityEvent event){
        if(!blockedEvents.containsKey(event.getEntity().getWorld()))return;
            World  world = event.getEntity().getWorld();
            debugLog(event.getClass().getName() + " is being checked for world "+ world.getName());
            if (!(event.getEntity().getLocation().getY() > WorldLimits.get(world)[1]) || !(event.getEntity().getLocation().getY() < WorldLimits.get(world)[0]) ){
                debugLog(event.getClass().getName() + " was inside world  Y limits");
                return;
            }
            debugLog(event.getClass().getName() + " is being checked...");
            List<String> events =  blockedEvents.get(event.getEntity().getWorld());
            if(events.contains(event.getClass().getName())){
                if(event instanceof Cancellable){
                    ((Cancellable) event).setCancelled(true);
                    debugLog(event.getClass().getName() + " was cancelled by Action Blocker");
                    return;
                }else{
                    debugLog("Event:" + event.getClass().getName() + " does not implement cancellable and could not be cancelled...");
                    return;
                }
            }
            debugLog(event.getClass().getName() + " was allowed to proceed");
    }
}


