package au.com.addstar.pandora.modules.autosave;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class Autosaver implements Module
{
	private Logger mLogger;
	private Config mConfig;
	private Plugin mPlugin;
	
	private RegionFileFlusher mFlushTask;
	private ChunkSaveTask mChunkTask;
	private UserCacheSaveTask mUserCacheTask;
	
	@Override
	public void onEnable()
	{
		if(mConfig.load())
			mConfig.save();
		
		if(mConfig.writeDataEnabled)
			mFlushTask = new RegionFileFlusher(this);
		
		if(mConfig.userCacheSaveEnabled)
			mUserCacheTask = new UserCacheSaveTask(this);
		
		mChunkTask = new ChunkSaveTask(this);
	}

	@Override
	public void onDisable()
	{
		if(mFlushTask != null)
			mFlushTask.stop();
		
		if(mChunkTask != null)
			mChunkTask.stop();
		
		if(mUserCacheTask != null)
			mUserCacheTask.stop();
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
		mLogger = plugin.getLogger();
		mConfig = new Config(new File(plugin.getDataFolder(), "autosaver.yml"));
	}
	
	public Config getConfig()
	{
		return mConfig;
	}
	
	public Plugin getPlugin()
	{
		return mPlugin;
	}

	public class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField(comment="The amount of chunks saved every tick when autosaving\nIf saving causes severe tick lag, lower it, if it takes too long, increase it")
		public int autoSaveBatchSize = 20;
		
		@ConfigField(comment="The tick interval at which the server saves automatically (20 ticks = 1 second)")
		public int autoSaveInterval = 400;
		
		@ConfigField(comment="Whether Pandora will attempt to write all world data to the region files at a set interval\nThis is done on another thread, so don't worry about the main thread lagging while this happens")
		public boolean writeDataEnabled = true;
		
		@ConfigField(comment="The tick interval at which the server actually writes the chunk data to file (20 ticks = 1 second)")
		public int writeDataInterval = 12000;
		
		@ConfigField(comment="Whether Pandora will attempt to save the UserCache\nThis is done on another thread, so don't worry about the thread lagging while this happens")
		public boolean userCacheSaveEnabled = false;
		
		@ConfigField(comment="The tick interval at which the UserCache is saved to file (20 ticks = 1 second)")
		public int userCacheSaveInterval;
		
		@Override
		protected void onPostLoad() throws InvalidConfigurationException
		{
			if(autoSaveBatchSize <= 0)
			{
				mLogger.warning("[Autosaver] Batch size is set too low and has been limited to 1");
				autoSaveBatchSize = 1;
			}
			
			if(autoSaveInterval < 400)
			{
				autoSaveInterval = 400;
				mLogger.warning("[Autosaver] Save interval is set too low and has been limited to a 400 tick (20 second) interval");
			}
			
			if(writeDataInterval < 600)
			{
				writeDataInterval = 600;
				mLogger.warning("[Autosaver] Data write interval is set too low and has been limited to 600 tickets (30 seconds)");
			}
			
			if (writeDataEnabled) 
			{
				double time = writeDataInterval / 20;
				
				String timetext;
				if (time < 60)
					timetext = time + " seconds";
				else if (time < 3600)
					timetext = (time / 60) + " minutes";
				else
					timetext = (time / 3600) + " hours";
				
				mLogger.info("[Autosaver] will write world data to all region files every " + writeDataInterval + " ticks (" + timetext + ")");
			}
			
			if(userCacheSaveInterval < 400)
			{
				userCacheSaveInterval = 400;
				mLogger.warning("[Autosaver] UserCache save interval is set too low and has been limited to a 400 tick (20 second) interval");
			}
			
			if(userCacheSaveEnabled)
			{
				double time = userCacheSaveInterval / 20;
				
				String timetext;
				if (time < 60)
					timetext = time + " seconds";
				else if (time < 3600)
					timetext = (time / 60) + " minutes";
				else
					timetext = (time / 3600) + " hours";
				
				mLogger.info("[Autosaver] will save the UserCache every " + userCacheSaveInterval + " ticks (" + timetext + ")");
			}
		}
	}
}
