package au.com.addstar.pandora.modules;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class SignColour implements Module, Listener
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
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onSignChange(SignChangeEvent event)
	{
		for(int i = 0; i < 4; ++i)
			event.setLine(i, colorize(event.getLine(i), event.getPlayer()));
	}
	
	public static String colorize(String message, CommandSender sender)
	{
		int pos = -1;
		char colorChar = '&';
		
		StringBuilder buffer = new StringBuilder(message);
		
		boolean hasColor = sender.hasPermission("pandora.signs.color");
		boolean hasReset = sender.hasPermission("pandora.signs.format.reset");
		boolean hasBold = sender.hasPermission("pandora.signs.format.bold");
		boolean hasItalic = sender.hasPermission("pandora.signs.format.italic");
		boolean hasUnderline = sender.hasPermission("pandora.signs.format.underline");
		boolean hasStrikethrough = sender.hasPermission("pandora.signs.format.strike");
		boolean hasMagic = sender.hasPermission("pandora.signs.format.magic");
		
		while((pos = message.indexOf(colorChar, pos+1)) != -1)
		{
			if(message.length() > pos + 1)
			{
				char atPos = Character.toLowerCase(message.charAt(pos+1));
				
				boolean allow = false;
				if(((atPos >= '0' && atPos <= '9') || (atPos >= 'a' && atPos <= 'f')) && hasColor)
					allow = true;
				else if(atPos == 'r' && hasReset)
					allow = true;
				else if(atPos == 'l' && hasBold)
					allow = true;
				else if(atPos == 'm' && hasStrikethrough)
					allow = true;
				else if(atPos == 'n' && hasUnderline)
					allow = true;
				else if(atPos == 'o' && hasItalic)
					allow = true;
				else if(atPos == 'k' && hasMagic)
					allow = true;
				
				if(allow)
					buffer.setCharAt(pos, ChatColor.COLOR_CHAR);
			}
		}
		
		return buffer.toString();
	}

}
