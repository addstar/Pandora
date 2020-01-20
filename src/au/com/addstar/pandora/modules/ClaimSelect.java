package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ClaimSelect implements Module, CommandExecutor {
    private Plugin mPlugin;

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
        plugin.getCommand("ClaimSelect").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be called by players.");
            return true;
        }

        Player player = (Player) sender;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim != null) {
            // Claim is found
            Location loc1 = claim.getLesserBoundaryCorner();
            Location loc2 = claim.getGreaterBoundaryCorner();
            RegionSelector sel = new CuboidRegionSelector(BukkitAdapter.adapt(player.getWorld()), BukkitAdapter.asBlockVector(loc1), BukkitAdapter.asBlockVector(loc2));
            WorldEditPlugin wep = (WorldEditPlugin) mPlugin.getServer().getPluginManager().getPlugin("WorldEdit");
            LocalSession sess = wep.getSession(player);
            sess.setRegionSelector(new BukkitWorld(player.getWorld()), sel);
            sess.dispatchCUISelection(wep.wrapPlayer(player));
            sender.sendMessage(ChatColor.GREEN + "Claim of selected with WorldEdit (" + sel.getArea() + " blocks)");
        } else {
            // Player is not in a claim
            sender.sendMessage(ChatColor.RED + "You are not standing in a claim.");
        }
        return true;
    }
}