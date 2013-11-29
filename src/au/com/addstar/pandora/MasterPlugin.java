package au.com.addstar.pandora;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.pandora.modules.AntiAutoFish;
import au.com.addstar.pandora.modules.AntiChatRepeater;
import au.com.addstar.pandora.modules.KeywordFilter;
import au.com.addstar.pandora.modules.QuickshopGPInterop;
import au.com.addstar.pandora.modules.TrustedHomes;
import au.com.addstar.pandora.modules.VanishCitizensIO;

public class MasterPlugin extends JavaPlugin
{
	private LinkedList<Module> mModules;
	private LinkedList<Module> mDisabledModules;
	
	public MasterPlugin()
	{
		mModules = new LinkedList<Module>();
		mDisabledModules = new LinkedList<Module>();
	}
	
	@Override
	public void onEnable()
	{
		getDataFolder().mkdir();
		PandoraCommand cmd = new PandoraCommand(this);
		getCommand("pandora").setExecutor(cmd);
		getCommand("pandora").setTabCompleter(cmd);
		
		registerModule(new TrustedHomes());
		registerModule(new QuickshopGPInterop());
		registerModule(new VanishCitizensIO());
		registerModule(new AntiAutoFish());
		registerModule(new KeywordFilter());
		registerModule(new AntiChatRepeater());
		// Register additional modules here
	}
	
	@Override
	public void onDisable()
	{
		for(Module module : mModules)
			module.onDisable();
		
		mModules.clear();
		mDisabledModules.clear();
	}
	
	public final List<Module> getEnabledModules()
	{
		return Collections.unmodifiableList(mModules);
	}
	
	public final List<Module> getDisabledModules()
	{
		return Collections.unmodifiableList(mDisabledModules);
	}
	
	public final boolean reloadModule(Module module)
	{
		if(mModules.remove(module))
			module.onDisable();
		
		mDisabledModules.remove(module);
		
		try
		{
			module.onEnable();
			mModules.add(module);
			return true;
		}
		catch(Throwable e)
		{
			getLogger().severe("Failed to enable module: " + module.getName());
			e.printStackTrace();
			mDisabledModules.add(module);
			
			return false;
		}
	}
	
	public final void registerModule(Module module)
	{
		module.setPandoraInstance(this);
		
		try
		{
			module.onEnable();
			if(module instanceof Listener)
				Bukkit.getPluginManager().registerEvents((Listener)module, this);
			mModules.add(module);
		}
		catch(Throwable e)
		{
			getLogger().severe("Failed to enable module: " + module.getName());
			e.printStackTrace();
			mDisabledModules.add(module);
		}
	}
}
