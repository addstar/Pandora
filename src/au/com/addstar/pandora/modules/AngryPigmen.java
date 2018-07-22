package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.List;

public class AngryPigmen implements Module, Listener
{
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
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onEat(PlayerItemConsumeEvent event)
	{
		if (event.getPlayer().getWorld().getEnvironment() != Environment.NETHER)
			return;
		
		if (event.getItem().getType() == Material.COOKED_PORKCHOP || event.getItem().getType() == Material.PORKCHOP)
		{
			List<Entity> entities = event.getPlayer().getNearbyEntities(30, 30, 30);
			for (Entity entity : entities)
			{
				if (entity instanceof PigZombie)
				{
					((PigZombie)entity).setAngry(true);
					((PigZombie)entity).setTarget(event.getPlayer());
				}
			}
		}
	}
			
}
