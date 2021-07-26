package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class StaffChat implements Module, CommandExecutor {
    private MasterPlugin mPlugin;
    private static final String[] prefixChannels = {"Mod", "HeadMod", "Admin"};
    private static Map<String,String> validCommands = new HashMap<String, String>();
    static {
        validCommands.put("mc","Mod");
        validCommands.put("hmc","HeadMod");
        validCommands.put("ac","Admin");
        validCommands.put("broady","Broady");
        validCommands.put("rawbcast","RawBCast");
    }

    @Override
    public void onEnable() {
        mPlugin.getCommand("mc").setExecutor(this);
        mPlugin.getCommand("hmc").setExecutor(this);
        mPlugin.getCommand("ac").setExecutor(this);
        mPlugin.getCommand("broady").setExecutor(this);
        mPlugin.getCommand("rawbcast").setExecutor(this);
    }

    @Override
    public void onDisable() {
        mPlugin.getCommand("mc").setExecutor(null);
        mPlugin.getCommand("hmc").setExecutor(null);
        mPlugin.getCommand("ac").setExecutor(null);
        mPlugin.getCommand("broady").setExecutor(null);
        mPlugin.getCommand("rawbcast").setExecutor(null);
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        this.mPlugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String c, String[] params) {
        String perm = command.getPermission();
        String channel = validCommands.get(command.getName());
        if (channel != null && !channel.isEmpty()) {
            if (commandSender instanceof Player && !commandSender.hasPermission(perm)) {
                return false;
            }
            String msg = String.join(" ", params);
            if ((ArrayUtils.contains(prefixChannels, channel)) && (commandSender instanceof ConsoleCommandSender)) {
                ConsoleCommandSender con = (ConsoleCommandSender) commandSender;
                if (con.getName() != null && !con.getName().isEmpty() && !con.getName().equalsIgnoreCase("CONSOLE")) {
                    if (Bukkit.getServer().getOnlinePlayers().size() == 0) {
                        mPlugin.getLogger().warning("[StaffChat] Error: Unable to send message - No players online.");
                        return true;
                    }
                    // Prepend the rcon user to message
                    msg = ChatColor.GRAY + "[" + ChatColor.RED + con.getName() + ChatColor.GRAY + "]: " + ChatColor.WHITE + msg;
                }
            }
            mPlugin.sendChatControlMessage(commandSender, channel, msg);
        } else {
            mPlugin.getLogger().warning("[StaffChat] Command " + command.getName() + " unknown!");
            return false;
        }
        return true;
    }
}

