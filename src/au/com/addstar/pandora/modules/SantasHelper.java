package au.com.addstar.pandora.modules;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class SantasHelper implements Module, CommandExecutor
{
	private MasterPlugin mPlugin;
	
	private boolean mUseGP;
	private GriefPrevention mGP;
	
	@Override
	public void onEnable()
	{
		mPlugin.getCommand("santahelper").setExecutor(this);
		
		if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention"))
		{
			mUseGP = true;
			mGP = GriefPrevention.getPlugin(GriefPrevention.class);
		}
		else
			mUseGP = false;
	}

	@Override
	public void onDisable()
	{
		mPlugin.getCommand("santahelper").setExecutor(null);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		if (args.length != 1)
			return false;
		
		World world = Bukkit.getWorld(args[0]);
		if (world == null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown world " + args[0]);
			return true;
		}
		
		new FinderTask(sender, world).runTaskTimer(mPlugin, 1, 1);
		sender.sendMessage(ChatColor.GOLD + "Now checking " + world.getName() + " for milk and cookies!");
		
		return true;
	}
	
	private class FinderTask extends BukkitRunnable
	{
		private Iterator<Chunk> mIterator;
		private long mMaxTime;
		
		private List<Location> mFoundLocations;
		private List<String> mFoundOwners;
		private CommandSender mSender;
		
		public FinderTask(CommandSender sender, World world)
		{
			mSender = sender;
			mIterator = Iterators.forArray(world.getLoadedChunks());
			mMaxTime = TimeUnit.MILLISECONDS.toNanos(5);
			mFoundLocations = Lists.newArrayList();
			mFoundOwners = Lists.newArrayList();
		}
		
		@Override
		public void run()
		{
			long startTime = System.nanoTime();
			
			while(mIterator.hasNext())
			{
				if (System.nanoTime() - startTime > mMaxTime)
					break;
				
				Chunk chunk = mIterator.next();
				
				for (BlockState tile : chunk.getTileEntities())
				{
					if (tile instanceof Chest)
					{
						Chest chest = (Chest)tile;
						if (chest.getInventory().contains(Material.MILK_BUCKET) && chest.getInventory().contains(Material.COOKIE))
						{
							mFoundLocations.add(chest.getLocation());
							if (mUseGP)
							{
								Claim claim = mGP.dataStore.getClaimAt(chest.getLocation(), false);
								if (claim != null)
									mFoundOwners.add(claim.getOwnerName());
								else
									mFoundOwners.add("Unknown");
							}
							else
								mFoundOwners.add("Unknown");
						}
					}
				}
			}
			
			cancel();
			
			mSender.sendMessage(ChatColor.GREEN + "Milk and cookie locator results:");
			if (mFoundLocations.isEmpty())
				mSender.sendMessage(ChatColor.GRAY + " - No results");
			for (int i = 0; i < mFoundLocations.size(); ++i)
			{
				Location location = mFoundLocations.get(i);
				mSender.sendMessage(ChatColor.GRAY + String.format(" - %d,%d,%d by %s", location.getBlockX(), location.getBlockY(), location.getBlockZ(), mFoundOwners.get(i)));
			}
		}
	}
}
