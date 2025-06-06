package net.vrcub.play.itembank.database.mysql;




import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.ConfigFile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnection {

    public volatile static HikariDataSource dataSource;

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
    public static HikariConfig getHikariConfig() {
        // 新建连接池配置
        HikariConfig config = new HikariConfig();
        ConfigFile configFile = ItemBank.getInstance().getConfigFile();
        // 连接字符串参数
        String jdbcUrl = "jdbc:mysql://"
                + configFile.getMySQLHost()
                + ":"
                + configFile.getMySQLPort()
                + "/"
                + configFile.getMySQLDatabase()
                + "?useSSL="
                + configFile.isMySQLUseSSL()
                + "&allowPublicKeyRetrieval=true" // 允许公钥检索以支持旧认证插件
                + "&serverTimezone=UTC"; // 设置时区，以避免与时间相关的问题

        // 设置连接池参数
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configFile.getMySQLUser());
        config.setPassword(configFile.getMySQLPassword());
        config.setMaximumPoolSize(configFile.getMySQLMaximumPoolSize()); // 连接池最大连接数
        config.setConnectionTimeout(configFile.getMySQLConnectionTimeout()); // 连接超时
        config.setIdleTimeout(configFile.getMySQLIdleTimeout()); // 空闲超时
        config.setMaxLifetime(configFile.getMySQLMaxLifetime()); // 最大生命周期

        return config;
    }
}

