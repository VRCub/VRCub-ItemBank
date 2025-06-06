package net.vrcub.play.itembank.listener;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.tools.RunnableUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ItemBank plugin;

    public PlayerListener(ItemBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        RunnableUtil.runTaskAsynchronously(() -> {
            String playerName = event.getPlayer().getName();
            if (!plugin.getDatabaseManager().getDB().playerExists(playerName)) {
                plugin.getDatabaseManager().getDB().insertPlayerData(playerName);
            }
            plugin.getCacheManager().getPlayerItemCache().put(playerName, plugin.getDatabaseManager().getDB().getPlayerItems(playerName));
        });

    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        RunnableUtil.runTaskAsynchronously(() -> {
            String playerName = event.getPlayer().getName();
            plugin.getDatabaseManager().getDB().savePlayerItems(playerName, plugin.getCacheManager().getPlayerItemCache().get(playerName));
            plugin.getCacheManager().getPlayerItemCache().remove(playerName);
            plugin.getBankGUIListener().getCatcher().remove(playerName);
        });
    }
}
