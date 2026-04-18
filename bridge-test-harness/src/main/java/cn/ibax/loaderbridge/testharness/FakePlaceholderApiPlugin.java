package cn.ibax.loaderbridge.testharness;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试用的 PlaceholderAPI 伪插件。
 */
public final class FakePlaceholderApiPlugin {
    private static final FakePlaceholderApiPlugin INSTANCE = new FakePlaceholderApiPlugin();

    private final FakeLocalExpansionManager localExpansionManager = new FakeLocalExpansionManager();

    private FakePlaceholderApiPlugin() {
        localExpansionManager.register(new ServerExpansion());
    }

    public static FakePlaceholderApiPlugin instance() {
        return INSTANCE;
    }

    public String getName() {
        return "PlaceholderAPI";
    }

    public FakeLocalExpansionManager getLocalExpansionManager() {
        return localExpansionManager;
    }

    public static final class FakeLocalExpansionManager {
        private final Map<String, PlaceholderExpansion> expansions = new LinkedHashMap<>();

        public boolean register(final PlaceholderExpansion expansion) {
            String identifier = expansion.getIdentifier();
            if (expansions.containsKey(identifier)) {
                return false;
            }
            expansions.put(identifier, expansion);
            return true;
        }

        public boolean unregister(final PlaceholderExpansion expansion) {
            return expansions.remove(expansion.getIdentifier(), expansion);
        }

        public Collection<String> getIdentifiers() {
            return List.copyOf(expansions.keySet());
        }

        public Optional<PlaceholderExpansion> findExpansionByIdentifier(final String identifier) {
            return Optional.ofNullable(expansions.get(identifier));
        }

        public Collection<PlaceholderExpansion> getExpansions() {
            return List.copyOf(expansions.values());
        }
    }

    private static final class ServerExpansion extends PlaceholderExpansion {
        @Override
        public String getIdentifier() {
            return "server";
        }

        @Override
        public String getAuthor() {
            return "测试";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public List<String> getPlaceholders() {
            return List.of("tps", "motd");
        }

        @Override
        public String onRequest(final OfflinePlayer player, final String params) {
            return switch (params) {
                case "tps" -> "20.0";
                case "motd" -> "测试服务器";
                default -> null;
            };
        }

        @Override
        public String onPlaceholderRequest(final Player player, final String params) {
            return onRequest(player, params);
        }
    }
}
