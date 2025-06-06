package net.vrcub.play.itembank.hook;

import net.vrcub.play.itembank.ItemBank;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {
    private final ItemBank plugin;

    public PlaceholderHook(ItemBank plugin) {
        this.plugin = plugin;
    }

    public static List<String> set(Player player, List<String> content) {
        return PlaceholderAPI.setPlaceholders(player, content);
    }

    public static String set(Player player, String content) {
        return PlaceholderAPI.setPlaceholders(player, content);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "itembank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "404Yuner";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.startsWith("get")) {
            return String.valueOf(plugin.getCacheManager().getPlayerItemCache().get(player.getName()).getScore());
        }
        return params;
    }

}
