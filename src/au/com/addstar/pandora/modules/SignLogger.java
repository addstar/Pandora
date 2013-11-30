package au.com.addstar.pandora.modules;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class SignLogger implements Module, Listener
{
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onSignChange(SignChangeEvent event)
	{
		String locationMessage = String.format("%s(%d,%d,%d)", event.getBlock().getWorld().getName(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
		String message = String.format("[Sign Placement] <%s> %s;%s;%s;%s @ %s", event.getPlayer().getName(), event.getLine(0), event.getLine(1), event.getLine(2), event.getLine(3), locationMessage);
		
		System.out.println(message);

		if(!event.getPlayer().hasPermission("pandora.signlogger.bypass"))
		{
			for(Player player : Bukkit.getOnlinePlayers())
			{
				if(player.equals(event.getPlayer()))
					continue;
				
				if(player.hasPermission("pandora.signlogger.listen"))
					player.sendMessage(ChatColor.GRAY + message);
			}
		}
	}
	
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

	@Override
	public String getName()
	{
		return "SignLogger";
	}

}
