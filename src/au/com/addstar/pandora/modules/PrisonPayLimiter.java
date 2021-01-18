package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.craftconomy3.events.PayAccountEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PrisonPayLimiter implements Module, Listener {
    private MasterPlugin mPlugin;

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAccountPay(PayAccountEvent event) {
        Account from = event.getFromAccount();
        Account to = event.getToAccount();
        double amount = event.getPayAmount();
        Player pFrom = Bukkit.getServer().getPlayer(from.getAccountName());
        Player pTo = Bukkit.getServer().getPlayer(to.getAccountName());

        // Recipient must be online
        if (pTo == null || !pTo.isOnline()) {
            pFrom.sendMessage(ChatColor.RED + "Sorry, you can only pay online players in Prison.");
            event.setCancelled(true);
            return;
        }

        // Check sender/recipient levels
        if (getPrestigeLevel(pFrom) != getPrestigeLevel(pTo)) {
            mPlugin.getLogger().warning("[PrisonPayLimiter] Denied payment of $" + amount + " from "
                    + from.getAccountName() + " to " + to.getAccountName());
            pFrom.sendMessage(ChatColor.RED + "Sorry, you can only pay players on the same Prestige level.");
            event.setCancelled(true);
            return;
        }
    }

    // Check the highest prestige level for a player
    private int getPrestigeLevel(Player p) {
        for (int x = 10; x > 0; x--) {
            String perm = "autosell.multipliers.prestige" + x;
            if (p.hasPermission(perm)) {
                // Return the highest prestige level found
                return x;
            }
        }
        return 0;
    }
}
