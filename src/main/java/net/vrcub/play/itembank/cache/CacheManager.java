package net.vrcub.play.itembank.cache;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.tools.RunnableUtil;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;
import net.vrcub.play.itembank.warpper.ShopItem;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private final ItemBank plugin;

    public LinkedHashMap<String, BankItem> getBankItemCache() {
        return bankItemCache;
    }

    public ConcurrentHashMap<String, PlayerItem> getPlayerItemCache() {
        return playerItemCache;
    }

    public LinkedHashMap<String, ShopItem> getShopItemCache() {
        return shopItemCache;
    }


    private final LinkedHashMap<String, BankItem> bankItemCache;
    private final ConcurrentHashMap<String, PlayerItem> playerItemCache;
    private final LinkedHashMap<String, ShopItem> shopItemCache;
    private List<String> rankCache;
    private Long expiration;

    public CacheManager(ItemBank plugin) {
        this.plugin = plugin;
        shopItemCache = new LinkedHashMap<>();
        bankItemCache = new LinkedHashMap<>();
        playerItemCache = new ConcurrentHashMap<>();
        rankCache = new ArrayList<>();

    }

    public void clear() {
        bankItemCache.clear();
        playerItemCache.clear();
    }

    public void refreshData() {
        RunnableUtil.runTaskAsynchronously(() -> {
            long startTime = System.currentTimeMillis();
            try {
                for (Map.Entry<String, PlayerItem> entry : playerItemCache.entrySet()) {
                    plugin.getDatabaseManager().getDB().savePlayerItems(entry.getKey(), entry.getValue());
                }
            } finally {
                LangFile.sendMessage(Bukkit.getConsoleSender(), "auto-save", playerItemCache.size(), (System.currentTimeMillis() - startTime) / 1000D);
                Debugger.addDebug("自动保存" + playerItemCache.size() + "名在线玩家数据完成 耗时: " + (System.currentTimeMillis() - startTime) / 1000D + "s", "CONSOLE");
            }
        });

    }

    public void putRankCache() {
        RunnableUtil.runTaskAsynchronously(() -> {
            rankCache = ItemBank.getInstance().getDatabaseManager().getDB().getTopTenPlayer();
            expiration = System.currentTimeMillis() + 10 * 1000L;
        });
    }

    public List<String> getRankCache() {
        if (rankCache == null || expiration == null || System.currentTimeMillis() >= expiration) {
            putRankCache();
        }
        return rankCache;
    }


}
