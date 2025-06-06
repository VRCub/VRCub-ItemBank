package net.vrcub.play.itembank.tools;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtil {

    public static ItemStack handleItemStack(String mat) {
        String matName = mat.toUpperCase();
        try {
            return new ItemStack(Material.matchMaterial(matName));
        } catch (Exception e) {
            return new ItemStack(Material.BEDROCK);
        }
    }

    public static void addGlow(ItemMeta itemMeta) {
        try {
            if (itemMeta != null) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } catch (NoClassDefFoundError ignore) {
            // 低版本无方法
        }
    }
}
