package com.outbacksmp.richman;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class EconomyService {

    private final RichManPlugin plugin;
    private Economy economy;

    public EconomyService(RichManPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        if(plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if(rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isReady() {
        return economy != null;
    }
    
    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public OfflinePlayer[] getAllOfflinePlayers() {
        return plugin.getServer().getOfflinePlayers();
    }
    
}
