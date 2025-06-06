package net.vrcub.play.itembank.gui.type;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.gui.builder.ItemBuilder;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.hook.PlaceholderHook;
import net.vrcub.play.itembank.tools.RunnableUtil;
import net.vrcub.play.itembank.warpper.BankItem;
import net.vrcub.play.itembank.warpper.PlayerItem;
import net.vrcub.play.itembank.warpper.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.stream.Collectors;

public class ShopGUI {
    public static Map<Integer,Inventory> invMap =  new HashMap<>();
    public ShopGUI(Player player, int page) {
        if (invMap.get(page) == null) {
            openMenu(player, page);
        } else {
            player.openInventory(invMap.get(page));
        }
    }

    public void openMenu(Player player, int page) {
        RunnableUtil.runTaskAsynchronously(() -> {
            ConfigurationSection section = ItemBank.getInstance().getMenuFile().getShopMenuSection();
            String playerName = player.getName();
            // 创建 GUI 界面
            Inventory shopGUI = Bukkit.createInventory(
                    new BankGUIHolder(GUIType.SHOP, page),
                    54,
                    PlaceholderHook.set(player, section.getString("title").replace("%page%", String.valueOf((page + 1)))));

            // 准备所需数据
            Collection<ShopItem> shopItems = ItemBank.getInstance().getCacheManager().getShopItemCache().values();
         
            // 计算页数和物品范围
            int itemsPerPage = 45;
            int startIndex = page * itemsPerPage;
            int endIndex = startIndex + itemsPerPage;
            // 遍历所有key
            int i = 0;

            for (ShopItem shopItem : shopItems) {
                // 如果当前id不在当前页的范围，跳过
                if (i < startIndex || i >= endIndex) {
                    i++;
                    continue;
                }
                // 计算当前页中的槽位
                int slot = i % itemsPerPage;

                // 创建银行物品
                ItemStack itemStack = shopItem.getItemStack().clone();
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta != null) {
                    List<String> lore = itemStack.getItemMeta().getLore();
                    if (lore != null) {
                        lore.addAll(section.getStringList("items.shopitem.lore"));
                    } else {
                        lore = new ArrayList<>(section.getStringList("items.shopitem.lore"));
                    }
                    List<String> newLore = new ArrayList<>();
                    // 遍历原 lore 列表
                    for (String line : lore) {
                        // 对每个元素进行替换操作
                        String replacedLine = line.replace("%price%", String.valueOf(shopItem.getPrice()));
                        // 将替换后的元素添加到新列表中
                        newLore.add(replacedLine);
                    }
                    itemMeta.setLore(newLore);

                }
                PersistentDataContainer container = itemMeta.getPersistentDataContainer();
                // 给容器设置一个 String 值
                container.set(ItemBank.getInstance().getKey(), PersistentDataType.STRING, shopItem.getName());
                itemStack.setItemMeta(itemMeta);
                shopGUI.setItem(slot, itemStack);
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

            ItemStack backItem = new ItemBuilder(Material.matchMaterial(section.getString("items.back.mat")))
                    .setDisplayName(section.getString("items.back.name"))
                    .setLore(section.getStringList("items.back.lore"))
                    .setCMD(section.getInt("items.back.cmd"))
                    .build();
            
            ItemStack frameItem = new ItemBuilder(Material.matchMaterial(section.getString("items.frame.mat")))
                    .setDisplayName(section.getString("items.frame.name"))
                    .setLore(section.getStringList("items.frame.lore"))
                    .setCMD(section.getInt("items.frame.cmd"))
                    .build();


            if (page > 0) {
                shopGUI.setItem(45, prevPageItem);
            }
            if (shopGUI.getItem(45) == null) {
                shopGUI.setItem(45, frameItem);
            }

            if (shopItems.size() > endIndex) {
                shopGUI.setItem(53, nextPageItem);
            }
            if (shopGUI.getItem(53) == null) {
                shopGUI.setItem(53, frameItem);
            }
            shopGUI.setItem(46, frameItem);
            shopGUI.setItem(47, frameItem);
            shopGUI.setItem(51, frameItem);
            shopGUI.setItem(52, frameItem);
            shopGUI.setItem(49, backItem);
            shopGUI.setItem(48, frameItem);
            shopGUI.setItem(50, frameItem);
            RunnableUtil.runTask(() -> {
                player.openInventory(shopGUI);
                invMap.put(page,shopGUI);
            });
        });

    }


}
