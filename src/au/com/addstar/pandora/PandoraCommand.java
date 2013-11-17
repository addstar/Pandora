package au.com.addstar.pandora;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

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
		}
		else if((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("reload"))
		{
			if(args.length == 1)
			{
				int count = 0;
				HashSet<Module> allMods = new HashSet<Module>(mPlugin.getEnabledModules());
				allMods.addAll(mPlugin.getDisabledModules());
				
				for(Module mod : allMods)
				{
					if(mPlugin.reloadModule(mod))
						++count;
				}
				
				sender.sendMessage(ChatColor.GREEN + "Reloaded ALL Pandora modules.");
				if(count < allMods.size())
					sender.sendMessage("" + ChatColor.RED + count + " Modules failed. See Console.");
			}
			else
			{
				HashSet<Module> allMods = new HashSet<Module>(mPlugin.getEnabledModules());
				allMods.addAll(mPlugin.getDisabledModules());
				for(Module mod : allMods)
				{
					if(args[1].equalsIgnoreCase(mod.getName().replace(' ', '_')))
					{
						if(mPlugin.reloadModule(mod))
							sender.sendMessage(ChatColor.GREEN + "Reloaded " + mod.getName());
						else
							sender.sendMessage(ChatColor.RED + "Failed to reload " + mod.getName());
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String alias, String[] args )
	{
		if(args.length == 0 || args.length == 1)
			return Arrays.asList("reload");
		else if(args.length == 2 && args[0].equalsIgnoreCase("reload"))
		{
			ArrayList<String> modules = new ArrayList<String>();
			for(Module mod : mPlugin.getEnabledModules())
			{
				String name = mod.getName().replace(' ', '_');
				
				if(args[1].isEmpty() || name.startsWith(args[1]))
					modules.add(name);
			}
			for(Module mod : mPlugin.getDisabledModules())
			{
				String name = mod.getName().replace(' ', '_');
				
				if(args[1].isEmpty() || name.startsWith(args[1]))
					modules.add(name);
			}
			
			return modules;
		}
		return null;
	}
	

}
