package net.vrcub.play.itembank.tools;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Debugger {

    private final ItemBank plugin;
    private static File debugFile;
    private static FileConfiguration debugConfig;
    public Debugger(ItemBank plugin) {
        this.plugin = plugin;
        loadDebugFile();
    }

    public void loadDebugFile() {
        debugFile = new File(plugin.getDataFolder(), "debug.yml");

        if (!debugFile.exists()) {
            plugin.saveResource("debug.yml", false);
        }
        debugConfig = YamlConfiguration.loadConfiguration(debugFile);
    }

    public static void addDebug(String debugMessage, String playerName) {
//        if (debugConfig == null || debugFile == null) {
//            throw new IllegalStateException("DEBUG文件加载失败！");
//        }
//
//        // 获取玩家对应的日志列表
//        List<String> logs = debugConfig.getStringList(playerName);

        // 添加新的日志信息
      //  logs.add("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + debugMessage);

        Bukkit.getConsoleSender().sendMessage(LangFile.yamlConfiguration.getString("prefix") +  "("+playerName+") " + debugMessage);
        // 将日志列表保存回配置中
    //    debugConfig.set(playerName, logs);

     //   RunnableUtil.runTaskAsynchronously(() -> {
//            try {
//                debugConfig.save(debugFile);
//            } catch (IOException e) {
//                e.printStackTrace();
//                ItemBank.getInstance().getLogger().warning("写入DEBUG日志失败！");
//            }
        //});

    }
}
