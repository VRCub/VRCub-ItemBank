package net.vrcub.play.itembank.database.sqlite;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.database.DatabaseManager;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.tools.TimeUtil;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SQLiteManager extends DatabaseManager.DB {
    public static Connection connection;

    public SQLiteManager(ItemBank plugin) {
        super(plugin);
    }

    public void createTable() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        if (!new File(plugin.getDataFolder().getAbsoluteFile() + "/database.db").exists()) {
            //      plugin.logger(LogType.DATABASE, "§e数据库文件不存在，重建数据库连接");
        }
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsoluteFile() + "/database.db";
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String playerDataTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                "player TEXT," +
                "lastLogin TEXT," +
                "score DOUBLE" +
                ")";
        executeUpdate(playerDataTable);

        for (String mat : plugin.getCacheManager().getBankItemCache().keySet()) {

            // 检查是否在 player_data 表中存在列名为 mat
            if (!columnExists(mat.toLowerCase())) {
                // 创建新列并初始化为 0
                String addColumnSQL = String.format("ALTER TABLE player_data ADD COLUMN %s INT DEFAULT 0", mat);
                executeUpdate(addColumnSQL);
            }
        }
    }

    private boolean columnExists(String columnName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, "player_data", columnName);
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public PlayerItem getPlayerItems(String playerName) {
        PlayerItem playerItem = new PlayerItem();
        String query = "SELECT * FROM player_data WHERE player = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            LinkedHashMap<String, BankItem> bankItemList = plugin.getCacheManager().getBankItemCache();

            if (resultSet.next()) {
                playerItem.setScore(resultSet.getBigDecimal("score"));
                playerItem.setLastLogin(resultSet.getString("lastLogin"));
                for (String mat : bankItemList.keySet()) {
                    int amount = resultSet.getInt(mat);
                    playerItem.setItem(mat, amount);
                    //  System.out.println(mat + amount);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Debugger.addDebug(new StringBuilder()
                .append("登录加载数据")
                .append(" 获取的积分为: ").append(playerItem.getScore()).toString(), playerName);

        // 发放物资银行积分利息
        if (!TimeUtil.isToday(playerItem.getLastLogin())) {
            ConfigurationSection rateSection = plugin.getConfigFile().getRateSection();
            BigDecimal start = playerItem.getScore();

            // 根据权限获取对应的利率
            double interestRate = getInterestRate(rateSection, playerName);

            // 计算新的积分：start + start * rate
            BigDecimal rate = BigDecimal.valueOf(interestRate);
            BigDecimal end = start.multiply(rate).add(start).setScale(2, RoundingMode.HALF_UP);

            // 更新玩家的积分
            playerItem.setScore(end);

            // 获取玩家对象
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                // 发送消息给玩家
                LangFile.sendMessages(player,"login-message", start,end.subtract(start));
            }
        }

        return playerItem;
    }
    private  double getInterestRate(ConfigurationSection rateSection, String playerName) {
        // 假设这里是获取玩家权限的逻辑
        String playerPermission = getPlayerPermission(playerName,rateSection); // 获取玩家的权限

        // 根据权限获取利率
        if (rateSection.contains(playerPermission)) {
            return rateSection.getDouble(playerPermission);
        }

        // 默认利率
        return rateSection.getDouble("default");
    }
    public static String getPlayerPermission(String playerName,ConfigurationSection rateSection) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return "default"; // 如果玩家不在线，默认返回 default
        }

        // 遍历玩家的权限，找到符合条件的最高级别权限
        for (String permission : rateSection.getKeys(false)) {
            if (player.hasPermission(permission)) {
                return permission.replace("itembank.rate.", ""); // 返回权限等级
            }
        }

        // 如果玩家没有任何匹配的权限，默认返回 default
        return "default";
    }
    public boolean playerExists(String playerName) {
        String sql = "SELECT player FROM player_data WHERE player = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertPlayerData(String playerName) {
        String sql = "INSERT INTO player_data (player, lastLogin, score) VALUES (?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            statement.setDouble(3, 0.0);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerItems(String playerName, PlayerItem playerItem) {
        // 获取所有物品字段
        Map<String, Integer> items = playerItem.getItems();
        String[] itemFields = items.keySet().toArray(new String[0]);

        // SQL 插入和更新语句
        StringBuilder insertColumns = new StringBuilder("player, lastLogin, score");
        StringBuilder insertPlaceholders = new StringBuilder("?, ?, ?");
        StringBuilder updateSet = new StringBuilder();

        for (String field : itemFields) {
            insertColumns.append(", ").append(field);
            insertPlaceholders.append(", ?");
            updateSet.append(field).append(" = ?, ");
        }

        // 去掉最后一个多余的逗号和空格
        if (updateSet.length() > 0) {
            updateSet.setLength(updateSet.length() - 2);
        }

        // 检查玩家是否存在
        if (playerExists(playerName)) {
            // 玩家已存在，执行更新操作
            String updateQuery = "UPDATE player_data SET lastLogin = ?, score = ?, " + updateSet.toString() + " WHERE player = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setString(1, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                stmt.setBigDecimal(2, playerItem.getScore());

                int index = 3;
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    stmt.setInt(index, entry.getValue());
                    index++;
                }

                stmt.setString(index, playerName); // 设置 WHERE 子句的玩家名
              //  plugin.getLogger().info(stmt.toString());
                stmt.executeUpdate();
            //    Debugger.addDebug(new StringBuilder().append("自动保存数据")
               //         .append(" 保存的积分为: ").append(playerItem.getScore()).toString(), playerName);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            // 玩家不存在，执行插入操作
            String insertQuery = "INSERT INTO player_data (" + insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")";
            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setString(1, playerName);
                stmt.setString(2, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                stmt.setDouble(3, 0.0); // 假设 score 为 0

                int index = 4;
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    stmt.setInt(index, entry.getValue());
                    index++;
                }

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private void executeUpdate(String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void migrate(Map<String, Integer> validItems, String playerName, String lastLogin, double score, Map<String, Integer> items) {


        // 获取当前版本的有效物品
        Set<String> validItemNames = validItems.keySet();

        // 动态构造SQL语句，只包含有效物品
        StringBuilder insertColumns = new StringBuilder();
        StringBuilder insertPlaceholders = new StringBuilder("?, ?, ?");
        Map<String, Integer> validPlayerItems = new HashMap<>();

        for (String itemName : items.keySet()) {
            String name = itemName.toLowerCase().replaceAll("[^a-z_]", "");
            //    System.out.println(name);
            if (validItemNames.contains(name)) {
                validPlayerItems.put(name, items.get(itemName));
                insertColumns.append(", ").append(name);
                insertPlaceholders.append(", ?");
            }

        }

        //  System.out.println("INSERT INTO player_data (" + "player, lastLogin, score,"+ insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")");
//        System.out.println("================================");
//        validPlayerItems.forEach((key,value) -> {
//            System.out.println(key+value);
//        });
        if (!validPlayerItems.isEmpty()) {
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO player_data (" + "player, lastLogin, score" + insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")")) {

                // 设置玩家信息
                pstmt.setString(1, playerName);
                pstmt.setString(2, lastLogin);
                pstmt.setDouble(3, score);

                // 设置物品信息
                int index = 3; // 从第4列开始是物品信息
                String[] colum = insertColumns.toString().split(", ");
                for (String co : colum) {

                    pstmt.setInt(index, validPlayerItems.getOrDefault(co, 0));
                    //     System.out.println(co + validPlayerItems.getOrDefault(co, 0));
                    index++;
                }
                // 执行插入操作
                pstmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public List<String> getTopTenPlayer() {
        String sql = "SELECT player, score FROM player_data ORDER BY score DESC LIMIT 10";
        List<String> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            int rank = 1;
            while (resultSet.next()) {
                String playerName = resultSet.getString("player");
                int score = resultSet.getInt("score");
                String format = " §7No." + rank + " §e" + playerName + " " + " §7积分: §a" + score;
                result.add(format);
                rank++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void recountScore() {
        Map<String, Double> itemPrices = getItemPrices();
        String sql = buildUpdateQuery(itemPrices);
        System.out.println(sql);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

    private static Map<String, Double> getItemPrices() {
        // 假设我们从 ItemBankReloaded 获取物品价格
        Collection<BankItem> bankItems = ItemBank.getInstance().getCacheManager().getBankItemCache().values();
        Map<String, Double> itemPrices = new HashMap<>();
        for (BankItem item : bankItems) {
            itemPrices.put(item.getName(), item.getPrice());
        }
        return itemPrices;
    }

    private static String buildUpdateQuery(Map<String, Double> itemPrices) {
        StringBuilder sql = new StringBuilder("UPDATE player_data SET score = ");
        boolean first = true;
        for (Map.Entry<String, Double> entry : itemPrices.entrySet()) {
            if (!first) {
                sql.append(" + ");
            }
            sql.append("COALESCE(").append(entry.getKey()).append(", 0) * ").append(entry.getValue());
            first = false;
        }
        return sql.toString();
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}