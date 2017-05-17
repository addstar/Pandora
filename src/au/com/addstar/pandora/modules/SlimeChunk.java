package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 17/05/2017.
 */
public class SlimeChunk implements Module, CommandExecutor {

    private MasterPlugin mPlugin;

    @Override
    public void onEnable() {
        mPlugin.getCommand("slimechunk").setExecutor(this);
    }

    @Override
    public void onDisable() {
        mPlugin.getCommand("slimechunk").setExecutor(null);

    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        this.mPlugin =  plugin;

    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            if (commandSender.hasPermission("Pandora.slimechunk.show")) {
                Chunk chunk = ((Player) commandSender).getLocation().getChunk();
                if (chunk.isSlimeChunk()) {
                    commandSender.sendMessage("This chunk is a Slime Chunk");
                } else {
                    commandSender.sendMessage("This chunk is NOT a Slime Chunk");
                }
            }else{
                commandSender.sendMessage("You do not have permission for that command.");
            }
            return true;
        }else{
            return false;
        }
    }
}

