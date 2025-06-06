package net.vrcub.play.itembank.gui.type;

import net.vrcub.play.itembank.ItemBank;
import net.vrcub.play.itembank.gui.builder.ItemBuilder;
import net.vrcub.play.itembank.gui.holder.BankGUIHolder;
import net.vrcub.play.itembank.gui.holder.GUIType;
import net.vrcub.play.itembank.hook.PlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StoreGUI {
    public StoreGUI(Player player) {
        openMenu(player);
    }

    private void openMenu(Player player) {
        ConfigurationSection section = ItemBank.getInstance().getMenuFile().getStoreMenuSection();
        // 创建 GUI 界面
        Inventory storeGUI = Bukkit.createInventory(
                new BankGUIHolder(GUIType.STORE, 1),
                54,
                PlaceholderHook.set(player, section.getString("title")));

        ItemStack saveItemItem = new ItemBuilder(Material.matchMaterial(section.getString("items.save.mat")))
                .setDisplayName(section.getString("items.save.name"))
                .setLore(section.getStringList("items.save.lore"))
                .setCMD(section.getInt("items.save.cmd"))
                .build();
        
        ItemStack frameItem = new ItemBuilder(Material.matchMaterial(section.getString("items.frame.mat")))
                .setDisplayName(section.getString("items.frame.name"))
                .setLore(section.getStringList("items.frame.lore"))
                .setCMD(section.getInt("items.frame.cmd"))
                .build();

        int[] frameSlots = {45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : frameSlots) {
            storeGUI.setItem(slot, frameItem);
        }
        storeGUI.setItem(49, saveItemItem);
        player.openInventory(storeGUI);
    }

}
