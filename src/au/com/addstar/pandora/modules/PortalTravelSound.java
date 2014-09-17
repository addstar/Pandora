package au.com.addstar.pandora.modules;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class PortalTravelSound implements Module, Listener
{
	private MasterPlugin mPlugin;
	
	@Override
	public void onEnable()
	{
	}

	@Override
	public void onDisable()
	{
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPortalTravel( final PlayerPortalEvent event)
	{
		final World source = event.getPlayer().getWorld();
		Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				if (source != event.getPlayer().getWorld())
					event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.PORTAL_TRAVEL, 0.4f, 1);
			}
		}, 2);
	}
}
