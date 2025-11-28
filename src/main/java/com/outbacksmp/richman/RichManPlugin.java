package com.outbacksmp.richman;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class RichManPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private DataStore dataStore;
    private EconomyService economyService;
    private ChallengeManager challengeManager;
    private BaltopChallenge baltopChallenge;

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

        challengeManager.startScheduler();


        RichManCommand richManCommand = new RichManCommand(this, configManager, dataStore, challengeManager, baltopChallenge, economyService);
        getCommand("richman").setExecutor(richManCommand);
        getCommand("richman").setTabCompleter(richManCommand);

        startRichManReminderTask(richManCommand);

        getServer().getPluginManager().registerEvents(new JoinListener(null, configManager, dataStore, baltopChallenge), this);

        getLogger().info("RichMan Successfully Enabled and Running.");
    }

    @Override
    public void onDisable() {
        if(dataStore != null) dataStore.save();
        getLogger().info("RichMan Successfully Disabled and Data Saved.");
    }

    private void startRichManReminderTask(RichManCommand richManCommand) {
        // run every 10 minutes (20 ticks * 60s * 10)
        long periodTicks = 10L;

        getServer().getScheduler().runTaskTimer(this, () -> {
            // pull from config and inject {time_left}
            String raw = getConfig().getString(
                    "messages.reminder",
                    "&6[Rich Man]&e A new Rich Man will be selected in &b{time_left}&e. Use &a/richman info &efor how to win."
            );
            raw = raw.replace("{time_left}", richManCommand.getNextRun());

            String message = ChatColor.translateAlternateColorCodes('&', raw);

            getServer().getOnlinePlayers().forEach(p -> p.sendMessage(message));

        }, 20L * 60L * 2L, 20L * 60L * periodTicks);
    }

}