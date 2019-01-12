package au.com.addstar.pandora.modules;

import au.com.addstar.bc.BungeeChat;
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
import org.bukkit.event.block.SignChangeEvent;

import java.io.File;

public class SignLogger implements Module, Listener{
    private MasterPlugin mPlugin;

    private Config mConfig;

    private boolean bungeechatenabled = false;

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    private void onSignChange(SignChangeEvent event)
    {
        if(event.getLine(0).isEmpty() && event.getLine(1).isEmpty() && event.getLine(2).isEmpty() && event.getLine(3).isEmpty())
            return;
        
        String locationMessage = String.format("%s(%d,%d,%d)", event.getBlock().getWorld().getName(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
        System.out.println(String.format("[Sign Placement] <%s> %s;%s;%s;%s @ %s", event.getPlayer().getName(), event.getLine(0), event.getLine(1), event.getLine(2), event.getLine(3), locationMessage));

        if(!event.getPlayer().hasPermission("pandora.signlogger.bypass")) {
            String message = String.format("[SIGN] %s: %s;%s;%s;%s @%s", event.getPlayer().getName(),
                    event.getLine(0) + ChatColor.RESET + ChatColor.GRAY,
                    event.getLine(1) + ChatColor.RESET + ChatColor.GRAY,
                    event.getLine(2) + ChatColor.RESET + ChatColor.GRAY,
                    event.getLine(3) + ChatColor.RESET + ChatColor.GRAY,
                    locationMessage);
            if(bungeechatenabled) {
                BungeeChat.mirrorChat(message, mConfig.channel);
            }else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.equals(event.getPlayer()))
                        continue;

                    if (player.hasPermission("pandora.signlogger.listen"))
                        player.sendMessage(ChatColor.GRAY + message);
                }
            }
        }

    }
    
    @Override
    public void onEnable() {
        if(mConfig.load())
            mConfig.save();
        if(!(mConfig.channel.length() > 1)) {
            bungeechatenabled = mPlugin.registerBungeeChat();
            if (!bungeechatenabled) mPlugin.getLogger().warning("BungeeChat is NOT enabled! Cross-server messages will be disabled.");
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance( MasterPlugin plugin ) {
        mConfig = new Config(new File(plugin.getDataFolder(), "SignLogger.yml"));
    }

    private class Config extends AutoConfig
    {
        public Config(File file)
        {
            super(file);
        }

        @ConfigField(comment="The bungee chat channel to broadcast on. Default is '~SS' (the SocialSpy broadcast channel). Setting this to '' will stop messages.")
        public String channel = "~SS";
    }
}
