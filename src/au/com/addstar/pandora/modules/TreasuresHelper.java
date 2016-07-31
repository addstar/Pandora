package au.com.addstar.pandora.modules;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.robifoxx.treasures.api.*;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class TreasuresHelper implements Module, Listener
{
	private MasterPlugin mPlugin;
	private Config mConfig;

	@Override
	public void onEnable()
	{
		if(mConfig.load())
			mConfig.save();

		Bukkit.getMessenger().registerOutgoingPluginChannel(mPlugin, "BungeeChat");
	}

	@Override
	public void onDisable()
	{
		Bukkit.getMessenger().unregisterOutgoingPluginChannel(mPlugin, "BungeeChat");
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mConfig = new Config(new File(plugin.getDataFolder(), "TreasuresHelper.yml"));
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onTreasureOpen(TreasuresChestOpenEvent event)
	{
		// Ignore the event unless the event is successful (only successful events use up keys or give rewards)
		if (event.getResult() != TreasuresChestOpenResult.SUCCESS)
			return;

		Player p = event.getPlayer();
		PlayerInventory inv = p.getInventory();
		if (mConfig.debug) System.out.println("[DEBUG] TreasuresChestOpenEvent called for " + p.getName());

		// Check how many (normal) inventory slots are free (excluding armour)
		int free = 0;
		for (int x = 0; x < 35; x++) {
			ItemStack is = inv.getItem(x);
			if (is == null) {
				free++;
			}
		}
		if (mConfig.debug) System.out.println("[DEBUG] " + p.getName() + " has " + free + " inventory slots free.");

		// Check if the player has required number of free slots
		if (free < mConfig.min_free_slots) {
			if (mConfig.debug) System.out.println("[DEBUG] Cancelling event for " + p.getName());
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', mConfig.not_enough_room_msg).replace("%slots%", String.valueOf(mConfig.min_free_slots)));
			event.setCancelled(true);
			event.getPlayer().closeInventory();
			TreasuresKeys.addKey(event.getPlayer().getUniqueId().toString(), event.getChestName(), 1);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onTreasureReward(TreasuresFindRewardEvent event)
	{
		if (mConfig.debug) System.out.println("[DEBUG] TreasuresFindRewardEvent called for " + event.getPlayer().getName());

		// Broadcast the message across other servers
		String msg = ChatColor.translateAlternateColorCodes('&', event.getBroadcastMessage());
		BungeeChat.mirrorChat(msg, mConfig.broadcast_channel);
	}
	
	private class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField(comment="Enable debug messages")
		public boolean debug = false;

		@ConfigField(comment="The bungee chat channel to broadcast on. Default is '~BC' (the reserved broadcast channel)")
		public String broadcast_channel = "~BC";

		@ConfigField(comment="The minimum number of slots required to be free to use a key")
		public int min_free_slots = 3;

		@ConfigField(comment="The message players see when they don't have enough slots free")
		public String not_enough_room_msg = "&cSorry, you must have %slots% inventory slots free to use this.";
	}
}