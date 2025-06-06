package net.vrcub.play.itembank.file;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.tools.ColorUtil;
import net.vrcub.play.itembank.warpper.BankItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class BankItemFile {

    private final ItemBank plugin;


    public BankItemFile(ItemBank plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File bankItemFile = new File(plugin.getDataFolder(), "bankitem.yml");
        if (!bankItemFile.exists()) {
            plugin.saveResource("bankitem.yml", false);
        }

        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(bankItemFile);

        loadBankItems(yamlConfig.getStringList("Items"));
    }

    private void loadBankItems(List<String> itemList) {

        for (String itemString : itemList) {
            String[] parts = itemString.split(";");
            if (parts.length == 3) {
                String materialName = parts[0];
                double price = Double.parseDouble(parts[1]);
                String displayName = ColorUtil.colorize(parts[2]);
                try {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        String mat = material.getKey().toString().split("minecraft:")[1];
                        BankItem bankItem = new BankItem(material, mat, price, displayName);
                        plugin.getCacheManager().getBankItemCache().put(mat, bankItem);
                    } else {
                        LangFile.sendMessage(Bukkit.getConsoleSender(), "bankitem.load.unknown-material",materialName);
                    }
                } catch (Exception e) {
                    LangFile.sendMessage(Bukkit.getConsoleSender(), "bankitem.load.unknown-material",materialName);
                }
            } else {
                LangFile.sendMessage(Bukkit.getConsoleSender(), "bankitem.load.unknown-format",itemString);
            }
        }
    }

}