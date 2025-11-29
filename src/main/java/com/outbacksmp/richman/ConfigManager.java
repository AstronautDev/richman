package com.outbacksmp.richman;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;

import org.bukkit.configuration.file.FileConfiguration;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager {

    private final RichManPlugin plugin;

    public ConfigManager(RichManPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    // Optional, just a helper if you ever want to call it from outside
    public void reload() {
        plugin.reloadConfig();
    }

    public boolean isChallengeEnabled() {
        return cfg().getBoolean("challenge.enabled", true);
    }

    public boolean isBroadcastEnabled() {
        return cfg().getBoolean("challenge.broadcast-msg", true);
    }

    public boolean isBroadcastOnJoinEnabled() {
        return cfg().getBoolean("challenge.broadcast-on-join", true);
    }

    public DayOfWeek getRunDay() {
        String raw = cfg().getString("challenge.run-day", "MONDAY");
        return DayOfWeek.valueOf(raw.toUpperCase(Locale.ROOT));
    }

    public int getRunHour() {
        return cfg().getInt("challenge.run-hour", 0);
    }

    // if you added run-time as HH:mm:
    public java.time.LocalTime getRunTime() {
        String raw = cfg().getString("challenge.run-time", "18:00");
        return java.time.LocalTime.parse(raw); // 24h format
    }

    public java.time.ZoneId getScheduleZone() {
        String id = cfg().getString("challenge.time-zone", java.time.ZoneId.systemDefault().getId());
        return java.time.ZoneId.of(id);
    }

    public List<String> getRewardCommands() {
        return cfg().getStringList("challenge.reward-commands");
    }

    private String getPrefix() {
        String raw = cfg().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String msg(String key) {
        String raw = cfg().getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', getPrefix() + raw);
    }

    public String msgRaw(String key) {
        String raw = cfg().getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getBroadcastTemplate() {
        String raw = cfg().getString("messages.broadcast", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
