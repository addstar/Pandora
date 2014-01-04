package au.com.addstar.pandora.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class JoinQuitNicknames implements Module, Listener
{
	private boolean ModuleEnabled = false;
	
	@EventHandler(ignoreCancelled=true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		if (ModuleEnabled)
		{
			Player p = event.getPlayer();
			if ((p != null) && (!p.getName().equals(p.getDisplayName())))
			{
				String msg = event.getJoinMessage().replaceAll(p.getName(), p.getDisplayName());
				event.setJoinMessage(msg);
			}
		}
	}

	@EventHandler(ignoreCancelled=true)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		if (ModuleEnabled)
		{
			Player p = event.getPlayer();
			if ((p != null) && (!p.getName().equals(p.getDisplayName())))
			{
				String msg = event.getQuitMessage().replaceAll(p.getName(), p.getDisplayName());
				event.setQuitMessage(msg);
			}
		}
	}
	
	@Override
	public void onEnable()
	{
		ModuleEnabled = true;
	}

	@Override
	public void onDisable()
	{
		ModuleEnabled = false;
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

}
