package au.com.addstar.pandora.modules;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class AntiSpawnerChange implements Module, Listener
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

	@EventHandler(ignoreCancelled=true)
	private void onSpawnerChange(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem())
		{
			Block block = event.getClickedBlock();
			ItemStack item = event.getItem();
			
			if (item.getType() == Material.MONSTER_EGG && block.getType() == Material.MOB_SPAWNER)
			{
				if (!event.getPlayer().hasPermission("pandora.spawner.change"))
					event.setUseInteractedBlock(Result.DENY);
			}
		}
	}
}
