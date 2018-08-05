package au.com.addstar.pandora.modules.gp;

import java.io.File;
import java.io.IOException;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Preconditions;

public class GPClaimData
{
	private final Claim claim;
	private final File file;
	
	private String name;
	
	private Location teleportLocation;
	
	public GPClaimData(Claim claim, File file)
	{
		this.claim = claim;
		this.file = file;
	}
	
	public Claim getClaim()
	{
		return claim;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public Location getTeleport()
	{
		if (teleportLocation == null)
			return null;
		
		return new Location(
			claim.getLesserBoundaryCorner().getWorld(),
			teleportLocation.getX(),
			teleportLocation.getY(),
			teleportLocation.getZ(),
			teleportLocation.getYaw(),
			teleportLocation.getPitch()
			);
	}
	
	public void setTeleport(Location location)
	{
		Preconditions.checkNotNull(location);
		Preconditions.checkArgument(location.getWorld() == claim.getLesserBoundaryCorner().getWorld());
		
		teleportLocation = location;
	}
	
	public void removeTeleport()
	{
		teleportLocation = null;
	}
	
	public void save()
	{
		YamlConfiguration config = new YamlConfiguration();
		
		if (name != null)
			config.set("name", name);
		
		if (teleportLocation != null)
		{
			ConfigurationSection warpSection = config.createSection("warp");
			warpSection.set("x", teleportLocation.getX());
			warpSection.set("y", teleportLocation.getY());
			warpSection.set("z", teleportLocation.getZ());
			warpSection.set("yaw", teleportLocation.getYaw());
			warpSection.set("pitch", teleportLocation.getPitch());
		}
		
		try
		{
			config.save(file);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void load()
	{
		YamlConfiguration config = new YamlConfiguration();
		try
		{
			config.load(file);
		}
		catch (IOException | InvalidConfigurationException e)
		{
			e.printStackTrace();
			return;
		}


		if (config.contains("name"))
			name = config.getString("name");
		
		if (config.contains("warp"))
		{
			ConfigurationSection warpSection = config.getConfigurationSection("warp");
			teleportLocation = new Location(null, warpSection.getDouble("x"), warpSection.getDouble("y"), warpSection.getDouble("z"), (float)warpSection.getDouble("yaw"), (float)warpSection.getDouble("pitch"));
		}
	}
}
