package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.LookupCallback;
import au.com.addstar.monolith.lookup.PlayerDefinition;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 24/05/2016.
 */
public class BookMonitor implements Module, CommandExecutor, Listener {
    MasterPlugin plugin;
    Map<UUID,List<BookMeta>> map;
    private File bFile;
    private FileConfiguration bConfig;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEditBook(PlayerEditBookEvent event){

        if(!event.getPlayer().hasPermission("pandora.booklogger.bypass")){
            Player player =  event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            BookMeta meta = event.getNewBookMeta();
            List<BookMeta> list = map.get(playerUUID);
            if(list ==null) list = new ArrayList<>();
            list.add(meta);
            map.remove(playerUUID);
            map.put(playerUUID,list);
            String title = meta.getTitle();
            String text = null;
            if (meta.hasPages()) {
                int i=1;
                for (String page : meta.getPages()) {
                    text = text + "Page "+ i +" "+page + "/n";
                    i++;
                }
            }
            Logger log = plugin.getLogger();
            log.info(player.getName()+ " Wrote Book: " + "Title:" + title);
            if(bConfig.getBoolean("offline.savereports",true)) {
                try {
                    saveBook(player, meta);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveBook(Player player, BookMeta meta) throws IOException{
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter out = null;
        try{
        File parent = new File(plugin.getDataFolder(), "bookreports");
        parent.mkdirs();
        File dest = new File(parent, player.getUniqueId() + ".txt");
        fw = new FileWriter(dest, true);
        bw = new BufferedWriter(fw);
        out = new PrintWriter(bw);
        out.println("-----------------------------------------");
        out.println("  Book report: " + meta.getTitle() + " Author:" + meta.getAuthor());
        out.println("  Completed " + DateFormat.getDateTimeInstance().format(new Date()));
        int i =1;
        for (String page:meta.getPages()) {
            out.println("Page:" +i);
            out.println(page);
        }
        out.flush();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private boolean loadConfig() {
        // Load the config
        try
        {
            bFile = new File(plugin.getDataFolder(), "BookMonitor.yml");
            if (!bFile.exists())
                plugin.saveResource("BookMonitor.yml", false);

            bConfig = YamlConfiguration.loadConfiguration(bFile);
            if (bFile.exists())
                bConfig.load(bFile);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    @Override
    public void onEnable() {
        loadConfig();
        int count = bConfig.getInt("booklogcount",10);
        map = createLRUMap(count);
    }

    @Override
    public void onDisable() {

        bConfig = null;
        bFile = null;
        map.clear();
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        this.plugin =  plugin;
        plugin.getCommand("bookreport").setExecutor(this);

    }

    public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
        return new LinkedHashMap<K, V>(maxEntries*10/7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender.hasPermission("Pandora.booklogger.readbooks")){
            if(args.length > 0){
                Player target =  Bukkit.getPlayer(args[0]);
                int report = 0;
                if(args.length > 1){
                  report =  Integer.parseInt(args[1]);
                }
                if (target == null){
                    BookMonitorCallback callback =  new BookMonitorCallback(sender,report);
                    Lookup.lookupPlayerName(args[0],callback);
                    return true;
                }else{
                    UUID targetUUID =  target.getUniqueId();
                    reportresult(sender,targetUUID,report);
                    return true;
                }

            }else{
                sender.sendMessage("Usage: /bookreport player integer");
                sender.sendMessage("The integer will refer to the number of the book that is stored that you want to read or view");
                return true;
            }

        }else{
            sender.sendMessage(ChatColor.RED + "You have no permission for that command");
        }

        return false;
    }

    private void reportresult(CommandSender sender, UUID uuid, int report){
        List<BookMeta> resultList =  map.get(uuid);
        if(resultList == null){
            if(bConfig.getBoolean("offline.savereports")){
                        offlinereporter(sender,uuid);
            }else{
                sender.sendMessage(ChatColor.RED + "No report available for that player.");
            }
        }else{
            if(report>0){
                int index = report-1;
                BookMeta meta = resultList.get(index);
                sender.sendMessage("Book report: " + meta.getTitle() + " Author:" + meta.getAuthor());
                sender.sendMessage("     Page Count: " + meta.getPageCount());
                int i = 1;
                for (String page:meta.getPages()) {
                    sender.sendMessage("  Page:"+i);
                    sender.sendMessage("    " + page);
                    i++;
                }
            }
        }
    }

    private void offlinereporter(CommandSender sender, UUID uuid){
        File parent = new File(plugin.getDataFolder(), "bookreports");
        File target = new File(parent, uuid + ".txt");
        if(target.isFile()){
            try (BufferedReader br = new BufferedReader(new FileReader(target))) {
                String line;
                sender.sendMessage("Reading books from " + target.getName());
                while ((line = br.readLine()) != null) {
                    sender.sendMessage(line);
                }
            }catch (IOException e){
                e.printStackTrace();
            }

        }else{
            sender.sendMessage("No Book reports found for player either in memory or in the filesystem");
        }
    }

    private class BookMonitorCallback implements LookupCallback{
        private CommandSender sender;
        private int report;

        private BookMonitorCallback(CommandSender sender, int reportNum){
            super();
            this.sender = sender;
            this.report = reportNum;
        }
        @Override
        public void onResult(boolean success, Object value, Throwable error) {
            PlayerDefinition result =  (PlayerDefinition)value;
            UUID targetUUID =  result.getUniqueId();
            reportresult(sender, targetUUID,report);
        }
    }
}
