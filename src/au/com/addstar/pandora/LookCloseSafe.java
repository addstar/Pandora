package au.com.addstar.pandora;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.citizensnpcs.Settings;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.util.Util;

public class LookCloseSafe extends Trait
{
	private boolean enabled = Settings.Setting.DEFAULT_LOOK_CLOSE.asBoolean();
	private Player mTarget;
	private double range = Settings.Setting.DEFAULT_LOOK_CLOSE_RANGE.asDouble();
	private boolean realisticLooking = Settings.Setting.DEFAULT_REALISTIC_LOOKING.asBoolean();
	
	private static final Location NPC_LOCATION = new Location(null, 0.0D, 0.0D, 0.0D);
	public LookCloseSafe()
    {
		super("lookclosesafe");
		enabled = true;
    }

	private boolean canSee(Player player)
	{
		return (!realisticLooking || (!(npc.getEntity() instanceof LivingEntity) || ((LivingEntity) npc.getEntity()).hasLineOfSight(player))) && !VanishUtil.isPlayerVanished(player);
	}

	public void configure( CommandContext args )
	{
		range = args.getFlagDouble("range", this.range);
		range = args.getFlagDouble("r", this.range);
		realisticLooking = args.hasFlag('r');
	}

	private void findNewTarget()
	{
		List<Entity> nearby = this.npc.getEntity().getNearbyEntities(range, range, range);
		
		final Location npcLocation = this.npc.getEntity().getLocation(NPC_LOCATION);
		nearby.sort((o1, o2) -> {
			double d1 = o1.getLocation().distanceSquared(npcLocation);
			double d2 = o2.getLocation().distanceSquared(npcLocation);
			return Double.compare(d1, d2);
		});
		
		for(Entity entity : nearby)
		{
			if(entity.getType() != EntityType.PLAYER || CitizensAPI.getNPCRegistry().isNPC(entity) || !canSee((Player)entity))
				continue;
			
			mTarget = (Player)entity;
			return;
		}
	}

	private boolean hasInvalidTarget()
	{
		if ( mTarget == null )
			return true;
		if (!mTarget.isOnline() || mTarget.getWorld() != npc.getEntity().getWorld() || mTarget.getLocation().distanceSquared(npc.getEntity().getLocation()) > range)
		{
			mTarget = null;
			return true;
		}
		return false;
	}

	public void load( DataKey key ) {
		enabled = key.getBoolean("enabled", key.getBoolean(""));

		range = key.getDouble("range", this.range);
		realisticLooking = key.getBoolean("realisticlooking", key.getBoolean("realistic-looking"));
	}

	public void lookClose( boolean lookClose )
	{
		enabled = lookClose;
	}

	public void onDespawn()
	{
		mTarget = null;
	}

	public void run()
	{
		if ( !enabled || !npc.isSpawned() || npc.getNavigator().isNavigating() )
			return;
		
		if ( hasInvalidTarget() )
			findNewTarget();

		
		if ( mTarget != null && canSee(mTarget))
			Util.faceEntity(npc.getEntity(), mTarget);
	}

	public void save( DataKey key )
	{
		if ( key.keyExists("") )
			key.removeKey("");
		if ( key.keyExists("realistic-looking") )
			key.removeKey("realistic-looking");
		key.setBoolean("enabled", enabled);
		key.setDouble("range", range);
		key.setBoolean("realisticlooking", realisticLooking);
	}

	public void setRange( int range )
	{
		this.range = range;
	}

	public void setRealisticLooking( boolean realistic )
	{
		realisticLooking = realistic;
	}

	public boolean toggle()
	{
		enabled = (!(enabled));
		return enabled;
	}

	public String toString()
	{
		return "LookCloseSafe{" + this.enabled + "}";
	}
}
