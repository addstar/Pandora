package au.com.addstar.pandora.modules;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class ClaimSelect implements Module, CommandExecutor
{
	private Plugin mPlugin;

	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		mPlugin = plugin;
		plugin.getCommand("ClaimSelect").setExecutor(this);
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("This command can only be called by players.");
			return true;
		}

		Player player = (Player) sender;
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false);
		if (claim != null) {
			// Claim is found
			Location loc1 = claim.getLesserBoundaryCorner();
			Location loc2 = claim.getGreaterBoundaryCorner();
			Selection sel = new CuboidSelection(player.getWorld(), loc1, loc2);
			WorldEditPlugin wep = (WorldEditPlugin) mPlugin.getServer().getPluginManager().getPlugin("WorldEdit");
			wep.setSelection(player, sel);
			sender.sendMessage(ChatColor.GREEN + "Claim of selected with WorldEdit (" + sel.getArea() + " blocks)");
		} else {
			// Player is not in a claim
			sender.sendMessage(ChatColor.RED + "You are not standing in a claim.");
		}
		return true;
	}
}