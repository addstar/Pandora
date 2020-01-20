package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AntiSwim implements Module, Listener {

    private MasterPlugin mPlugin;
    private File mFile;
    private FileConfiguration mConfig;
    private HashMap<World, ProtOpts> protworlds = new HashMap<>();
    private boolean Debug = false;

    public enum ProtAction {
        IGNORE,
        CANCEL,
        SPAWN,
        KILL
    }

    public class ProtOpts {
        ProtAction Action = ProtAction.IGNORE;
        String PlayerMessage;
        int WaterScanRadius;
    }

    @Override
    public void onEnable() {
        loadConfig();
    }

    @Override
    public void onDisable() {
        mConfig = null;
        mFile = null;
        protworlds.clear();
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (protworlds.isEmpty() || !protworlds.containsKey(e.getPlayer().getWorld()))
            return;

        // 1: Check player is in water (at their feet)
        Block block = e.getTo().getBlock();
        if (!block.isLiquid()) return;

        // Allow permission bypass
        if (e.getPlayer().hasPermission("pandora.antiswim.bypass"))
            return;

        // Try to find the sea level so we dont have obstacles getting in the way
        int x;
        for (x = 0; x < 15; x++) {
            if (!block.getRelative(BlockFace.UP, x + 1).isLiquid())
                break;
        }

        // Adjust the position if we found a highter water level
        if (x > 0) {
            block = block.getRelative(BlockFace.UP, x);
        }

        // 2: Check the 4 corners around the player for water (at a distance of WaterScanRadius)
        ProtOpts opts = protworlds.get(e.getPlayer().getWorld());

        if (!block.getRelative(BlockFace.NORTH, opts.WaterScanRadius)
                .getRelative(BlockFace.WEST, opts.WaterScanRadius).isLiquid()) return;

        if (!block.getRelative(BlockFace.NORTH, opts.WaterScanRadius)
                .getRelative(BlockFace.EAST, opts.WaterScanRadius).isLiquid()) return;

        if (!block.getRelative(BlockFace.SOUTH, opts.WaterScanRadius)
                .getRelative(BlockFace.WEST, opts.WaterScanRadius).isLiquid()) return;

        if (!block.getRelative(BlockFace.SOUTH, opts.WaterScanRadius)
                .getRelative(BlockFace.EAST, opts.WaterScanRadius).isLiquid()) return;

        if (Debug) mPlugin.getLogger().info("[AntiSwim] Water in all 4 corners around " + e.getPlayer().getName());

        ProtAction action = opts.Action;
        if (action == null) {
            action = ProtAction.IGNORE;
        }

        // Send message to player
        if (!opts.PlayerMessage.isEmpty()) {
            e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', opts.PlayerMessage));
        }

        switch (action) {
            case SPAWN:
                if (Debug) mPlugin.getLogger().info("[AntiSwim] Spawning player");
                e.setCancelled(true);
                final Player ps = e.getPlayer();
                Location pos = Bukkit.getWorlds().get(0).getSpawnLocation();
                if (ps.teleport(pos)) {
                    ps.setVelocity(new Vector(0, 0, 0));
                    ps.setHealth(20.0f);
                    ps.setSaturation(20.0f);

                    // The best way to prevent lingering damage effects from still being applied after the teleport
                    Bukkit.getScheduler().runTask(mPlugin, () -> {
                        ps.setFireTicks(0);
                        ps.setNoDamageTicks(40);
                        ps.setHealth(20.0f);
                        ps.setSaturation(20.0f);
                        ps.setFallDistance(0);
                        ps.setVelocity(new Vector(0, 0, 0));
                    });
                } else {
                    mPlugin.getLogger().warning("[AntiSwim] Spawn teleport failed for " + ps.getName() + "!");
                }

                break;
            case KILL:
                if (Debug) mPlugin.getLogger().warning("[AntiSwim] Killing player " + e.getPlayer().getName());
                e.getPlayer().setHealth(0.0);
                break;
            case CANCEL:
                if (Debug) mPlugin.getLogger().warning("[AntiSwim] Canceling move " + e.getPlayer().getName());
                e.setCancelled(true);
                break;
            case IGNORE:
                if (Debug)
                    mPlugin.getLogger().warning("[AntiSwim] Player " + e.getPlayer().getName() + " is swimming but will be ignored");
                break;
            default:
                break;
        }
    }

    private boolean loadConfig() {
        mFile = new File(mPlugin.getDataFolder(), "AntiSwim.yml");
        mConfig = YamlConfiguration.loadConfiguration(mFile);

        Debug = mConfig.getBoolean("debug", false);

        if (mConfig.getConfigurationSection("worlds") == null)
            return true;

        Set<String> worlds = mConfig.getConfigurationSection("worlds").getKeys(false);
        for (String w : worlds) {
            World world = Bukkit.getWorld(w);
            if (world == null) {
                mPlugin.getLogger().warning("[AntiSwim] Invalid world \"" + w + "\"!");
                continue;
            }

            ConfigurationSection worldSection = mConfig.getConfigurationSection("worlds." + w);

            ProtOpts opts = new ProtOpts();
            opts.PlayerMessage = worldSection.getString("message", "");
            opts.WaterScanRadius = worldSection.getInt("radius", 10);

            // Get world action
            String action = worldSection.getString("action", "SPAWN").toUpperCase();
            try {
                opts.Action = ProtAction.valueOf(action);
            } catch (Exception e) {
                mPlugin.getLogger().warning("[AntiSwim] Invalid action \"" + action + "\" in world \"" + w + "\"");
                continue;
            }
            protworlds.put(world, opts);
        }

        if (Debug) {
            System.out.println("==============================================");
            for (Map.Entry<World, ProtOpts> entry : protworlds.entrySet()) {
                System.out.println("World: " + entry.getKey().getName());
                ProtOpts opts = entry.getValue();
                System.out.println("  action : " + opts.Action);
                System.out.println("  radius : " + opts.WaterScanRadius);
                System.out.println("  message: " + opts.PlayerMessage);
            }
            System.out.println("==============================================");
        }

        return false;
    }
}