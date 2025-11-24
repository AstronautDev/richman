package com.outbacksmp.richman;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final RichManPlugin plugin;
    private final ConfigManager config;
    private final DataStore dataStore;
    private final BaltopChallenge baltop;

    public JoinListener(RichManPlugin plugin, ConfigManager config, DataStore dataStore, BaltopChallenge baltop) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.baltop = baltop;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(!config.isBroadcastEnabled() || !config.isBroadcastOnJoinEnabled()) {
            return;
        }

        Optional<WinnerRecord> lastOpt = dataStore.getLastWinner();
        if(lastOpt.isEmpty()) return;

        WinnerRecord winner = lastOpt.get();
        String amountStr = baltop.formatAmount(winner.amount());

        String templ = config.getBroadcastTemplate();
        String msg = templ.replace("{player}", winner.name())
          .replace("{amount}", amountStr);

        e.getPlayer().sendMessage(msg);
    }

}