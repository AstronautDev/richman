package com.outbacksmp.richman;

import java.time.DayOfWeek;
import java.util.Locale;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager {

    private final RichManPlugin plugin;
    private final FileConfiguration config;

    public ConfigManager(RichManPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public boolean isChallengeEnabled() {
        return config.getBoolean("challenge.enabled", true);
    }

    public boolean isBroadcastEnabled() {
        return config.getBoolean("challenge.broadcast-msg", true);
    }

    public boolean isBroadcastOnJoinEnabled() {
        return config.getBoolean("challenge.broadcast-on-join", true);
    }

    public DayOfWeek getRunDay() {
        String raw = config.getString("challenge.run-day", "MONDAY");
        return DayOfWeek.valueOf(raw.toUpperCase(Locale.ROOT));
    }

    public int getRunHour() {
        return config.getInt("challenge.run-hour", 0);
    }

    public List<String> getRewardCommands() {
        return config.getStringList("challenge.reward-commands");
    }

    private String getPrefix() {
        String raw = config.getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String msg(String key) {
        String raw = config.getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', getPrefix() + raw);
    }

    public String msgRaw(String key) {
        String raw = config.getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getBroadcastTemplate() {
        String raw = config.getString("messages.broadcast", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
    
}
