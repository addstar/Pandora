package au.com.addstar.pandora;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MasterPlugin extends JavaPlugin
{
	private HashMap<String, Module> mLoadedModules;
	
	private HashMap<String, ModuleDefinition> mAvailableModules;
	private HashMap<String, ModuleDefinition> mAvailableModulesByName;
	
	private Config mConfig;
	
	private static MasterPlugin mInstance;
	public static MasterPlugin getInstance()
	{
		return mInstance;
	}
	
	public MasterPlugin()
	{
		mAvailableModules = new HashMap<String, ModuleDefinition>();
		mAvailableModulesByName = new HashMap<String, ModuleDefinition>();
		
		mLoadedModules = new HashMap<String, Module>();
	}
	
	private void registerModules()
	{
		registerModule("TrustedHomes", "au.com.addstar.pandora.modules.TrustedHomes", "GriefPrevention");
		registerModule("Quickshop-Griefprevention-Interop", "au.com.addstar.pandora.modules.QuickshopGPInterop", "GriefPrevention", "QuickShop");
		registerModule("Vanish-Citizens-Interop", "au.com.addstar.pandora.modules.VanishCitizensIO", "VanishNoPacket", "Citizens");
		registerModule("AntiChatRepeater", "au.com.addstar.pandora.modules.AntiChatRepeater");
		registerModule("SignLogger", "au.com.addstar.pandora.modules.SignLogger");
		registerModule("KickBanner", "au.com.addstar.pandora.modules.KickBanner");
		registerModule("AntiPortalTrap", "au.com.addstar.pandora.modules.AntiPortalTrap");
		registerModule("LWC-GP-Interop", "au.com.addstar.pandora.modules.LWCGPInterop", "LWC", "GriefPrevention");
		registerModule("GP-WorldGuard-Interop", "au.com.addstar.pandora.modules.GPWGInterop", "GriefPrevention", "WorldGuard");
		registerModule("TPClaim", "au.com.addstar.pandora.modules.TpClaim", "GriefPrevention");
		registerModule("FlyCanceller", "au.com.addstar.pandora.modules.FlyCanceller");
		registerModule("PVPHandler", "au.com.addstar.pandora.modules.PVPHandler", "WorldGuard");
		registerModule("EventManipulator", "au.com.addstar.pandora.modules.EventManipulator");
		registerModule("BeaconFix", "au.com.addstar.pandora.modules.BeaconFix", "ProtocolLib");
		registerModule("AntiPortalEntityTravel", "au.com.addstar.pandora.modules.AntiPortalEntityTravel");
		registerModule("SignColour", "au.com.addstar.pandora.modules.SignColour");
		registerModule("MinigameBCast", "au.com.addstar.pandora.modules.MinigameBroadcaster", "Minigames", "BungeeChatBukkit");
		registerModule("AntiBuild", "au.com.addstar.pandora.modules.AntiBuild");
		registerNMSModule("Autosaver", "au.com.addstar.pandora.modules.autosave.Autosaver", "1_7_R4");
		registerModule("ItemDB", "au.com.addstar.pandora.modules.ItemDB", "Monolith");
		registerModule("ItemGiving", "au.com.addstar.pandora.modules.ItemGiving", "Monolith");
		registerModule("PortalTravelSound", "au.com.addstar.pandora.modules.PortalTravelSound");
		registerModule("LobbyProtection", "au.com.addstar.pandora.modules.LobbyProtection");
		registerModule("SurvivalGamesBCast", "au.com.addstar.pandora.modules.SurvivalGamesBroadcaster", "SurvivalGames", "BungeeChatBukkit");
		registerModule("PlayerList", "au.com.addstar.pandora.modules.PlayerListing", "BungeeChatBukkit");
		//TODO: Register additional modules here
	}
	
	@Override
	public void onEnable()
	{
		mInstance = this;
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
		mInstance = null;
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
			if(instance instanceof Listener)
				HandlerList.unregisterAll((Listener)instance);
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
	public void registerModule(String name, String moduleClass, String... dependencies)
	{
		ModuleDefinition def = new ModuleDefinition(name, moduleClass, dependencies);
		mAvailableModules.put(moduleClass, def);
		mAvailableModulesByName.put(name, def);
	}
	
	/**
	 * Registers a module with NMS code for loading
	 * @param name Name of module
	 * @param moduleClass Class for the module
	 * @param version The CB version that must be loaded. This should be in the form of "1_7_R4"
	 * @param dependencies Names of plugins needed for this module to load
	 */
	public void registerNMSModule(String name, String moduleClass, String version, String... dependencies)
	{
		if(!version.equals(getCBVersion()))
		{
			getLogger().severe("[NMS Module] Cannot load " + name + ". Required: " + version + " has: " + getCBVersion());
			return;
		}
		
		registerModule(name, moduleClass, dependencies);
	}
	
	private String mCBVersion = null;
	private String getCBVersion()
	{
		if(mCBVersion == null)
		{
			String name = Bukkit.getServer().getClass().getName();
			name = name.substring("org.bukkit.craftbukkit.v".length());
			name = name.substring(0,name.indexOf("."));
			mCBVersion = name;
		}
		
		return mCBVersion;
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
	
	private Module createModule(String name, String moduleClass)
	{
		try
		{
			Class<?> rawClazz = Class.forName(moduleClass);
			if(!Module.class.isAssignableFrom(rawClazz))
			{
				getLogger().severe("Module class '" + moduleClass + "' is not an instance of Module!");
				return null;
			}
			
			Module module = rawClazz.asSubclass(Module.class).newInstance();
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
		catch ( ClassNotFoundException e )
		{
			getLogger().severe("Failed to instanciate " + name + ". Class not found");
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
		public final String moduleClass;
		public final String[] dependencies;
		public ModuleDefinition(String name, String moduleClass, String... dependencies)
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
