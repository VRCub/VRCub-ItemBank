package net.vrcub.play.itembank;

import net.vrcub.play.itembank.cache.CacheManager;

import net.vrcub.play.itembank.command.MainCommand;
import net.vrcub.play.itembank.database.DatabaseManager;
import net.vrcub.play.itembank.file.*;
import net.vrcub.play.itembank.hook.PlaceholderHook;
import net.vrcub.play.itembank.listener.BankGUIListener;
import net.vrcub.play.itembank.listener.PlayerListener;
import net.vrcub.play.itembank.listener.ShopGUIListener;
import net.vrcub.play.itembank.listener.StoreGUIListener;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.tools.RunnableUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemBank extends JavaPlugin {

    public static ItemBank getInstance() {
        return instance;
    }
    public static Map<String, Long> clickIntervalMap = new ConcurrentHashMap<>();
    public NamespacedKey getKey() {
        return key;
    }

    private NamespacedKey key;
    public BankItemFile getBankItemFile() {
        return bankItemFile;
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }
    public MenuFile getMenuFile() {
        return menuFile;
    }
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public BankGUIListener getBankGUIListener() {
        return bankGUIListener;
    }

    private static ItemBank instance;
    
    private BankItemFile bankItemFile;

    private ConfigFile configFile;
    private MenuFile menuFile;
    
    private DatabaseManager databaseManager;
    
    private CacheManager cacheManager;
    private ShopItemFile shopItemFile;
    private LangFile langFile;
    private BankGUIListener bankGUIListener;

    @Override
    public void onEnable() {
        RunnableUtil.initScheduler(this);
        key = new NamespacedKey(this, "shopitem_name");
        instance = this;
        new Debugger(this);
        langFile = new LangFile(this);
        cacheManager = new CacheManager(this);

        bankItemFile = new BankItemFile(this);
        shopItemFile = new ShopItemFile(this);
        menuFile = new MenuFile(this);
        configFile = new ConfigFile(this);
        databaseManager = new DatabaseManager(this);


        new PlaceholderHook(this).register();

        this.getCommand("itembank").setExecutor(new MainCommand(this));
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        bankGUIListener = new BankGUIListener(this);
        this.getServer().getPluginManager().registerEvents(bankGUIListener, this);
        this.getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);
        this.getServer().getPluginManager().registerEvents(new StoreGUIListener(this), this);
    }

    @Override
    public void onDisable() {
        cacheManager.getPlayerItemCache().forEach((value,key) -> databaseManager.getDB().savePlayerItems(value, key));
        new PlaceholderHook(this).unregister();
        databaseManager.close();
        cacheManager.clear();
       // RunnableUtil.cancelTask();
    }


}
