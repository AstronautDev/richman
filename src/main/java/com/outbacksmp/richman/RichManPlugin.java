package com.outbacksmp.richman;

import com.outbacksmp.richman.api.APIImpl;
import com.outbacksmp.richman.api.RichManAPI;
import com.outbacksmp.richman.schedulers.CountdownScheduler;
import org.bukkit.ChatColor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class RichManPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private DataStore dataStore;
    private EconomyService economyService;
    private ChallengeManager challengeManager;
    private BaltopChallenge baltopChallenge;
    private CountdownScheduler countdownScheduler;

    private RichManAPI richManAPI;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);


        this.economyService = new EconomyService(this);
        if(!economyService.init()) {
            getLogger().severe("No Vault dependency found. Disabling RichMan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.dataStore = new DataStore(this);

        this.baltopChallenge = new BaltopChallenge(this, economyService);
        Challenge coreChallenge = baltopChallenge;

        this.challengeManager = new ChallengeManager(this, coreChallenge, dataStore, configManager, baltopChallenge);
        this.countdownScheduler = new CountdownScheduler(this, challengeManager, richManAPI);

        // API Implementation
        this.richManAPI = new APIImpl(dataStore, economyService, configManager);
        getServer().getServicesManager().register(RichManAPI.class, richManAPI, this, ServicePriority.Normal);

        countdownScheduler.scheduleCountdowns(getRichManAPI().getNextSelectionTime());




        RichManCommand richManCommand = new RichManCommand(this, configManager, dataStore, challengeManager, baltopChallenge, economyService);
        getCommand("richman").setExecutor(richManCommand);
        getCommand("richman").setTabCompleter(richManCommand);

        startRichManReminderTask();

        getServer().getPluginManager().registerEvents(new JoinListener(null, configManager, dataStore, baltopChallenge), this);

        getLogger().info("RichMan Successfully Enabled and Running.");
    }

    @Override
    public void onDisable() {
        if(dataStore != null) dataStore.save();
        getLogger().info("RichMan Successfully Disabled and Data Saved.");

        getServer().getServicesManager().unregisterAll(this);
    }

    private void startRichManReminderTask() {

        long periodMinutes = 10L;

        getServer().getScheduler().runTaskTimer(this, () -> {

            Instant next = getRichManAPI().getNextSelectionTime();
            if (next == null) {
                return; // no event scheduled for some reason
            }

            Instant now = Instant.now();
            Duration d = Duration.between(now, next);

            // If we are past the time, don't spam
            if (d.isNegative()) {
                return;
            }

            String timeLeft = formatDurationShort(d); // e.g. "2d 3h", "5h 12m", "3m 10s"

            String raw = getConfig().getString(
                    "messages.reminder",
                    "&6[Rich Man]&e A new Rich Man will be selected in &b{time_left}&e. Use &a/richman info &efor how to win."
            );
            raw = raw.replace("{time_left}", timeLeft);

            String msg = ChatColor.translateAlternateColorCodes('&', raw);
            getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));

        }, 20L * 60L * 2L, 20L * 60L * periodMinutes);
    }

    // helper in RichManPlugin
    private String formatDurationShort(Duration d) {
        long seconds = d.getSeconds();
        if (seconds <= 0) {
            return "soon";
        }

        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 && days == 0) sb.append(minutes).append("m");

        String out = sb.toString().trim();
        return out.isEmpty() ? "soon" : out;
    }


    public RichManAPI getRichManAPI() {
        return richManAPI;
    }

}