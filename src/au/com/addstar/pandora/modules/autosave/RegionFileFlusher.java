package au.com.addstar.pandora.modules.autosave;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.scheduler.BukkitTask;

public class RegionFileFlusher implements Runnable
{
	private Autosaver mModule;
	private BukkitTask mTask;
	private boolean mIsRunning;
	
	public RegionFileFlusher(Autosaver module)
	{
		mModule = module;
		mIsRunning = false;
		
		mTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(), this, module.getConfig().writeDataInterval, module.getConfig().writeDataInterval);
	}
	
	public void stop()
	{
		mTask.cancel();
	}
	
	@Override
	public void run()
	{
		if(mIsRunning)
			return;
		
		mIsRunning = true;
		Bukkit.getScheduler().runTaskAsynchronously(mModule.getPlugin(), new SavingTask(Bukkit.getWorlds()));
	}
	
	private class SavingTask implements Runnable
	{
		private List<World> mWorlds;
		public SavingTask(List<World> worlds)
		{
			mWorlds = worlds;
		}
		
		@Override
		public void run()
		{
			try
			{
				for(World world : mWorlds)
					((CraftWorld)world).getHandle().saveLevel();
			}
			finally
			{
				mIsRunning = false;
			}
		}
	}
}
