package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.LookCloseSafe;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.VanishUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.speech.Talkable;
import net.citizensnpcs.api.ai.speech.event.SpeechBystanderEvent;
import net.citizensnpcs.api.ai.speech.event.SpeechEvent;
import net.citizensnpcs.api.ai.speech.event.SpeechTargetedEvent;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.logging.Logger;

public class VanishCitizensIO implements Listener, Module {
	private Field mTalkableField;
	
	@Override
	public void onEnable()
	{
		if(CitizensAPI.getTraitFactory().getTrait("lookclosesafe") == null)
			CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(LookCloseSafe.class).withName("lookclosesafe"));
		else
			Logger.getLogger("Pandora").warning("Could not register the trait lookclosesafe. It already exists.");
		
		try
		{
			mTalkableField = SpeechEvent.class.getDeclaredField("target");
			mTalkableField.setAccessible(true);
		}
		catch ( NoSuchFieldException | SecurityException e )
		{
			e.printStackTrace();
		}
    }
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	private void onNCPTalk(SpeechTargetedEvent event)
	{
		try
		{
			Talkable talkable = (Talkable)mTalkableField.get(event);
			
			if(talkable.getEntity() instanceof Player)
			{
				Player player = (Player)talkable.getEntity();
				
				if(!CitizensAPI.getNPCRegistry().isNPC(player) && VanishUtil.isPlayerVanished(player))
					event.setCancelled(true);
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException(e);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	private void onNCPTalk(SpeechBystanderEvent event)
	{
		try
		{
			Iterator<Talkable> it = event.getContext().iterator();
			
			while(it.hasNext())
			{
				Talkable t = it.next();
				
				if(t.getEntity() instanceof Player)
				{
					Player player = (Player)t.getEntity();
					if(!CitizensAPI.getNPCRegistry().isNPC(player) && VanishUtil.isPlayerVanished(player))
						it.remove();
				}
			}
			
			if(!event.getContext().hasRecipients())
				event.setCancelled(true);
		}
		catch ( Exception e )
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

}
