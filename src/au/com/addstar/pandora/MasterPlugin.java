package au.com.addstar.pandora;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.pandora.modules.AntiAutoFish;
import au.com.addstar.pandora.modules.AntiChatRepeater;
import au.com.addstar.pandora.modules.AntiPortalTrap;
import au.com.addstar.pandora.modules.GPWGInterop;
import au.com.addstar.pandora.modules.KeywordFilter;
import au.com.addstar.pandora.modules.KickBanner;
import au.com.addstar.pandora.modules.LWCGPInterop;
import au.com.addstar.pandora.modules.QuickshopGPInterop;
import au.com.addstar.pandora.modules.SignLogger;
import au.com.addstar.pandora.modules.TpClaim;
import au.com.addstar.pandora.modules.TrustedHomes;
import au.com.addstar.pandora.modules.VanishCitizensIO;

public class MasterPlugin extends JavaPlugin
{
	private HashMap<String, Module> mLoadedModules;
	
	private HashMap<Class<? extends Module>, ModuleDefinition> mAvailableModules;
	private HashMap<String, ModuleDefinition> mAvailableModulesByName;
	
	private Config mConfig;
	
	public MasterPlugin()
	{
		mAvailableModules = new HashMap<Class<? extends Module>, ModuleDefinition>();
		mAvailableModulesByName = new HashMap<String, ModuleDefinition>();
		
		mLoadedModules = new HashMap<String, Module>();
	}
	
	private void registerModules()
	{
		registerModule("TrustedHomes", TrustedHomes.class, "GriefPrevention", "Essentials");
		registerModule("Quickshop-Griefprevention-Interop", QuickshopGPInterop.class, "GriefPrevention", "QuickShop");
		registerModule("Vanish-Citizens-Interop", VanishCitizensIO.class, "VanishNoPacket", "Citizens");
		registerModule("Anti-AutoFish", AntiAutoFish.class);
		registerModule("KeywordHighlighter", KeywordFilter.class);
		registerModule("AntiChatRepeater", AntiChatRepeater.class, "Essentials");
		registerModule("SignLogger", SignLogger.class);
		registerModule("KickBanner", KickBanner.class);
		registerModule("AntiPortalTrap", AntiPortalTrap.class);
		registerModule("LWC-GP-Interop", LWCGPInterop.class, "LWC", "GriefPrevention");
		registerModule("GP-WorldGuard-Interop", GPWGInterop.class, "GriefPrevention", "WorldGuard");
		registerModule("TPClaim", TpClaim.class, "GriefPrevention");
		//TODO: Register additional modules here
	}
	
	@Override
	public void onEnable()
	{
		mConfig = new Config(new File(getDataFolder(), "config.yml"));
		
		getDataFolder().mkdir();
		
		if(mConfig.load())
			mConfig.save();
		
		PandoraCommand cmd = new PandoraCommand(this);
		getCommand("pandora").setExecutor(cmd);
		getCommand("pandora").setTabCompleter(cmd);
		
		registerModules();
		loadModules();
	}
	
	@Override
	public void onDisable()
	{
		for(Entry<String, Module> module : mLoadedModules.entrySet())
		{
			try
			{
				module.getValue().onDisable();
			}
			catch(Throwable e)
			{
				getLogger().severe("Error disabling module: " + module.getKey());
				e.printStackTrace();
			}
		}
		
		mLoadedModules.clear();
		mAvailableModules.clear();
		mAvailableModulesByName.clear();
	}

	public final boolean isModuleLoaded(String module)
	{
		return mLoadedModules.containsKey(module);
	}
	
	public final Set<String> getAllModules()
	{
		return Collections.unmodifiableSet(mAvailableModulesByName.keySet());
	}
	
