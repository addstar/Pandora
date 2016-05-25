package au.com.addstar.pandora.modules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.permissions.Permissible;

import com.google.common.collect.HashMultimap;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class PlayerListing implements Module, CommandExecutor, Listener
{
	private MasterPlugin mPlugin;
	
	private LinkedHashMap<String, Group> mGroups;
	private ArrayList<Group> mVisualGroups;
	
	public PlayerListing()
	{
		mGroups = new LinkedHashMap<>();
	}
	
	@Override
	public void onEnable()
	{
		mPlugin.getCommand("list").setExecutor(this);
		mGroups.clear();
		
		// Load the config
		try
		{
			File configFile = new File(mPlugin.getDataFolder(), "PlayerListGroups.yml");
			if (!configFile.exists())
				mPlugin.saveResource("PlayerListGroups.yml", false);
			
			FileConfiguration config = new YamlConfiguration();
			if (configFile.exists())
				config.load(configFile);
			else
				return;
			
			ConfigurationSection section = config.getConfigurationSection("groups");
			for (String groupName : section.getKeys(false))
			{
				ConfigurationSection group = section.getConfigurationSection(groupName);
				String memPerm = group.getString("member-perm");
				String seePerm = group.getString("see-perm");
				
				if (memPerm == null || memPerm.isEmpty())
					throw new InvalidConfigurationException("Cannot have empty member permission for group " + groupName);
				mGroups.put(groupName.toLowerCase(), new Group(groupName, memPerm, seePerm));
			}
		}
		catch(InvalidConfigurationException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		mVisualGroups = new ArrayList<>(mGroups.values());
		mVisualGroups.add(new Group("Players", null, null));
		mVisualGroups.add(new Group("Console", null, null));
	}

	@Override
	public void onDisable()
	{
		mPlugin.getCommand("list").setExecutor(null);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerLogin(final PlayerLoginEvent event)
	{
		Group playerGroup = null;
		for (Group group : mGroups.values())
		{
			if (event.getPlayer().hasPermission(group.member))
			{
				playerGroup = group;
				break;
			}
		}
		
		if (playerGroup != null)
		{
			final Group finalGroup = playerGroup;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> BungeeChat.getSyncManager().setPlayerProperty(event.getPlayer().getUniqueId(), "AS:group", finalGroup.name.toLowerCase()), 2);
		}
	}

	private boolean isVanished(CommandSender player, Map<String, Object> visibility)
	{
		// Is the player vanished
		UUID id = PlayerManager.getUniqueId(player);
		if (id == null)
			return false;
		Object obj = visibility.get(id.toString());
		return obj instanceof Boolean && (Boolean) obj;
	}
	
	private String getGroupName(CommandSender player, Map<String, Object> groups)
	{
		Object obj = groups.get(PlayerManager.getUniqueId(player).toString());
		if (obj instanceof String)
			return (String)obj;
		return null;
	}
	
	private boolean isConsole(Permissible sender)
	{
		return (sender instanceof ConsoleCommandSender);
	}
	
	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
	{
		BungeeChat.getSyncManager().getPropertiesAsync("VNP:vanished", new IMethodCallback<Map<String,Object>>()
		{
			@Override
			public void onFinished( final Map<String, Object> visibility )
			{
				BungeeChat.getSyncManager().getPropertiesAsync("AS:group", new IMethodCallback<Map<String,Object>>()
				{
					@Override
					public void onFinished( Map<String, Object> groups )
					{
						displayPlayerList(sender, visibility, groups);
					}
					
					@Override
					public void onError( String type, String message )
					{
						sender.sendMessage(ChatColor.RED + "An internal error occured while executing that command");
					}
				});
			}
			
			@Override
			public void onError( String type, String message )
			{
				sender.sendMessage(ChatColor.RED + "An internal error occured while executing that command");
			}
		});
		
		return true;
	}
	
	private String getName(CommandSender player)
	{
		String name = BungeeChat.getPlayerManager().getPlayerNickname(player);
		if (name == null || name.isEmpty())
			name = player.getName();
		return ChatColor.stripColor(name);
	}
	
	private void displayPlayerList(CommandSender sender, Map<String, Object> visibility, Map<String, Object> groups)
	{
		int playerCount = 0;
		int hiddenCount = 0;
		
		boolean seeHidden = sender.hasPermission("pandora.list.see.hidden");
		boolean seeConsole = sender.hasPermission("pandora.list.see.console");
		
		Collection<CommandSender> allPlayers = BungeeChat.getPlayerManager().getPlayers();

		// Group players, total count, and hide hidden players
		HashMultimap<String, CommandSender> grouped = HashMultimap.create();
		for (CommandSender player : allPlayers)
		{
			boolean vanished = isVanished(player, visibility);
			
			if (vanished)
				++hiddenCount;
			else
				++playerCount;
			
			if (vanished && !seeHidden)
				continue;
			
			String group = getGroupName(player, groups);
			Group g = mGroups.get(group);
			if (g != null)
			{
				if (!sender.isOp() && g.see != null && !g.see.isEmpty() && !sender.hasPermission(g.see))
					group = "players";
			}
			else
				group = "players";
			grouped.put(group, player);
		}
		
		if (seeConsole)
		{
			Set<Permissible> holders = Bukkit.getPluginManager().getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS);
			for(Permissible holder : holders)
			{
				if (!isConsole(holder))
					continue;
				
				// Dont want to see the normal console here
				if (Bukkit.getConsoleSender() == holder)
					continue;
				
				grouped.put("console", (CommandSender)holder);
			}
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(ChatColor.GOLD);
		builder.append("There are ");
		builder.append(ChatColor.RED);
		builder.append(playerCount);
		builder.append(ChatColor.GOLD);
		if (seeHidden && hiddenCount > 0)
		{
			builder.append('/');
			builder.append(hiddenCount);
		}
		
		builder.append(" players online.");
		
		for (Group group : mVisualGroups)
		{
			Set<CommandSender> players = grouped.get(group.name.toLowerCase());
			if(players == null || players.isEmpty())
				continue;
			
			// Sort by name in natural order
			ArrayList<CommandSender> ordered = new ArrayList<>(players);
			Collections.sort(ordered, new Comparator<CommandSender>()
			{
				@Override
				public int compare( CommandSender o1, CommandSender o2 )
				{
					String name1 = getName(o1);
					String name2 = getName(o2);
					return name1.compareToIgnoreCase(name2);
				}
			});
			
			builder.append('\n');
			builder.append(ChatColor.GOLD);
			builder.append(group.name);
			builder.append(ChatColor.WHITE);
			builder.append(": ");
			
			boolean first = true;
			for(CommandSender player : ordered)
			{
				if(!first)
					builder.append(", ");
				first = false;
				
				String name = getName(player);
				if (isVanished(player, visibility))
				{
					builder.append(ChatColor.GRAY);
					builder.append("[HIDDEN]");
					builder.append(ChatColor.WHITE);
				}
				if (isConsole(player))
				{
					builder.append(ChatColor.GRAY);
					builder.append("[CONSOLE]");
					builder.append(ChatColor.WHITE);
				}
				
				// TODO: afk
				
				builder.append(name);
			}
		}
		
		sender.sendMessage(builder.toString());
	}
	
	private class Group
	{
		public String name;
		public String member;
		public String see;
		
		public Group(String name, String memPerm, String seePerm)
		{
			this.name = name;
			member = memPerm;
			see = seePerm;
			
		}
	}
}
