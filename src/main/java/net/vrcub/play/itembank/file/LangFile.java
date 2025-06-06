package net.vrcub.play.itembank.file;

import net.vrcub.play.itembank.ItemBank;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class LangFile {
    private final ItemBank plugin;
    public static YamlConfiguration yamlConfiguration;

    public LangFile(ItemBank plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File bankItemFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!bankItemFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        yamlConfiguration = YamlConfiguration.loadConfiguration(bankItemFile);
    }

    public static void sendMessage(CommandSender sender, String node, Object... args) {
        sender.sendMessage(yamlConfiguration.getString("prefix") + String.format(yamlConfiguration.getString(node, "§c未知语言节点: §a§n" + node), args));
    }
    public static void sendMessages(CommandSender sender, String node, Object... args) {
        List<String> list = yamlConfiguration.getStringList(node);
        list.forEach(line-> {
            sender.sendMessage(yamlConfiguration.getString("prefix") + String.format(line, args));
        });

    }
}
