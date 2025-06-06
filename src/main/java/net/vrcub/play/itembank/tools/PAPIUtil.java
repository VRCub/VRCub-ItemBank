package net.vrcub.play.itembank.tools;

import net.vrcub.play.itembank.hook.PlaceholderHook;
import org.bukkit.entity.Player;

import java.util.List;

public class PAPIUtil {
    public static String set(Player player, String input) {
        return ColorUtil.colorize(PlaceholderHook.set(player,input));
    }
    public static List<String> set(Player player, List<String> input) {
        return ColorUtil.colorize(PlaceholderHook.set(player,input));
    }
}
