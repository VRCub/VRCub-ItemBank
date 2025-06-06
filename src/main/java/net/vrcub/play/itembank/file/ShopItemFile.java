package net.vrcub.play.itembank.file;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.warpper.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class ShopItemFile {

    private final ItemBank plugin;


    public ShopItemFile(ItemBank plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File shopitemFile = new File(plugin.getDataFolder(), "shopitem.yml");
        if (!shopitemFile.exists()) {
            plugin.saveResource("shopitem.yml", false);
        }

        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(shopitemFile);

        // 遍历配置文件中的所有怪物掉落
        for (String itemName : yamlConfig.getConfigurationSection("shopitem").getKeys(false)) {
            ConfigurationSection dropSection = yamlConfig.getConfigurationSection("shopitem." + itemName);
            double price = dropSection.getDouble("price", 100000000D);

            // 解析 item 字段为 ItemStack
            ItemStack item = ItemStack.deserialize(dropSection.getConfigurationSection("item").getValues(false));

            plugin.getCacheManager().getShopItemCache().put(itemName, new ShopItem(item, itemName, price));

        }
        LangFile.sendMessage(Bukkit.getConsoleSender(), "shopitem.load.successfully-loaded",plugin.getCacheManager().getShopItemCache().size());
    }


}
