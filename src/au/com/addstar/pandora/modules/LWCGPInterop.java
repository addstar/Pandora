package au.com.addstar.pandora.modules;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

import com.griefcraft.lwc.LWC;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class LWCGPInterop implements Module, Listener
{
	private MasterPlugin mPlugin;
	private boolean mIsEnabled = false;
	@Override
	public void onEnable()
	{
		mIsEnabled = true;
		LWC.getInstance().getModuleLoader().registerModule(mPlugin, new GPModule());
	}

	@Override
	public void onDisable()
	{
		mIsEnabled = false;
		LWC.getInstance().getModuleLoader().removeModules(mPlugin);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
	}

	public class GPModule extends JavaModule
	{
		@Override
		public void onRegisterProtection( LWCProtectionRegisterEvent event )
		{
			if(!mIsEnabled)
				return;
			
			PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getUniqueId());
			Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getBlock().getLocation(), true, pdata.lastClaim);

			if(claim != null)
			{
				String reason = claim.allowBuild(event.getPlayer(), event.getBlock().getType());
				
				if(reason != null)
				{
					event.getPlayer().sendMessage(ChatColor.RED + reason);
					event.setCancelled(true);
				}
			}
		}
	}
}
