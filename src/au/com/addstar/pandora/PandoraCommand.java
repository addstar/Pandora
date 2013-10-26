package au.com.addstar.pandora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PandoraCommand implements CommandExecutor
{
	private MasterPlugin mPlugin;
	
	public PandoraCommand(MasterPlugin plugin)
	{
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length != 0)
			return false;
		
		sender.sendMessage(String.format(ChatColor.GRAY + "Pandora version %s:", ChatColor.YELLOW + mPlugin.getDescription().getVersion() + ChatColor.GRAY));
		
		List<Module> enabled = mPlugin.getEnabledModules();
		List<Module> disabled = mPlugin.getDisabledModules();
		
		ArrayList<String> all = new ArrayList<String>(enabled.size() + disabled.size());
		for(Module mod : enabled)
			all.add(ChatColor.GREEN + mod.getName());
		
		for(Module mod : disabled)
			all.add(ChatColor.RED + mod.getName());
		
		Collections.sort(all, new Comparator<String>()
		{
			@Override
			public int compare( String o1, String o2 )
			{
				return ChatColor.stripColor(o1).compareTo(ChatColor.stripColor(o2));
			}
		});
		
		String modules = "";
		for(String mod : all)
		{
			if(!modules.isEmpty())
				modules += ", ";
			
			modules += mod + ChatColor.GRAY;
		}
		
		sender.sendMessage(modules);
		return true;
	}

}
