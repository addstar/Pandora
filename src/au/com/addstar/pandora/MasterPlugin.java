package au.com.addstar.pandora;

import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.pandora.modules.QuickshopGPInterop;
import au.com.addstar.pandora.modules.TrustedHomes;

public class MasterPlugin extends JavaPlugin
{
	private LinkedList<Module> mModules;
	
	public MasterPlugin()
	{
		mModules = new LinkedList<Module>();
	}
	
	@Override
	public void onEnable()
	{
		registerModule(new TrustedHomes());
		registerModule(new QuickshopGPInterop());
		// Register additional modules here
	}
	
	@Override
	public void onDisable()
	{
		for(Module module : mModules)
			module.onDisable();
		
		mModules.clear();
	}
	
	public final void registerModule(Module module)
	{
		module.setPandoraInstance(this);
		
		module.onEnable();
		
		if(module instanceof Listener)
			Bukkit.getPluginManager().registerEvents((Listener)module, this);
		
		mModules.add(module);
	}
}
