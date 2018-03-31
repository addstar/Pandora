package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Created for the AddstarMC
 * Created by Narimm on 23/08/2017.
 */
public class DeathInterceptor implements Listener, Module {

    private MasterPlugin mPlugin;

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW,ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event){
        String message = event.getDeathMessage();
        Player player  = event.getEntity();
        message = StringUtils.replaceOnce(message, player.getName(), player.getDisplayName());
        Player killer = player.getKiller();
        if(killer != null) {
            message = StringUtils.replaceOnce(message, killer.getName(), killer.getDisplayName());
        }
        event.setDeathMessage(message);
    }

}
