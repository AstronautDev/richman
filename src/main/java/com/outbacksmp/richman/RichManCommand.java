package com.outbacksmp.richman;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class RichManCommand implements CommandExecutor, TabCompleter{

    private final ConfigManager config;
    private final DataStore dataStore;
    private final ChallengeManager challengeManager;
    private final BaltopChallenge baltop;
    private final DateTimeFormatter dateFormatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.US)
        .withZone(ZoneId.systemDefault());

    public RichManCommand(ConfigManager config, DataStore dataStore, ChallengeManager challengeManager, BaltopChallenge baltop) {
        this.config = config;
        this.dataStore = dataStore;
        this.challengeManager = challengeManager;
        this.baltop = baltop;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("forcerun")) {
                if(!sender.hasPermission("richman.admin")) {
                    sender.sendMessage(config.msg("no-permission"));
                    return true;
                }
                challengeManager.runNow(sender);
                return true;
            }
        }

        Optional<WinnerRecord> lastOpt = dataStore.getLastWinner();
        if(lastOpt.isEmpty()) {
            sender.sendMessage(config.msg("no-winner"));
            return true;
        }

        WinnerRecord last = lastOpt.get();
        String amount = baltop.formatAmount(last.amount());
        Instant when = dataStore.getLastRun();

        String base = config.msgRaw("last-winner")
            .replace("{player}", last.name())
            .replace("{amount}", amount);
        sender.sendMessage(base);
        if(when != null) {
            sender.sendMessage("§7Evaluated at: §f" + dateFormatter.format(when));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(!cmd.getName().equalsIgnoreCase("richman")) {
            return Collections.emptyList();
        }

        if(args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            
            if(sender.hasPermission("richman.admin")) {
                String typed = args[0].toLowerCase();
                if ("forcerun".startsWith(typed)) {
                    suggestions.add("forcerun");
                }
            }

            return suggestions;
        }
        return Collections.emptyList();
    }
    
}
