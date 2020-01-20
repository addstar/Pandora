package au.com.addstar.pandora.modules;

import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class BeaconFix implements Module, PacketListener {
    private Plugin mPlugin;

    @Override
    public void onEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Server.UPDATE_TIME)) {
            PacketContainer packet = event.getPacket();
            long time = packet.getLongs().read(0);
            time = time % 24000;
            packet.getLongs().write(0, time);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).normal().types(PacketType.Play.Server.UPDATE_TIME).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return mPlugin;
    }

}
