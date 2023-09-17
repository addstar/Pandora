package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerExileEvent;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerKingdomEvent;
import com.github.rumsfield.konquest.api.event.town.KonquestTownCaptureEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import me.evilterabite.rplace.events.player.PlayerCreatePixelEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.dynmap.DynmapAPI;

public class Konquest implements Module, Listener {
    private MasterPlugin mPlugin;
    private KonquestAPI api = null;
    private Plugin konquest = null;

    @Override
    public void onEnable() {
        konquest = Bukkit.getPluginManager().getPlugin("Konquest");

        if (konquest != null && konquest.isEnabled()) {
            RegisteredServiceProvider<KonquestAPI> provider = Bukkit.getServicesManager().getRegistration(KonquestAPI.class);
            if (provider != null) {
                api = provider.getProvider();
                mPlugin.getLogger().info("Successfully enabled Konquest API");
            } else {
                mPlugin.getLogger().warning("Failed to enable Konquest API, invalid provider");
            }
        } else {
            mPlugin.getLogger().warning("Failed to enable Konquest API, plugin not found or disabled");
        }
    }

    @Override
    public void onDisable() {
        api = null;
        konquest = null;
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler
    public void kingdomChange(KonquestPlayerKingdomEvent event) {
        mPlugin.getLogger().warning("kingdomChange() handler called");
        KonquestKingdom kingdom = event.getNewKingdom();
        KonquestPlayer player = event.getPlayer();
        mPlugin.getLogger().info(
                String.format("[Pandora] Player %s joined kingdom: %s",
                        player.getBukkitPlayer().getDisplayName(),
                        kingdom.getName()
                )
        );
    }

    @EventHandler
    public void townCaptured(KonquestTownCaptureEvent event) {
        String type = event.isCapital() ? "capitalcapture" : "towncapture";
        String kingdom = event.getNewKingdom().getName();
        // Only take the first word of town name (Capitals are always "SomeKingdoms Capital")
        String town = StringUtils.substringBefore(event.getTown().getName(), " ");
        String player = event.getPlayer().getBukkitPlayer().getDisplayName();
        String cmd = String.format("runalias /discordnotify %s %s %s %s", type, player, kingdom, town);
        mPlugin.getLogger().warning("Running command: " + cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
