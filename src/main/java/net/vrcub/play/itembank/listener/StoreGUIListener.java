package net.vrcub.play.itembank.listener;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.gui.type.BankGUI;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StoreGUIListener implements Listener {
    private final ItemBank plugin;
    private final int[] stableSlot = {45, 46, 47, 48, 49, 50, 51, 52, 53};

    public StoreGUIListener(ItemBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getInventory().getHolder() instanceof BankGUIHolder) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            if (((BankGUIHolder) event.getInventory().getHolder()).getGUIType() != GUIType.STORE) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            int rawSlot = event.getRawSlot();

            if (Arrays.binarySearch(stableSlot, rawSlot) >= 0) {
                event.setCancelled(true);
                if (System.currentTimeMillis() - ItemBank.clickIntervalMap.getOrDefault(player.getName(), 0L) < 500) {
                    LangFile.sendMessage(player, "click-to-fast");
                    return;
                }
                ItemBank.clickIntervalMap.put(player.getName(), System.currentTimeMillis());
                if (rawSlot == 49 && event.getClick().isLeftClick()) {
                    Inventory inventory = event.getInventory();
                    String playerName = player.getName();
                    PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(playerName);
                    Set<String> allowMats = playerItem.getItems().keySet();
                    double temp = 0.0D;

                    StringBuilder debugBuilder = new StringBuilder();
                    int totalAmount = 0;
                    Map<String, Integer> itemCounts = new HashMap<>();

                    for (ItemStack itemStack : inventory) {
                        if (itemStack != null) {
                            String mat = itemStack.getType().toString().toLowerCase();
                            if (allowMats.contains(mat)) {
                                if (itemStack.hasItemMeta()) {
                                    continue;
                                }
                                double price = plugin.getCacheManager().getBankItemCache().get(mat).getPrice();
                                int amount = itemStack.getAmount();
                                totalAmount += amount;
                                temp = temp + price * amount;
                                playerItem.addItemAmount(mat, amount);
                                itemStack.setAmount(0);

                                // 记录每个物品的数量
                                itemCounts.put(mat, itemCounts.getOrDefault(mat, 0) + amount);
                            }
                        }
                    }

                    // 构建调试信息

                    if (temp != 0.0D) {
                        playerItem.setScore(playerItem.getScore().add(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP)));
                        LangFile.sendMessage(player, "bankitem.deposit.successfully-deposit",Math.round(temp * 100.0) / 100.0);
                        debugBuilder.append("本次获得积分: ").append(Math.round(temp * 100.0) / 100.0).append(" 存入 ").append(totalAmount).append(" 个物品: ");
                        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                            debugBuilder.append(entry.getKey()).append(" x").append(entry.getValue()).append(",");
                        }

                        Debugger.addDebug(debugBuilder.toString(), player.getName());
                    } else {
                        LangFile.sendMessage(player, "bankitem.deposit.deposit-not-invalid");
                    }
                } else if (rawSlot == 49 && event.getClick().isRightClick()) {
                    new BankGUI(player, 0);
                } else if (event.getClick() == ClickType.DROP) {
                    PlayerInventory playerInventory = player.getInventory();
                    String playerName = player.getName();
                    PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(playerName);
                    Set<String> allowMats = playerItem.getItems().keySet();
                    double temp = 0.0D;

                    StringBuilder debugBuilder = new StringBuilder();
                    int totalAmount = 0;
                    Map<String, Integer> itemCounts = new HashMap<>();

                    for (ItemStack itemStack : playerInventory) {
                        if (itemStack != null) {
                            String mat = itemStack.getType().toString().toLowerCase();
                            if (allowMats.contains(mat)) {
                                if (itemStack.hasItemMeta()) {
                                    continue;
                                }
                                BankItem bankItem = plugin.getCacheManager().getBankItemCache().get(mat);
                                double price = bankItem.getPrice();

                                int amount = itemStack.getAmount();
                                totalAmount += amount;
                                temp = temp + price * amount;
                                playerItem.addItemAmount(mat, amount);
                                itemStack.setAmount(0);

                                // 记录每个物品的数量
                                itemCounts.put(bankItem.getDisplayName(), itemCounts.getOrDefault(mat, 0) + amount);
                            }
                        }
                    }


                    if (temp != 0.0D) {
                        playerItem.setScore(playerItem.getScore().add(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP)));
                        LangFile.sendMessage(player, "bankitem.deposit.successfully-deposit",Math.round(temp * 100.0) / 100.0);
                        debugBuilder.append("本次获得积分: ").append(Math.round(temp * 100.0) / 100.0).append(" 存入 ").append(totalAmount).append(" 个物品: ");
                        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                            debugBuilder.append(entry.getKey()).append(" x").append(entry.getValue()).append(",");
                        }

                        Debugger.addDebug(debugBuilder.toString(), player.getName());
                    } else {
                        LangFile.sendMessage(player, "bankitem.deposit.deposit-not-invalid");
                    }
                }
            }
        }
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BankGUIHolder) {
            if (((BankGUIHolder) event.getInventory().getHolder()).getGUIType() != GUIType.STORE) {
                return;
            }

            Player player = (Player) event.getPlayer();

            // 遍历 0-44 格的物品
            for (int i = 0; i < 45; i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    // 尝试将物品放回玩家背包
                    HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(item);

                    // 如果背包满了，将剩余物品掉在地上
                    if (!remainingItems.isEmpty()) {
                        for (ItemStack remainingItem : remainingItems.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), remainingItem);
                        }
                    }
                }
            }
        }
    }
}
