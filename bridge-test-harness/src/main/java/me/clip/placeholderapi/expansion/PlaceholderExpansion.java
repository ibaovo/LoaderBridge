package me.clip.placeholderapi.expansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 测试用的 PlaceholderExpansion 基类。
 */
public abstract class PlaceholderExpansion {
    public abstract String getIdentifier();

    public String getAuthor() {
        return "unknown";
    }

    public String getVersion() {
        return "unknown";
    }

    public boolean persist() {
        return false;
    }

    public boolean canRegister() {
        return true;
    }

    public List<String> getPlaceholders() {
        return List.of();
    }

    public String onRequest(final OfflinePlayer player, final String params) {
        return null;
    }

    public String onPlaceholderRequest(final Player player, final String params) {
        return onRequest(player, params);
    }
}
