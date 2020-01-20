package au.com.addstar.pandora.modules.autosave;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.scheduler.BukkitTask;

public class UserCacheSaveTask implements Runnable {
    private Autosaver mModule;
    private BukkitTask mTask;
    private boolean mIsRunning;

    public UserCacheSaveTask(Autosaver module) {
        mModule = module;
        mIsRunning = false;

        mTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(), this, module.getConfig().userCacheSaveInterval, module.getConfig().userCacheSaveInterval);
    }

    public void stop() {
        mTask.cancel();
    }

    @Override
    public void run() {
        if (mIsRunning)
            return;

        mIsRunning = true;
        Bukkit.getScheduler().runTaskAsynchronously(mModule.getPlugin(), new SavingTask());
    }

    private class SavingTask implements Runnable {
        @Override
        public void run() {
            try {
                ((CraftServer) Bukkit.getServer()).getServer().getUserCache().c(); // Save
            } finally {
                mIsRunning = false;
            }
        }
    }
}
