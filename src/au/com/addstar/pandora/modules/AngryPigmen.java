package au.com.addstar.pandora.modules;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

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
		
		if (event.getItem().getType() == Material.GRILLED_PORK || event.getItem().getType() == Material.PORK)
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
