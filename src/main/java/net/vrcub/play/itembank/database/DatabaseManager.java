package net.vrcub.play.itembank.database;



import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.database.mysql.MySQLConnection;
import net.vrcub.play.itembank.database.mysql.MySQLManager;
import net.vrcub.play.itembank.database.sqlite.SQLiteManager;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.tools.RunnableUtil;
import net.vrcub.play.itembank.warpper.PlayerItem;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DatabaseManager {
    private final ItemBank plugin;

    public DatabaseManager(ItemBank plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + "/database.db";
        this.databaseType = DatabaseType.valueOf(plugin.getConfigFile().getDataType().toUpperCase());
        this.load();
    }

    private DatabaseType databaseType;
    private volatile DB db;
    public String dbPath;

    private DB initialize(DatabaseType type) {

        Map<DatabaseType, Supplier<DB>> dbMap = new EnumMap<>(DatabaseType.class);
        dbMap.put(DatabaseType.MYSQL, () -> new MySQLManager(plugin));
        dbMap.put(DatabaseType.SQLITE, () -> new SQLiteManager(plugin));

        Supplier<DB> dbSupplier = dbMap.get(type);
        if (dbSupplier != null) {
            db = dbSupplier.get();
        } else {
            // 记录警告日志并禁用插件
            LangFile.sendMessage(Bukkit.getConsoleSender(), "database.unknown-database-type",plugin.getConfigFile().getDataType());
            plugin.getPluginLoader().disablePlugin(plugin);
        }
        return db;
    }

    private void load() {
        LangFile.sendMessage(Bukkit.getConsoleSender(), "database.database-selected-type",plugin.getConfigFile().getDataType());

        db = initialize(databaseType);

        try {
            switch (databaseType) {
                case MYSQL:
                    LangFile.sendMessage(Bukkit.getConsoleSender(), "database.initial-mysql");
                    MySQLConnection.dataSource = new HikariDataSource(MySQLConnection.getHikariConfig());
                    break;
                case SQLITE:
                    File dataFolder = plugin.getDataFolder();
                    if (!dataFolder.exists()) dataFolder.mkdirs();
                    if (!new File(dbPath).exists()) {
                        LangFile.sendMessage(Bukkit.getConsoleSender(), "database.missing-sqlite-file");
                    }
                    break;
            }
            db.createTable();
            LangFile.sendMessage(Bukkit.getConsoleSender(), "database.successfully-loaded");
        } catch (Exception e) {
            LangFile.sendMessage(Bukkit.getConsoleSender(), "database.exception-in-load-database",e.getMessage());
            e.printStackTrace();
            plugin.getPluginLoader().disablePlugin(plugin);
        }
        writeOnlinePlayerRewardData();
    }



    public void writeOnlinePlayerRewardData() {
        RunnableUtil.runTaskAsynchronously(() -> {
            // 适用于热加载插件时。写入数据，避免出现报错
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (!db.playerExists(playerName)) {
                    db.insertPlayerData(playerName);
                }
                plugin.getCacheManager().getPlayerItemCache().put(playerName, db.getPlayerItems(playerName));
            }
            LangFile.sendMessage(Bukkit.getConsoleSender(), "use-plugman-to-load-online-player-data",plugin.getCacheManager().getPlayerItemCache().size());
        });

    }

    public void close() {
        // 关闭数据库链接
        db.close();
        LangFile.sendMessage(Bukkit.getConsoleSender(), "database.close-database-connection",databaseType);
    }

    public DB getDB() {
        return db;
    }

    public abstract static class DB {

        public ItemBank plugin;

        public DB(ItemBank plugin) {
            this.plugin = plugin;
        }

        public abstract void createTable();

        public abstract PlayerItem getPlayerItems(String playerName);

        public abstract void insertPlayerData(String playerName);

        public abstract List<String> getTopTenPlayer();

        public abstract void savePlayerItems(String playerName, PlayerItem playerItem);

        public abstract void migrate(Map<String, Integer> validItems, String playerName, String lastLogin, double score, Map<String, Integer> items);

        public abstract void recountScore();

        public abstract void close();

        public abstract boolean playerExists(String playerName);
    }
}