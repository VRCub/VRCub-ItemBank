package net.vrcub.play.itembank.database.mysql;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MySQLManager extends DatabaseManager.DB {
    public MySQLManager(ItemBank plugin) {
        super(plugin);
    }
    private Set<String> cachedColumns = null;
    public void createTable() {
        String playerDataTable = "CREATE TABLE IF NOT EXISTS itembank_data (" +
                "player VARCHAR(255)," +
                "lastLogin TEXT," +
                "score DOUBLE" +
                ")";
        executeUpdate(playerDataTable);

        for (String mat : plugin.getCacheManager().getBankItemCache().keySet()) {
            // 检查是否在 itembank_data 表中存在列名为 mat
            if (!columnExists(mat.toLowerCase())) {
                // 创建新列并初始化为 0
                String addColumnSQL = String.format("ALTER TABLE itembank_data ADD COLUMN %s INT DEFAULT 0", mat);
                executeUpdate(addColumnSQL);
            }
        }

    }



    private void cacheColumns() {
        if (cachedColumns == null) {
            cachedColumns = new HashSet<>();
            try (Connection connection = MySQLConnection.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet resultSet = metaData.getColumns(null, null, "itembank_data", null)) {
                    while (resultSet.next()) {
                        cachedColumns.add(resultSet.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean columnExists(String columnName) {
        cacheColumns(); // 确保列信息已经被缓存
        return cachedColumns.contains(columnName.toLowerCase());
    }
    public PlayerItem getPlayerItems(String playerName) {
        PlayerItem playerItem = new PlayerItem();
        String sql = "SELECT * FROM itembank_data WHERE player = ?";

        try (Connection connection = MySQLConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            ResultSet resultSet = stmt.executeQuery();
            LinkedHashMap<String, BankItem> bankItemList = plugin.getCacheManager().getBankItemCache();

            if (resultSet.next()) {
                playerItem.setScore(resultSet.getBigDecimal("score"));
                playerItem.setLastLogin(resultSet.getString("lastLogin"));
                for (String mat : bankItemList.keySet()) {
                    int amount = resultSet.getInt(mat);
                    playerItem.setItem(mat, amount);
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
        String sql = "SELECT player FROM itembank_data WHERE player = ?";
        try (Connection connection = MySQLConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            ResultSet resultSet = stmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertPlayerData(String playerName) {
        String sql = "INSERT INTO itembank_data (player, lastLogin, score) VALUES (?, ?, ?);";

        try (Connection connection = MySQLConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.setString(2, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            stmt.setDouble(3, 0.0);
            stmt.executeUpdate();
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
            String sql = "UPDATE itembank_data SET lastLogin = ?, score = ?, " + updateSet.toString() + " WHERE player = ?";
            try (Connection connection = MySQLConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                stmt.setBigDecimal(2, playerItem.getScore());
            //    Debugger.addDebug(new StringBuilder().append("自动保存数据")
              //          .append(" 保存的积分为: ").append(playerItem.getScore()).toString(), playerName);

                int index = 3;
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    stmt.setInt(index, entry.getValue());
                    index++;
                }

                stmt.setString(index, playerName); // 设置 WHERE 子句的玩家名
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            // 玩家不存在，执行插入操作
            String sql = "INSERT INTO itembank_data (" + insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")";
            try (Connection connection = MySQLConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        try (Connection connection = MySQLConnection.getConnection();
             Statement statement = connection.prepareStatement(sql)) {
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

        //  System.out.println("INSERT INTO itembank_data (" + "player, lastLogin, score,"+ insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")");
//        System.out.println("================================");
//        validPlayerItems.forEach((key,value) -> {
//            System.out.println(key+value);
//        });
        if (!validPlayerItems.isEmpty()) {
            String sql = "INSERT INTO itembank_data (" + "player, lastLogin, score" + insertColumns.toString() + ") VALUES (" + insertPlaceholders.toString() + ")";
            try (Connection connection = MySQLConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                // 设置玩家信息
                stmt.setString(1, playerName);
                stmt.setString(2, lastLogin);
                stmt.setDouble(3, score);

                // 设置物品信息
                int index = 3; // 从第4列开始是物品信息
                String[] colum = insertColumns.toString().split(", ");
                for (String co : colum) {

                    stmt.setInt(index, validPlayerItems.getOrDefault(co, 0));
                    //     System.out.println(co + validPlayerItems.getOrDefault(co, 0));
                    index++;
                }
                // 执行插入操作
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public List<String> getTopTenPlayer() {
        String sql = "SELECT player, score FROM itembank_data ORDER BY score DESC LIMIT 10";
        List<String> result = new ArrayList<>();

        try (Connection connection = MySQLConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet resultSet = stmt.executeQuery();

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
        try (Connection connection = MySQLConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
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
        StringBuilder sql = new StringBuilder("UPDATE itembank_data SET score = ");
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
        if (MySQLConnection.dataSource != null) {
            MySQLConnection.close();
        }
    }

}
