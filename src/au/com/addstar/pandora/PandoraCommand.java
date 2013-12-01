package au.com.addstar.pandora;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class PandoraCommand implements CommandExecutor, TabCompleter
{
	private MasterPlugin mPlugin;
	
	public PandoraCommand(MasterPlugin plugin)
	{
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 0)
		{
			sender.sendMessage(String.format(ChatColor.GRAY + "Pandora version %s:", ChatColor.YELLOW + mPlugin.getDescription().getVersion() + ChatColor.GRAY));
			
			Set<String> modules = mPlugin.getAllModules();
			
			ArrayList<String> all = new ArrayList<String>(modules.size());
			for(String module : modules)
			{
				if(mPlugin.isModuleLoaded(module))
					all.add(ChatColor.GREEN + module);
				else
					all.add(ChatColor.RED + module);
			}

			Collections.sort(all, new Comparator<String>()
			{
				@Override
				public int compare( String o1, String o2 )
				{
					return ChatColor.stripColor(o1).compareTo(ChatColor.stripColor(o2));
				}
			});
			
			String moduleList = "";
			for(String mod : all)
			{
				if(!moduleList.isEmpty())
					moduleList += ", ";
				
				moduleList += mod + ChatColor.GRAY;
			}
			
			sender.sendMessage(moduleList);
		}
		else if((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("reload"))
		{
			if(args.length == 1)
			{
				int count = 0;
				int total = 0;
				Set<String> modules = mPlugin.getAllModules();
				for(String module : modules)
				{
					if(!mPlugin.isModuleLoaded(module))
						continue;
					if(mPlugin.reloadModule(module))
						++count;
					
					++total;
				}
				
				sender.sendMessage(ChatColor.GREEN + "Reloaded ALL Pandora modules.");
				if(count < total)
					sender.sendMessage("" + ChatColor.RED + (total - count) + " Modules failed. See Console.");
			}
			else
			{
				if(mPlugin.reloadModule(args[1]))
					sender.sendMessage(ChatColor.GREEN + "Reloaded " + args[1]);
				else
					sender.sendMessage(ChatColor.RED + "Failed to reload " + args[1]);
			}
		}
		else if(args.length == 2 && args[0].equalsIgnoreCase("disable"))
		{
			if(!mPlugin.isModuleLoaded(args[1]))
				sender.sendMessage(ChatColor.RED + "That module is not loaded.");
			else
			{
				if(mPlugin.disableModule(args[1]))
					sender.sendMessage(ChatColor.GREEN + args[1] + " was disabled.");
				else
					sender.sendMessage(ChatColor.RED + "Unable to disable " + args[1] + ". See console for details.");
			}
		}
		else if(args.length == 2 && args[0].equalsIgnoreCase("enable"))
		{
			if(mPlugin.isModuleLoaded(args[1]))
				sender.sendMessage(ChatColor.RED + "That module is already loaded.");
			else
			{
				if(mPlugin.enableModule(args[1]))
					sender.sendMessage(ChatColor.GREEN + args[1] + " was enabled.");
				else
					sender.sendMessage(ChatColor.RED + "Unable to enable " + args[1] + ". See console for details.");
			}
		}
		return true;
	}
	
	private List<String> matchModules(String module)
	{
		ArrayList<String> matching = new ArrayList<String>();
		for(String name : mPlugin.getAllModules())
		{
			if(module.isEmpty() || name.startsWith(module))
				matching.add(name);
		}
		
		return matching;
	}
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String alias, String[] args )
	{
		if(args.length == 1)
		{
			if(args[0].isEmpty())
				return Arrays.asList("enable", "disable", "reload");
			if("enable".startsWith(args[0].toLowerCase()))
				return Arrays.asList("enable");
			if("disable".startsWith(args[0].toLowerCase()))
				return Arrays.asList("disable");
			if("reload".startsWith(args[0].toLowerCase()))
				return Arrays.asList("reload");
		}
		else if(args.length == 2 && args[0].equalsIgnoreCase("reload"))
			return matchModules(args[1]);
		else if(args.length == 2 && args[0].equalsIgnoreCase("enable"))
			return matchModules(args[1]);
		else if(args.length == 2 && args[0].equalsIgnoreCase("disable"))
			return matchModules(args[1]);
		return null;
	}
	

}
