package net.vrcub.play.itembank.warpper;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ShopItem {

    private final ItemStack itemStack; // 物品

    private final String name;

    private final double price;  // 价格



    public ShopItem(ItemStack itemStack, String name, double price) {
        this.itemStack = itemStack;
        this.name = name;
        this.price = price;
    }
    public ItemStack getItemStack() {
        return this.itemStack;
    }
    public double getPrice() {
        return this.price;
    }

    public String getName() {
        return name;
    }
}
