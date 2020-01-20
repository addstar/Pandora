package au.com.addstar.pandora;

import java.util.logging.Level;

/**
 * Created for the AddstarMC Project. Created by Narimm on 10/01/2019.
 */
public abstract class AbstractModule implements Module {

    protected boolean debug = false;
    private String name = getClass().getName();

    protected void debugLog(String message) {
        if (debug) MasterPlugin.getInstance().getLogger().log(Level.ALL, "[" + name + " DEBUG]" + message);
    }

    public void log(String message) {
        MasterPlugin.getInstance().getLogger().log(Level.ALL, "[" + name + " INFO]" + message);
    }

}
