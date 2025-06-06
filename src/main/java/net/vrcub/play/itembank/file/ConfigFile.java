package net.vrcub.play.itembank.file;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.tools.RunnableUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;


public class ConfigFile {
    private String DataType;

    public String getDataType() {
        return DataType;
    }

    public String getMySQLHost() {
        return MySQLHost;
    }

    public String getMySQLDatabase() {
        return MySQLDatabase;
    }

    public String getMySQLUser() {
        return MySQLUser;
    }

    public String getMySQLPassword() {
        return MySQLPassword;
    }

    public boolean isMySQLUseSSL() {
        return MySQLUseSSL;
    }

    public int getMySQLPort() {
        return MySQLPort;
    }

    public int getMySQLMaximumPoolSize() {
        return MySQLMaximumPoolSize;
    }

    public int getMySQLConnectionTimeout() {
        return MySQLConnectionTimeout;
    }

    public int getMySQLIdleTimeout() {
        return MySQLIdleTimeout;
    }

    public int getMySQLMaxLifetime() {
        return MySQLMaxLifetime;
    }

    public ItemBank getPlugin() {
        return plugin;
    }

    private String MySQLHost, MySQLDatabase, MySQLUser, MySQLPassword;
    private boolean MySQLUseSSL;
    private int MySQLPort, MySQLMaximumPoolSize, MySQLConnectionTimeout, MySQLIdleTimeout, MySQLMaxLifetime;
    private final ItemBank plugin;

    public long getAutoSaveTick() {
        return AutoSaveTick;
    }

    private long AutoSaveTick;

    public ConfigurationSection getRateSection() {
        return rateSection;
    }

    public double getFee() {
        return fee;
    }

    private ConfigurationSection rateSection;
    private double fee;

    public ConfigFile(ItemBank plugin) {
        this.plugin = plugin;
        this.load();
    }
    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 数据库配置
        DataType = config.getString("Database.type", "SQLite");
        AutoSaveTick = config.getLong("Database.auto-save", 6000);
        MySQLHost = config.getString("Database.MySQL.host");
        MySQLPort = config.getInt("Database.MySQL.port");
        MySQLDatabase = config.getString("Database.MySQL.database");
        MySQLUser = config.getString("Database.MySQL.user");
        MySQLPassword = config.getString("Database.MySQL.password");
        MySQLUseSSL = config.getBoolean("Database.MySQL.use-SSL");
        MySQLMaximumPoolSize = config.getInt("Database.MySQL.maximum-pool-size", 5);
        MySQLConnectionTimeout = config.getInt("Database.MySQL.connection-timeout", 30000);
        MySQLIdleTimeout = config.getInt("Database.MySQL.idle-timeout", 600000);
        MySQLMaxLifetime = config.getInt("Database.MySQL.max-lifetime", 1800000);
        rateSection = config.getConfigurationSection("Rate");
        fee = config.getDouble("Fee");
        RunnableUtil.runTaskTimer(() -> plugin.getCacheManager().refreshData(), AutoSaveTick, AutoSaveTick);
    }
}
