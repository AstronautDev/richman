package com.outbacksmp.richman;

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

        RichManCommand richManCommand = new RichManCommand(configManager, dataStore, challengeManager, baltopChallenge);
        getCommand("richman").setExecutor(richManCommand);
        getCommand("richman").setTabCompleter(richManCommand);

        getServer().getPluginManager().registerEvents(new JoinListener(null, configManager, dataStore, baltopChallenge), this);

        getLogger().info("RichMan Successfully Enabled and Running.");
    }
    @Override
    public void onDisable() {
        if(dataStore != null) dataStore.save();
        getLogger().info("RichMan Successfully Disabled and Data Saved.");
    }

}