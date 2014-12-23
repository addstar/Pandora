package au.com.addstar.pandora.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class SantasHelper implements Module, CommandExecutor, Listener
{
	private MasterPlugin mPlugin;
	
	private boolean mUseGP;
	
	private File mFoundFile;
	private Set<FoundLocation> mAllFoundBlocks;
	
	private BukkitTask mTask;
	
	@Override
	public void onEnable()
	{
		mPlugin.getCommand("santahelper").setExecutor(this);
		
		mUseGP = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
		
		mAllFoundBlocks = Sets.newHashSet();
		loadFound();
		
		mTask = Bukkit.getScheduler().runTaskTimer(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				saveFound();
			}
		}, 1200, 1200);
	}

	@Override
	public void onDisable()
	{
		saveFound();
		mTask.cancel();
		mPlugin.getCommand("santahelper").setExecutor(null);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
		mFoundFile = new File(plugin.getDataFolder(), "milkcookies.txt");
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	private void onChunkLoad(ChunkLoadEvent event)
	{
		Chunk chunk = event.getChunk();
		for (BlockState tile : chunk.getTileEntities())
		{
			if (tile instanceof Chest)
			{
				Chest chest = (Chest)tile;
				if (chest.getInventory().contains(Material.MILK_BUCKET) && chest.getInventory().contains(Material.COOKIE))
				{
					String owner = "Unknown";
					if (mUseGP)
					{
						Claim claim = GriefPrevention.instance.dataStore.getClaimAt(chest.getLocation(), false);
						if (claim != null)
							owner = claim.getOwnerName();
					}
					
					mAllFoundBlocks.add(new FoundLocation(tile.getBlock(), owner));
				}
			}
		}
	}
	
	private void loadFound()
	{
		if (!mFoundFile.exists())
			return;
		
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(mFoundFile));
			
			String line;
			while ((line = reader.readLine()) != null)
				mAllFoundBlocks.add(new FoundLocation(line));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (reader != null)
					reader.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void saveFound()
	{
		FileWriter writer = null;
		
		try
		{
			writer = new FileWriter(mFoundFile);
			
			for (FoundLocation loc : mAllFoundBlocks)
				writer.write(loc.toString() + "\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (writer != null)
					writer.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
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
	
	private static class FoundLocation
	{
		private BlockVector mPos;
		private String mOwner;
		private String mWorld;
		
		public FoundLocation(Block block, String owner)
		{
			mPos = new BlockVector(block.getX(), block.getY(), block.getZ());
			mWorld = block.getWorld().getName();
			mOwner = owner;
		}
		
		public FoundLocation(String line)
		{
			String[] parts = line.split(",");
			mPos = new BlockVector(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			mWorld = parts[3];
			mOwner = parts[4];
		}
		
		@Override
		public int hashCode()
		{
			return mPos.hashCode() ^ mWorld.hashCode();
		}
		
		@Override
		public boolean equals( Object obj )
		{
			if (!(obj instanceof FoundLocation))
				return false;
			
			return mPos.equals(((FoundLocation)obj).mPos) && mWorld.equals(((FoundLocation)obj).mWorld);
		}
		
		@Override
		public String toString()
		{
			return String.format("%d,%d,%d,%s,%s", mPos.getBlockX(), mPos.getBlockY(), mPos.getBlockZ(), mWorld, mOwner);
		}
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
							String owner = "Unknown";
							if (mUseGP)
							{
								Claim claim = GriefPrevention.instance.dataStore.getClaimAt(chest.getLocation(), false);
								if (claim != null)
									owner = claim.getOwnerName();
							}
							
							mFoundOwners.add(owner);
							mAllFoundBlocks.add(new FoundLocation(tile.getBlock(), owner));
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
			
			saveFound();
		}
	}
}
