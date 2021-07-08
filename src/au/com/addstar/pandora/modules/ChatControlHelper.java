package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.chatcontrol.api.ChatControlAPI;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

public class ChatControlHelper implements Module, Listener {
    private MasterPlugin mPlugin;
    private Config mConfig;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();

        mPlugin.getLogger().info("[ChatControlHelper] Registering " + mConfig.channel + " channel...");
        mPlugin.getServer().getMessenger().registerIncomingPluginChannel(
                mPlugin,
                mConfig.channel,
                new ChatControlMirrorListener(mPlugin, mConfig));

        if (mPlugin.getServer().getMessenger().getIncomingChannels(mPlugin).size() > 0) {
            mPlugin.getLogger().info("[ChatControlHelper] Registered the following incoming channels:");
            for (String name : mPlugin.getServer().getMessenger().getIncomingChannels(mPlugin)) {
                mPlugin.getLogger().info("    " + name);
            }
        } else {
            mPlugin.getLogger().info("  No Incoming channels");
        }
    }

    @Override
    public void onDisable() {
        mPlugin.getLogger().info("[ChatControlHelper] Unregistering " + mConfig.channel + " channel...");
        mPlugin.getServer().getMessenger().unregisterOutgoingPluginChannel(mPlugin, mConfig.channel);
    }

    public class ChatControlMirrorListener implements PluginMessageListener {
        private MasterPlugin mPlugin;
        private Config mConfig;
        public ChatControlMirrorListener(MasterPlugin plugin, Config config) {
            this.mPlugin = plugin;
            this.mConfig = config;
        }

        @Override
        public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, @NotNull byte[] bytes) {
            DataInputStream in = new DataInputStream( new ByteArrayInputStream( bytes ) );
            try {
                String type = in.readUTF();
                String channel = in.readUTF();
                if ("json".equals(type)) {
                    // not implemented yet
                } else {
                    // default to string format
                    String msg = in.readUTF();
                    ChatControlAPI.sendMessage(channel, ChatColor.translateAlternateColorCodes('&', msg));
                }
            } catch (IOException e) {
                mPlugin.getLogger().warning("[ChatControlHelper] Failed to handle plugin message!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "ChatControlHelper.yml"));
        mPlugin = plugin;
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The plugin message channel to listen on")
        public String channel = "pandora:chatcontrolmirror";
    }
}