	public final boolean reloadModule(String module)
	{
		if(!isModuleLoaded(module))
			return loadModule(module);
		
		Module instance = mLoadedModules.get(module);
		
		mLoadedModules.remove(module);
		
		try
		{
			instance.onDisable();
		}
		catch(Throwable e)
		{
			getLogger().severe("Error disabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		try
		{
			instance.onEnable();
		}
		catch(Throwable e)
		{
			getLogger().severe("Error enabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		mLoadedModules.put(module, instance);
		return true;
	}
	
	public final boolean enableModule(String module)
	{
		if(isModuleLoaded(module))
			return false;
		
		return loadModule(module);
	}
	
	public final boolean disableModule(String module)
	{
		if(!isModuleLoaded(module))
			return false;
		
		Module instance = mLoadedModules.get(module);
		
		mLoadedModules.remove(module);
		
		try
		{
			instance.onDisable();
		}
		catch(Throwable e)
		{
			getLogger().severe("Error disabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Registers a module for loading
	 * @param name Name of module
	 * @param moduleClass Class for the module
	 * @param dependencies Names of plugins needed for this module to load
	 */
	public void registerModule(String name, Class<? extends Module> moduleClass, String... dependencies)
	{
		ModuleDefinition def = new ModuleDefinition(name, moduleClass, dependencies);
		mAvailableModules.put(moduleClass, def);
		mAvailableModulesByName.put(name, def);
	}
	
	private void loadModules()
	{
		mLoadedModules.clear();
		
		for(String name : mAvailableModulesByName.keySet())
		{
			if(!mConfig.disabledModules.contains(name.toLowerCase()))
				loadModule(name);
			else
				getLogger().info(String.format("[%s] Not enabling, disabled from config", name));
		}
	}
	
	private boolean loadModule(String name)
	{
		ModuleDefinition module = mAvailableModulesByName.get(name);
		
		String missingDeps = "";
		
		for(String plugin : module.dependencies)
		{
			if(!Bukkit.getPluginManager().isPluginEnabled(plugin))
			{
				if(!missingDeps.isEmpty())
					missingDeps += ", ";
				missingDeps += plugin;
			}
		}
		
		if(!missingDeps.isEmpty())
		{
			getLogger().info(String.format("[%s] Not enabling, missing dependencies: %s", name, missingDeps));
			return false;
		}
		
		Module instance = createModule(module.name, module.moduleClass);
		
		if(instance == null)
			return false;
		
		mLoadedModules.put(module.name, instance);
		
		return true;
	}
	
	private Module createModule(String name, Class<? extends Module> moduleClass)
	{
		try
		{
			Module module = moduleClass.newInstance();
			module.setPandoraInstance(this);
			
			try
			{
				module.onEnable();
				if(module instanceof Listener)
					Bukkit.getPluginManager().registerEvents((Listener)module, this);
				
				return module;
			}
			catch(Throwable e)
			{
				getLogger().severe("Failed to enable module: " + name);
				e.printStackTrace();
			}
		}
		catch(InstantiationException e)
		{
			getLogger().severe("Failed to instanciate " + name);
			e.printStackTrace();
		}
		catch(ExceptionInInitializerError e)
		{
			getLogger().severe("Failed to instanciate " + name);
			e.printStackTrace();
		}
		catch ( IllegalAccessException e )
		{
			getLogger().severe("Failed to instanciate " + name + ". No public default constructor available.");
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField()
		public HashSet<String> disabledModules = new HashSet<String>();
		
		@Override
		protected void onPostLoad() throws InvalidConfigurationException
		{
			HashSet<String> lowerCaseSet = new HashSet<String>(disabledModules.size());
			
			for(String name : disabledModules)
				lowerCaseSet.add(name.toLowerCase());
			
			disabledModules = lowerCaseSet;
		}
	}
	
	private static class ModuleDefinition
	{
		public final String name;
		public final Class<? extends Module> moduleClass;
		public final String[] dependencies;
		public ModuleDefinition(String name, Class<? extends Module> moduleClass, String... dependencies)
		{
			this.name = name;
			this.moduleClass = moduleClass;
			if(dependencies == null)
				this.dependencies = new String[0];
			else
				this.dependencies = dependencies;
		}
	}
}
