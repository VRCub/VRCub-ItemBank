package net.vrcub.play.itembank.command;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.gui.type.BankGUI;
import net.vrcub.play.itembank.gui.type.ShopGUI;
import net.vrcub.play.itembank.gui.type.StoreGUI;
import net.vrcub.play.itembank.tools.RunnableUtil;
import net.vrcub.play.itembank.warpper.PlayerItem;
import net.vrcub.play.itembank.warpper.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class MainCommand implements CommandExecutor {

    private final ItemBank plugin;

    public MainCommand(ItemBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // 如果命令发送者是玩家
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getBankGUIListener().getCatcher().remove(player.getName());

            if (args.length == 0) {
                new BankGUI(player, 0);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "bank":
                    if (args.length == 1) {
                        new BankGUI(player, 0);
                    }
                    return true;
                case "store":
                    new StoreGUI(player);
                    return true;
                case "migrate":
                    if (player.isOp()) {
                        migrate(player);
                    }
                    return true;
                case "recount":
                    if (player.isOp()) {
                        plugin.getDatabaseManager().getDB().recountScore();
                        plugin.getCacheManager().getPlayerItemCache().clear();
                        plugin.getDatabaseManager().writeOnlinePlayerRewardData();
                    }
                    return true;
                case "shop":
                    new ShopGUI(player, 0);
                    return true;
                case "shopedit":
                    if (player.isOp()) {
                        if (args.length >= 2) {
                            switch (args[1].toLowerCase()) {
                                case "add":
                                    if (args.length >= 4) {

                                        String itemName = args[2];
                                        double price;
                                        try {
                                            price = Double.parseDouble(args[3]);
                                        } catch (Exception e) {
                                            LangFile.sendMessage(player, "command.shopedit.add.parse-double-failed", args[3]);
                                            return false;
                                        }

                                        ItemStack item = player.getInventory().getItemInMainHand();

                                        if (plugin.getCacheManager().getShopItemCache().containsKey(itemName)) {
                                            LangFile.sendMessage(player, "command.shopedit.add.shop-item-exist", itemName);
                                        } else {
                                            saveItemToConfig(itemName, item, price, player);
                                        }
                                        return true;

                                    } else {
                                        LangFile.sendMessage(player, "command.shopedit.add.command-args-missing");
                                        return false;
                                    }
                                case "delete":
                                    if (args.length >= 3) {
                                        String itemName = args[2];
                                        if (plugin.getCacheManager().getShopItemCache().containsKey(itemName)) {
                                            // 从缓存中移除
                                            deleteItemFromConfig(itemName, player);
                                        } else {
                                            LangFile.sendMessage(player, "command.shopedit.delete.shop-item-not-exist", itemName);
                                        }
                                        return true;
                                    } else {
                                        LangFile.sendMessage(player, "command.shopedit.delete.command-args-missing");
                                        return false;
                                    }
                                default:
                                    LangFile.sendMessage(player, "command.shopedit.missing-action");
                                    return true;
                            }
                        } else {
                            LangFile.sendMessage(player, "command.shopedit.missing-action");
                        }
                    } else {
                        LangFile.sendMessage(player, "command.no-permission");
                    }
            }
        }

        // 如果命令发送者是控制台
        if (sender instanceof ConsoleCommandSender) {
            if (args.length == 0) {
                LangFile.sendMessage(sender, "command-args-missing");
                return false;
            }
            if (args.length != 3) {
                LangFile.sendMessage(sender, "command-args-missing");
            }

            if (args[0].equalsIgnoreCase("set")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    try {
                        double setScore = Double.parseDouble(args[2]);
                        PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(target.getName());
                        BigDecimal have = playerItem.getScore();
                        BigDecimal setScoreBigDecimal = new BigDecimal(Double.toString(setScore));

                        playerItem.setScore(setScoreBigDecimal);
                        LangFile.sendMessage(sender, "command.set.successfully-set",target.getName(),have,setScore);
                        return true;
                    } catch (NumberFormatException e) {
                        LangFile.sendMessage(sender, "command.set.double-parse-failed",args[2]);
                        return false;
                    }
                } else {
                    LangFile.sendMessage(sender, "command.set.player-is-offline", args[1]);
                }
                // 假设你有一些需要执行的逻辑
                return true;
            }
            LangFile.sendMessage(sender,"no-in-console");
            return false;
        }

        // 如果命令发送者既不是玩家也不是控制台
        // sender.sendMessage("该命令只能由玩家或控制台执行！");
        return false;
    }

    private void saveItemToConfig(String itemName, ItemStack item, double price, Player player) {
        // 确保配置文件存在
        File configFile = new File(plugin.getDataFolder(), "shopitem.yml");
        if (!configFile.exists()) {
            plugin.saveResource("shopitem.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 创建新的掉落条目
        ItemStack serializedItem = item.clone();
        serializedItem.setItemMeta(item.getItemMeta());

        config.set("shopitem." + itemName + ".item", serializedItem.serialize());
        config.set("shopitem." + itemName + ".price", price);

        // 保存配置文件
        try {
            config.save(configFile);
            ShopGUI.invMap.clear();
            LangFile.sendMessage(player, "command.shopedit.add.successfully-added", itemName, price);
        } catch (Exception e) {
            LangFile.sendMessage(player, "command.shopedit.add.failed-to-add", itemName, e.getMessage());
            e.getStackTrace();
        }
        plugin.getCacheManager().getShopItemCache().put(itemName, new ShopItem(item, itemName, price));
    }

    private void deleteItemFromConfig(String itemName, Player player) {
        // 确保配置文件存在
        File configFile = new File(plugin.getDataFolder(), "shopitem.yml");
        if (!configFile.exists()) {
            // 如果配置文件不存在，直接返回，因为没有可删除的内容
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 检查配置中是否存在该物品的条目
        if (config.contains("shopitem." + itemName)) {
            // 从配置中移除该物品的条目
            config.set("shopitem." + itemName, null);

            // 保存配置文件
            try {
                config.save(configFile);
                ShopGUI.invMap.clear();
                LangFile.sendMessage(player, "command.shopedit.delete.successfully-deleted", itemName);
            } catch (Exception e) {
                LangFile.sendMessage(player, "command.shopedit.delete.failed-to-delete", itemName, e.getMessage());
                e.getStackTrace();
            }
        }
        // 从缓存中移除该物品
        plugin.getCacheManager().getShopItemCache().remove(itemName);
    }


    public static void migrate(Player player) {
        String dataDirectoryPath = ItemBank.getInstance().getDataFolder().getAbsoluteFile() + "/data"; // 数据文件夹路径
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        File dataDirectory = new File(dataDirectoryPath);
        File[] yamlFiles = dataDirectory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml")); // 过滤出 YAML 文件
        Map<String, Integer> validItems = ItemBank.getInstance().getCacheManager().getPlayerItemCache().get(player.getName()).getItems();
        if (yamlFiles != null) {
            for (File yamlFile : yamlFiles) {
                RunnableUtil.runTask(() -> {
                    try (InputStream input = Files.newInputStream(yamlFile.toPath())) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> data = yaml.load(input);

                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                String playerName = entry.getKey();
                                ItemBank.getInstance().getLogger().info(playerName);
                                Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

                                // 提取玩家数据
                                String lastLogin = (String) playerData.get("lastUpdate");
                                double score = ((Number) playerData.get("points")).doubleValue(); // 确保转为 double
                                if (score == 0.0) continue;
                                Map<String, Integer> items = new HashMap<>();

                                // 提取物品数据
                                Map<String, Integer> itemMap = (Map<String, Integer>) playerData.get("ownItem");
                                if (itemMap != null) {
                                    int logAmount = 0;
                                    int woolAmount = 0;
                                    int fishAmount = 0;
                                    int sulphurAmount = 0;
                                    int inkSackAmount = 0;
                                    int netherStalkAmount = 0;
                                    int carrotItemAmount = 0;
                                    int potatoItemAmount = 0;
                                    int rawBeefAmount = 0;
                                    int porkAmount = 0;
                                    int rawChickenAmount = 0;

                                    for (Map.Entry<String, Integer> itemEntry : itemMap.entrySet()) {
                                        String key = itemEntry.getKey();
                                        int value = itemEntry.getValue();

                                        if (key.startsWith("LOG")) {
                                            logAmount = logAmount + value;
                                        }
                                        if (key.startsWith("WOOL")) {
                                            woolAmount = woolAmount + value;
                                        }
                                        if (key.startsWith("RAW_FISH")) {
                                            fishAmount = fishAmount + value;
                                        }
                                        if (key.startsWith("SULPHUR")) {
                                            sulphurAmount = sulphurAmount + value;
                                        }
                                        if (key.startsWith("INK_SACK")) {
                                            inkSackAmount = inkSackAmount + value;
                                        }
                                        if (key.startsWith("NETHER_STALK")) {
                                            netherStalkAmount = netherStalkAmount + value;
                                        }
                                        if (key.startsWith("CARROT_ITEM")) {
                                            carrotItemAmount = carrotItemAmount + value;
                                        }
                                        if (key.startsWith("POTATO_ITEM")) {
                                            potatoItemAmount = potatoItemAmount + value;
                                        }
                                        if (key.startsWith("RAW_BEEF")) {
                                            rawBeefAmount = rawBeefAmount + value;
                                        }
                                        if (key.startsWith("PORK")) {
                                            porkAmount = porkAmount + value;
                                        }
                                        if (key.startsWith("RAW_CHICKEN")) {
                                            rawChickenAmount = rawChickenAmount + value;
                                        }
                                    }

                                    itemMap.put("oak_log", logAmount);
                                    itemMap.put("white_wool", woolAmount);
                                    itemMap.put("cod", fishAmount);
                                    itemMap.put("gunpowder", sulphurAmount);
                                    itemMap.put("ink_sac", inkSackAmount);
                                    itemMap.put("nether_wart", netherStalkAmount);
                                    itemMap.put("carrot", carrotItemAmount);
                                    itemMap.put("potato", potatoItemAmount);
                                    itemMap.put("beef", rawBeefAmount);
                                    itemMap.put("porkchop", porkAmount);
                                    itemMap.put("chicken", rawChickenAmount);
                                    items.putAll(itemMap);
                                }
//                            items.forEach((key,value) -> {
//                                System.out.println(key+value);
//                            });
                                ItemBank.getInstance().getDatabaseManager().getDB().migrate(validItems, playerName, lastLogin, score, items);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            }
        } else {
            LangFile.sendMessage(player, "command.migrate.missing-data-file");
        }
    }

}
