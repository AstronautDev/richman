package com.outbacksmp.richman;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ChallengeManager {

    private final RichManPlugin plugin;
    private final Challenge challenge;
    private final DataStore dataStore;
    private final ConfigManager config;
    private final BaltopChallenge baltop;

    public ChallengeManager(RichManPlugin plugin, Challenge challenge, DataStore dataStore, ConfigManager config, BaltopChallenge baltop) {
        this.plugin = plugin;
        this.challenge = challenge;
        this.dataStore = dataStore;
        this.config = config;
        this.baltop = baltop;
    }

    public void startScheduler() {
        long intervalTicks = 5L * 60L * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!config.isChallengeEnabled()) return;
                if(shouldRunNow()) {
                    runChallenge(null);
                }
            }
        }.runTaskTimer(plugin, 20L, intervalTicks);
    }

    private boolean shouldRunNow() {
        Instant lastRun = dataStore.getLastRun();
        ZonedDateTime now = ZonedDateTime.now();
        if(lastRun == null) return true;

        DayOfWeek runDay = config.getRunDay();
        int runHour = config.getRunHour();

        ZonedDateTime thisRun = now.with(TemporalAdjusters.previousOrSame(runDay))
        .withHour(runHour).withMinute(0).withSecond(0).withNano(0);

        return (now.isAfter(thisRun) || now.isEqual(thisRun)) && lastRun.isBefore(thisRun.toInstant());
    }

    public void runNow(CommandSender sender) {
        runChallenge(sender);
    }

    private void runChallenge(CommandSender sender) {
        if(sender != null) {
            sender.sendMessage(config.msg("running"));
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<WinnerRecord> winnerOpt = challenge.evaluate();
            if(winnerOpt.isEmpty()) {
                if(sender != null) {
                    sender.sendMessage(config.msgRaw("no-winner"));
                }
                return;
            }

            WinnerRecord winner = winnerOpt.get();
            dataStore.setLastRun(Instant.now());
            dataStore.setLastWinner(winner);

            String amountStr = baltop.formatAmount(winner.amount());

            // Command Executor
            List<String> commands = config.getRewardCommands();
            for(String cmd : commands) {
                String parsed = cmd.replace("{player}", winner.name())
                    .replace("{uuid}", winner.uuid().toString())
                    .replace("{amount}", amountStr);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
                });
            }

            if(config.isBroadcastEnabled()) {
                String tmpl = config.getBroadcastTemplate();
                String msg = tmpl.replace("{player}", winner.name())
                    .replace("{amount}", amountStr);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for(Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendMessage(msg);
                    }
                });
            }

            String line = config.msgRaw("winner-line")
                .replace("{player}", winner.name())
                .replace("{amount}", amountStr);
            if(sender != null) {
                sender.sendMessage(line);
            }
        });
    }
    
}
