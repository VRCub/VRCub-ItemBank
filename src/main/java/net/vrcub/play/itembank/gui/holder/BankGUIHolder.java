package net.vrcub.play.itembank.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BankGUIHolder implements InventoryHolder {
    private final GUIType guiType;
    private final int page;

    public BankGUIHolder(GUIType guiType, int page) {
        this.guiType = guiType;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public GUIType getGUIType() {
        return guiType;
    }

    public int getPage() {
        return page;
    }
}
