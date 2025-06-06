package net.vrcub.play.itembank.file;

import net.vrcub.play.itembank.ItemBank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MenuFile {
    private final ItemBank plugin;
    private ConfigurationSection bankMenuSection;
    private ConfigurationSection storeMenuSection;



    private ConfigurationSection shopMenuSection;
    public MenuFile(ItemBank plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }

        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(guiFile);
        bankMenuSection = yamlConfig.getConfigurationSection("BankGUI");
        storeMenuSection = yamlConfig.getConfigurationSection("StoreGUI");
        shopMenuSection = yamlConfig.getConfigurationSection("ShopGUI");
    }

    public ConfigurationSection getBankMenuSection() {
        return bankMenuSection;
    }
    public ConfigurationSection getShopMenuSection() {
        return shopMenuSection;
    }
    public ConfigurationSection getStoreMenuSection() {
        return storeMenuSection;
    }
}
