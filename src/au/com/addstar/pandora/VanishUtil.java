package au.com.addstar.pandora;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kitteh.vanish.VanishPlugin;

public class VanishUtil {
    public static VanishPlugin plugin = (VanishPlugin) Bukkit.getPluginManager().getPlugin("VanishNoPacket");

    public static boolean isPlayerVanished(Player player) {
        return plugin.getManager().isVanished(player);
    }
}
