package net.vrcub.play.itembank.listener;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.gui.type.BankGUI;
import net.vrcub.play.itembank.gui.type.ShopGUI;
import net.vrcub.play.itembank.tools.ColorUtil;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.warpper.PlayerItem;
import net.vrcub.play.itembank.warpper.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

public class ShopGUIListener implements Listener {
    private final ItemBank plugin;

    public ShopGUIListener(ItemBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getInventory().getHolder() instanceof BankGUIHolder) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            if (((BankGUIHolder) event.getInventory().getHolder()).getGUIType() != GUIType.SHOP) {
                return;
            }
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            if (System.currentTimeMillis() - ItemBank.clickIntervalMap.getOrDefault(player.getName(), 0L) < 500) {
                LangFile.sendMessage(player, "click-to-fast");
                return;
            }
            ItemBank.clickIntervalMap.put(player.getName(), System.currentTimeMillis());
            // 变量声明
            int rawSlot = 0;
            Inventory inventory = null;
            ItemStack clickItemStack = null;
            //  ItemStack infoItemStack = null;
            int page = 0;

            try {
                // 赋值操作
                rawSlot = event.getRawSlot();
                inventory = event.getInventory();
                clickItemStack = inventory.getItem(rawSlot);
                //  infoItemStack = inventory.getItem(48);
                page = ((BankGUIHolder) inventory.getHolder()).getPage();
            } catch (ArrayIndexOutOfBoundsException ignored) {
                // 忽略莫名其妙的报错
            }
            if (clickItemStack == null) return;

            if (rawSlot == 45) {
                new ShopGUI(player, page - 1);
                return;
            }
            if (rawSlot == 53) {
                // 下一页逻辑
                new ShopGUI(player, page + 1);
                return;
            }
            if (rawSlot == 49) {
                new BankGUI(player, 0);
                return;
            }

            if (rawSlot <= 44 && rawSlot >= 0) {

                String playerName = player.getName();
                PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(playerName);
                String itemName = clickItemStack.getItemMeta().getPersistentDataContainer().get(plugin.getKey(), PersistentDataType.STRING);
                ShopItem shopItem = plugin.getCacheManager().getShopItemCache().get(itemName);
                if (shopItem == null) {
                    LangFile.sendMessage(player, "shopitem.buy.unknown-item",itemName);
                    return;
                }

                BigDecimal haveScore = playerItem.getScore();
                BigDecimal costScore = BigDecimal.valueOf(shopItem.getPrice());
                
                if (haveScore.compareTo(costScore) < 0) {
                    LangFile.sendMessage(player, "shopitem.buy.not-have-enough-score",itemName,haveScore,costScore);
                    return;
                }

                BigDecimal finalScore = haveScore.subtract(costScore);
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(shopItem.getItemStack());

                if (overflow.isEmpty()) {
                    playerItem.setScore(finalScore.setScale(2, RoundingMode.HALF_UP));
                    LangFile.sendMessage(player, "shopitem.buy.successfully-brought",costScore,itemName,finalScore);

                    Debugger.addDebug(new StringBuilder()
                            .append("积分购买 ").append(itemName)
                            .append(" x1 花费: ").append(costScore)
                            .append(" 剩余: ").append(finalScore).toString(), playerName);
                } else {
                    LangFile.sendMessage(player, "shopitem.buy.not-have-enough-slots",itemName);
                }
            }
        }
    }
}
