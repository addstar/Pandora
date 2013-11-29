package au.com.addstar.pandora;

import org.bukkit.entity.Player;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

public class VanishUtil
{
	public static boolean isPlayerVanished(Player player)
	{
		try
		{
			return VanishNoPacket.isVanished(player.getName());
		}
		catch ( VanishNotLoadedException e )
		{
			return false;
		}
	}
}
