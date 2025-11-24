package com.outbacksmp.richman;

import java.util.Locale;
import java.util.Optional;

import org.bukkit.OfflinePlayer;

public class BaltopChallenge implements Challenge {
    
    private final RichManPlugin plugin;
    private final EconomyService economy;

    public BaltopChallenge(RichManPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public String getName() {
        return "Rich Man (Baltop)";
    }

    @Override
    public Optional<WinnerRecord> evaluate() {
        OfflinePlayer[] players = economy.getAllOfflinePlayers();
        OfflinePlayer best = null;
        double bestBal = -1;

        for (OfflinePlayer p : players) {
            if(!p.hasPlayedBefore()) continue;
            double bal = economy.getBalance(p);
            if(bal > bestBal) {
                bestBal = bal;
                best = p;
            }
        }

        if(best == null) return Optional.empty();
        String name = best.getName() == null ? "Unknown" : best.getName();
        return Optional.of(new WinnerRecord(best.getUniqueId(), name, bestBal));
    }

    public String formatAmount(double amount) {
        return String.format(Locale.US, "%, .2f", amount);
    }

}
