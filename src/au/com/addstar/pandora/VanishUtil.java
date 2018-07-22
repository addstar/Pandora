package au.com.addstar.pandora;

import org.bukkit.entity.Player;
import org.kitteh.vanish.staticaccess.VanishNoPacket;

public class VanishUtil
{
	public static boolean isPlayerVanished(Player player)
	{
		try
		{
			return VanishNoPacket.isVanished(player.getName());
		}
		catch ( Exception e )
		{
			return false;
		}
	}
}
