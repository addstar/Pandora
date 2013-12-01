package au.com.addstar.pandora.modules;

import java.util.WeakHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class AntiAutoFish implements Module, Listener
{
	private MasterPlugin mPlugin;
	private WeakHashMap<Player, Long> mLastCastTime;
	
	public static long minimumWaitTime = 1000;
	
	public AntiAutoFish()
	{
		mLastCastTime = new WeakHashMap<Player, Long>();
	}
	
	@Override
	public void onEnable()
	{
	}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) { mPlugin = plugin; }

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	private void onFish(PlayerFishEvent event)
	{
		if(event.getState() == State.FISHING)
			mLastCastTime.put(event.getPlayer(), System.currentTimeMillis());
		else if(event.getState() == State.CAUGHT_FISH)
		{
			if(mLastCastTime.containsKey(event.getPlayer()))
			{
				long time = System.currentTimeMillis() - mLastCastTime.get(event.getPlayer());
				
				if(time < minimumWaitTime)
				{
					event.setCancelled(true);
					mPlugin.getLogger().info("Prevented " + event.getPlayer().getName() + " from fishing too quickly");
				}
			}
		}
	}
}
