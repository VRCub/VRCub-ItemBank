package net.vrcub.play.itembank.listener;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.file.LangFile;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.gui.type.BankGUI;
import net.vrcub.play.itembank.gui.type.ShopGUI;
import net.vrcub.play.itembank.gui.type.StoreGUI;
import net.vrcub.play.itembank.tools.ColorUtil;
import net.vrcub.play.itembank.tools.Debugger;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BankGUIListener implements Listener {
    private final ItemBank plugin;

    public Map<String, BankItem> getCatcher() {
        return catcher;
    }

    private Map<String, BankItem> catcher;
    public BankGUIListener(ItemBank plugin) {
        this.plugin = plugin;
        this.catcher = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getInventory().getHolder() instanceof BankGUIHolder) {

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            if (((BankGUIHolder) event.getInventory().getHolder()).getGUIType() != GUIType.BANK) {
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
            ItemStack infoItemStack = null;
            int page = 0;

            try {
                // 赋值操作
                rawSlot = event.getRawSlot();
                inventory = event.getInventory();
                clickItemStack = inventory.getItem(rawSlot);
                infoItemStack = inventory.getItem(48);
                page = ((BankGUIHolder) inventory.getHolder()).getPage();
            } catch (ArrayIndexOutOfBoundsException ignored) {
                // 忽略莫名其妙的报错
            }
            if (clickItemStack == null || infoItemStack == null) return;

            if (rawSlot == 45) {
                new BankGUI(player, page - 1);
                return;
            }
            if (rawSlot == 53) {
                // 下一页逻辑
                new BankGUI(player, page + 1);
                return;
            }
            if (rawSlot == 49) {
                new StoreGUI(player);
                return;
            }
            if (rawSlot == 48) {
                new ShopGUI(player,0);
                return;
            }

            if (rawSlot <= 44 && rawSlot >= 0) {
                ClickType clickType = event.getClick();

                String playerName = player.getName();
                PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(playerName);
                String mat = clickItemStack.getType().toString().toLowerCase();
                int haveAmount = playerItem.getItemAmount(mat);
                BankItem bankItem = plugin.getCacheManager().getBankItemCache().get(mat);
                BigDecimal haveScore = playerItem.getScore();
                BigDecimal price = BigDecimal.valueOf(bankItem.getPrice());


                if (clickType.isLeftClick()) {
                    BigDecimal costScore = price.multiply(BigDecimal.valueOf(64)).multiply(BigDecimal.valueOf(plugin.getConfigFile().getFee()).add(BigDecimal.valueOf(1))).setScale(2, RoundingMode.HALF_UP);
                    // 左键点击，取出64个物品
                    if (haveAmount < 64) {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-amount",bankItem.getDisplayName());
                        return;
                    }
                    if (haveScore.compareTo(costScore) < 0) {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-score",haveScore,costScore);
                        return;
                    }

                    BigDecimal finalScore = haveScore.subtract(costScore);
                    ItemStack stack = new ItemStack(clickItemStack.getType(), 64);
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack);

                    if (overflow.isEmpty()) {
                        int finalAmount = haveAmount - 64;
                        playerItem.setItem(mat, finalAmount);
                        playerItem.setScore(finalScore.setScale(2, RoundingMode.HALF_UP));
                        LangFile.sendMessage(player, "bankitem.withdraw.successfully-withdraw",64,bankItem.getDisplayName(),finalScore);

                        Debugger.addDebug(new StringBuilder()
                                .append("取出 ").append(ColorUtil.replaceColor(bankItem.getDisplayName()))
                                .append(" x64 花费: ").append(costScore)
                                .append(" 剩余: ").append(finalScore)
                                .append(" 余量: ").append(finalAmount)
                                .append(" 左键").toString(), playerName);

                        clickItemStack.setAmount(Math.min(Math.max(finalAmount, 1), 64));
                        updateItemMeta(clickItemStack, String.valueOf(finalAmount), "%amount%",false);
                        inventory.setItem(rawSlot, clickItemStack);

                        updateItemMeta(infoItemStack, finalScore.toString(), "%score%",true);
                        inventory.setItem(48, infoItemStack);
                    } else {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-slots",bankItem.getDisplayName());
                    }


                } else if (clickType.isRightClick()) {
                    BigDecimal costScore = price.multiply(BigDecimal.valueOf(320)).multiply(BigDecimal.valueOf(plugin.getConfigFile().getFee()).add(BigDecimal.valueOf(1))).setScale(2, RoundingMode.HALF_UP);
                    // 左键点击，取出64个物品
                    if (haveAmount < 320) {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-amount",bankItem.getDisplayName());
                        return;
                    }
                    if (haveScore.compareTo(costScore) < 0) {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-score",haveScore,costScore);
                        return;
                    }
                    if (getFreeSlots(player.getInventory()) < 4) {
                        LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-slots",bankItem.getDisplayName());
                        return;
                    }

                    int remainingAmount = 320;

                    while (remainingAmount > 0) {
                        //   int giveAmount = 64; // 每次最多给64个
                        ItemStack stack = new ItemStack(clickItemStack.getType(), 64);

                        // 尝试添加到背包
                        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack);

                        if (overflow.isEmpty()) {
                            remainingAmount -= 64; // 减少剩余数量
                        } else {
                            // 背包满了，将剩余物品丢到地上
                            LangFile.sendMessage(player, "bankitem.withdraw.drop-on-the-ground",bankItem.getDisplayName());
                            for (ItemStack item : overflow.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                            break; // 退出循环，不再尝试添加更多物品
                        }
                    }

                    int finalAmount = haveAmount - 320;
                    BigDecimal finalScore = haveScore.subtract(costScore);
                    // 更新积分和银行物品数量
                    playerItem.setScore(finalScore.setScale(2, RoundingMode.HALF_UP));
                    playerItem.setItem(mat, finalAmount);
                    LangFile.sendMessage(player, "bankitem.withdraw.successfully-withdraw",320,bankItem.getDisplayName(),finalScore);
                    Debugger.addDebug(new StringBuilder()
                            .append("取出 ").append(ColorUtil.replaceColor(bankItem.getDisplayName()))
                            .append(" x320 花费: ").append(costScore)
                            .append(" 积分: ").append(finalScore)
                            .append(" 余量: ").append(finalAmount)
                            .append(" 右键").toString(), playerName);
                    // 更新显示信息
                    clickItemStack.setAmount(Math.min(Math.max(finalAmount, 1), 64));
                    updateItemMeta(clickItemStack,  String.valueOf(finalAmount), "%amount%",false);
                    inventory.setItem(rawSlot, clickItemStack);

                    updateItemMeta(infoItemStack, finalScore.toString(), "%score%",true);
                    inventory.setItem(48, infoItemStack);
                } if (clickType == ClickType.DROP) {
                    player.closeInventory();
                    LangFile.sendMessage(player, "bankitem.withdraw.input-start",bankItem.getDisplayName());
                    catcher.put(playerName, bankItem);
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // 检查玩家是否在等待输入
        if (catcher.containsKey(playerName)) {
            // 取消聊天事件，防止其他插件处理
            event.setCancelled(true);
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                catcher.remove(playerName);
                LangFile.sendMessage(player, "bankitem.withdraw.input-cancel");
                return;
            }

            try {
                int number = Integer.parseInt(message);

                BankItem bankItem = catcher.get(playerName);
                PlayerItem playerItem = plugin.getCacheManager().getPlayerItemCache().get(playerName);
                int haveAmount = playerItem.getItemAmount(bankItem.getName());
                BigDecimal haveScore = playerItem.getScore();
                BigDecimal price = BigDecimal.valueOf(bankItem.getPrice());
                BigDecimal costScore = price.multiply(BigDecimal.valueOf(number)).multiply(BigDecimal.valueOf(plugin.getConfigFile().getFee()).add(BigDecimal.valueOf(1))).setScale(2, RoundingMode.HALF_UP);

                if (haveAmount < number) {
                    LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-amount",bankItem.getDisplayName());
                    catcher.remove(playerName);
                    return;
                }
                if (haveScore.compareTo(costScore) < 0) {
                    LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-score",haveScore,costScore);
                    catcher.remove(playerName);
                    return;
                }
                if (getFreeSlots(player.getInventory()) < (number / 64D)) {
                    LangFile.sendMessage(player, "bankitem.withdraw.not-have-enough-slots",bankItem.getDisplayName());
                    return;
                }

                int fullStacks = number / 64;
                int remainingAmount = number % 64;

                // 发放64倍数的物品
                for (int i = 0; i < fullStacks; i++) {
                    ItemStack stack = new ItemStack(bankItem.getMaterial(), 64);
                    player.getInventory().addItem(stack);
                }

                // 发放剩余的物品
                if (remainingAmount > 0) {
                    ItemStack stack = new ItemStack(bankItem.getMaterial(), remainingAmount);
                    player.getInventory().addItem(stack);
                }
                BigDecimal finalScore = haveScore.subtract(costScore);
                playerItem.setScore(finalScore.setScale(2, RoundingMode.HALF_UP));
                playerItem.setItem(bankItem.getName(), haveAmount - number);
                // 移除玩家的等待状态
                catcher.remove(playerName);
                LangFile.sendMessage(player, "bankitem.withdraw.successfully-withdraw",number,bankItem.getDisplayName(), finalScore);
                Debugger.addDebug(new StringBuilder()
                        .append("取出 ").append(ColorUtil.replaceColor(bankItem.getDisplayName()))
                        .append(" x").append(number)
                        .append(" 花费: ").append(costScore)
                        .append(" 积分: ").append(finalScore)
                        .append(" 余量: ").append(haveAmount - number)
                        .append(" Q键").toString(), playerName);

            } catch (NumberFormatException e) {
                LangFile.sendMessage(player, "bankitem.withdraw.input-not-invalid");
            }
        }
    }

    // 辅助方法：计算背包的空格数量
    private int getFreeSlots(Inventory inventory) {
        int freeSlots = 0;
        for (int i = 0; i < 35; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) {
                freeSlots++;
            }
        }
        return freeSlots;
    }
    private void updateItemMeta(ItemStack itemStack, String replacement,String placeholder,boolean isInfoItem) {
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = new ArrayList<>(meta.getLore());
//        List<String> newLore = new ArrayList<>();
//        for (String temp : lore) {
//            newLore.add(temp.replace(placeholder, replacement));
//        }
        int index = isInfoItem ? 1 :5;
        String newLine = lore.get(index).replaceAll("§a.*", "§a"+replacement);
        lore.set(index, newLine);
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

}
