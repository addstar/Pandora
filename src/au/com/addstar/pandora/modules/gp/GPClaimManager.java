package au.com.addstar.pandora.modules.gp;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;

public class GPClaimManager implements Listener
{
	private final Map<Long, GPClaimData> claimData;
	private final File root;
	
	public GPClaimManager(File root)
	{
		claimData = Maps.newHashMap();
		this.root = new File(root, "claimextra");
	}
	
	private File getFile(Claim claim)
	{
		return new File(root, claim.getID() + ".yml");
	}
	
	public GPClaimData getData(Claim claim)
	{
		GPClaimData data = claimData.get(claim.getID());
		if (data != null)
			return data;
		
		File file = getFile(claim);
		if (file.exists())
		{
			data = new GPClaimData(claim, file);
			data.load();
			
			claimData.put(claim.getID(), data);
			
			return data;
		}
		else
		{
			data = new GPClaimData(claim, file);
			claimData.put(claim.getID(), data);
			
			return data;
		}
	}
	
	public List<GPClaimData> getData(List<Claim> claims)
	{
		List<GPClaimData> data = Lists.newArrayListWithCapacity(claims.size());
		for (Claim claim : claims)
			data.add(getData(claim));
		
		return data;
	}
	
	@EventHandler
	private void onClaimDestroy(ClaimDeletedEvent event)
	{
		Claim claim = event.getClaim();
		GPClaimData data = claimData.remove(claim.getID());
		if (data != null)
			getFile(claim).delete();
	}
}
