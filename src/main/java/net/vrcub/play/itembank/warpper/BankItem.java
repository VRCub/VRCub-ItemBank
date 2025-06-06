package net.vrcub.play.itembank.warpper;


import org.bukkit.Material;

public class BankItem {

    private final Material material; // 物品

    private final String name;

    private final double price;  // 价格

    private final String displayName; // 展示名

    public BankItem() {
        this.material = Material.AIR;
        this.price = 0.0D;
        this.name = "";
        this.displayName = "";
    }

    public BankItem(Material material, String name, double price, String display) {
        this.material = material;
        this.name = name;
        this.price = price;
        this.displayName = display;
    }
    public Material getMaterial() {
        return this.material;
    }

    public double getPrice() {
        return this.price;
    }

    public String getDisplayName() {
        return this.displayName;
    }
    public String getName() {
        return name;
    }

}