package net.vrcub.play.itembank.gui.type;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.gui.builder.ItemBuilder;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.hook.PlaceholderHook;
import net.vrcub.play.itembank.tools.RunnableUtil;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BankGUI {
    public BankGUI(Player player, int page) {
        openMenu(player, page);
    }

    public void openMenu(Player player, int page) {
        RunnableUtil.runTaskAsynchronously(() -> {
            ConfigurationSection section = ItemBank.getInstance().getMenuFile().getBankMenuSection();
            String playerName = player.getName();
            // 创建 GUI 界面
            Inventory bankGUI = Bukkit.createInventory(
                    new BankGUIHolder(GUIType.BANK, page),
                    54,
                    PlaceholderHook.set(player, section.getString("title").replace("%page%", String.valueOf((page + 1)))));

            // 准备所需数据
            Collection<BankItem> bankItems = ItemBank.getInstance().getCacheManager().getBankItemCache().values();
            PlayerItem playerItem = ItemBank.getInstance().getCacheManager().getPlayerItemCache().get(playerName);
            // 计算页数和物品范围
            int itemsPerPage = 45;
            int startIndex = page * itemsPerPage;
            int endIndex = startIndex + itemsPerPage;
            // 遍历所有key
            int i = 0;

            for (BankItem bankItem : bankItems) {
                // 如果当前id不在当前页的范围，跳过
                if (i < startIndex || i >= endIndex) {
                    i++;
                    continue;
                }
                // 计算当前页中的槽位
                int slot = i % itemsPerPage;

                // 创建银行物品
                ItemStack itemStack = new ItemBuilder(bankItem.getMaterial())
                        .setAmount(Math.min(Math.max(playerItem.getItemAmount(bankItem.getName()), 1), 64))
                        .setDisplayName(section.getString("bankItems.name").replace("%name%", bankItem.getDisplayName()))
                        .setLore(section.getStringList("bankItems.lore").stream()
                                .map(lore -> lore.replace("%price%", String.valueOf(bankItem.getPrice()))
                                        .replace("%amount%", String.valueOf(playerItem.getItemAmount(bankItem.getName()))))
                                .collect(Collectors.toList()))
                        .setGlow(playerItem.getItemAmount(bankItem.getName()) > 0)
                        .build();
                bankGUI.setItem(slot, itemStack);
                i++;
            }

            // 设置 上一页 和 下一页
            ItemStack prevPageItem = new ItemBuilder(Material.matchMaterial(section.getString("items.prev.mat")))
                    .setDisplayName(section.getString("items.prev.name"))
                    .setLore(section.getStringList("items.prev.lore").stream()
                            .map(lore -> lore.replace("%page%", String.valueOf(page + 1)))
                            .collect(Collectors.toList()))
                    .setCMD(section.getInt("items.prev.cmd"))
                    .build();

            ItemStack nextPageItem = new ItemBuilder(Material.matchMaterial(section.getString("items.next.mat")))
                    .setDisplayName(section.getString("items.next.name"))
                    .setLore(section.getStringList("items.next.lore").stream()
                            .map(lore -> lore.replace("%page%", String.valueOf(page + 1)))
                            .collect(Collectors.toList()))
                    .setCMD(section.getInt("items.next.cmd"))
                    .build();

            ItemStack storeItem = new ItemBuilder(Material.matchMaterial(section.getString("items.store.mat")))
                    .setDisplayName(section.getString("items.store.name"))
                    .setLore(section.getStringList("items.store.lore"))
                    .setCMD(section.getInt("items.store.cmd"))
                    .build();

            ItemStack infoItem = new ItemBuilder(Material.matchMaterial(section.getString("items.info.mat")))
                    .setDisplayName(section.getString("items.info.name"))
                    .setLore(section.getStringList("items.info.lore").stream()
                            .map(lore -> lore.replace("%score%", String.valueOf(playerItem.getScore())))
                            .collect(Collectors.toList()))
                    .setCMD(section.getInt("items.info.cmd"))
                    .build();


            ItemStack rankItem = new ItemBuilder(Material.matchMaterial(section.getString("items.rank.mat")))
                    .setDisplayName(section.getString("items.rank.name"))
                    .setLore(processLore(section.getStringList("items.rank.lore"), ItemBank.getInstance().getCacheManager().getRankCache()))
                    .setCMD(section.getInt("items.rank.cmd"))
                    .build();
            ItemStack frameItem = new ItemBuilder(Material.matchMaterial(section.getString("items.frame.mat")))
                    .setDisplayName(section.getString("items.frame.name"))
                    .setLore(processLore(section.getStringList("items.frame.lore"), ItemBank.getInstance().getCacheManager().getRankCache()))
                    .setCMD(section.getInt("items.frame.cmd"))
                    .build();


            if (page > 0) {
                bankGUI.setItem(45, prevPageItem);
            }
            if (bankGUI.getItem(45) == null) {
                bankGUI.setItem(45, frameItem);
            }

            if (bankItems.size() > endIndex) {
                bankGUI.setItem(53, nextPageItem);
            }
            if (bankGUI.getItem(53) == null) {
                bankGUI.setItem(53, frameItem);
            }
            bankGUI.setItem(46, frameItem);
            bankGUI.setItem(47, frameItem);
            bankGUI.setItem(51, frameItem);
            bankGUI.setItem(52, frameItem);
            bankGUI.setItem(49, storeItem);
            bankGUI.setItem(48, infoItem);
            bankGUI.setItem(50, rankItem);
            RunnableUtil.runTask(() -> player.openInventory(bankGUI));
        });

    }

    private List<String> processLore(List<String> lore, List<String> rank) {
        int rankInsertIndex = -1;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("%rank%")) {
                rankInsertIndex = i;
                lore.remove(i);
                break;
            }
            lore.set(i, line);
        }

        if (rankInsertIndex != -1) {
            lore.addAll(rankInsertIndex, rank);
        }

        return lore;
    }


}
