package au.com.addstar.pandora.modules.autosave;

import java.util.LinkedList;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.scheduler.BukkitTask;

public class ChunkSaveTask implements Runnable
{
	private boolean mIsRunning;
	
	private Autosaver mModule;
	private BukkitTask mTask;
	private ProgressiveSaveTask mSaveTask;
	
	public ChunkSaveTask(Autosaver module)
	{
		mModule = module;
		
		mTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(), this, module.getConfig().autoSaveInterval, module.getConfig().autoSaveInterval);
	}
	
	public void stop()
	{
		mTask.cancel();
	}
	
	public int getProgress()
	{
		if(mSaveTask == null)
			return 0;
		return mSaveTask.getProgress();
	}
	
	@Override
	public void run()
	{
		if(mIsRunning)
			return;
		
		LinkedList<Chunk> chunksToSave = findChunksForSaving();
		mSaveTask = new ProgressiveSaveTask(chunksToSave);
	}
	
	private LinkedList<Chunk> findChunksForSaving()
	{
		LinkedList<Chunk> chunks = new LinkedList<>();
		
		for(World world : Bukkit.getWorlds())
		{
			for(Chunk chunk : world.getLoadedChunks())
			{
				if(needsSaving(chunk))
					chunks.add(chunk);
			}
		}
		
		return chunks;
	}
	
	private boolean needsSaving(Chunk chunk)
	{
		return ((CraftChunk)chunk).getHandle().a(false); // Chunk.needsSaving(bool)
	}
	
	private class ProgressiveSaveTask implements Runnable
	{
		private BukkitTask mTask;
		private LinkedList<Chunk> mChunksToSave;
		private int mTotal;
		private int mPercentage;
		
		public ProgressiveSaveTask(LinkedList<Chunk> chunks)
		{
			Validate.isTrue(!mIsRunning, "ProgressiveSaveTask is already running. Cannot continue!");
			
			mChunksToSave = chunks;
			mTotal = chunks.size();
			mPercentage = 0;
			
			mIsRunning = true;
			mTask = Bukkit.getScheduler().runTaskTimer(mModule.getPlugin(), this, 1, 1);
		}
		
		@Override
		public void run()
		{
			try
			{
				for(int i = 0; i < mModule.getConfig().autoSaveBatchSize; ++i )
				{
					Chunk chunk = mChunksToSave.poll();
					if(chunk == null)
					{
						stop();
						return;
					}
					mPercentage =((100 * mChunksToSave.size()) / mTotal);
					
					if(chunk.isLoaded())
						saveChunk(chunk);
				}
			}
			finally
			{
				mIsRunning = false;
			}
		}
		
		public void stop()
		{
			mTask.cancel();
			mIsRunning = false;
		}
		
		public int getProgress()
		{
			return mPercentage;
		}
		
		private void saveChunk(Chunk chunk)
		{
			((CraftWorld)chunk.getWorld()).getHandle().getChunkProviderServer().saveChunk(((CraftChunk)chunk).getHandle());
		}
	}
}
