package com.outbacksmp.richman.schedulers;

import com.outbacksmp.richman.RichManPlugin;
import com.outbacksmp.richman.ChallengeManager;
import com.outbacksmp.richman.api.RichManAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CountdownScheduler {

    private final RichManPlugin plugin;
    private final ChallengeManager challengeManager;
    private final RichManAPI api;

    private final List<BukkitTask> tasks = new ArrayList<>();

    public CountdownScheduler(RichManPlugin plugin,
                              ChallengeManager challengeManager,
                              RichManAPI api) {
        this.plugin = plugin;
        this.challengeManager = challengeManager;
        this.api = api;
    }

    /**
     * Clear any existing countdown tasks.
     */
    public void clear() {
        for (BukkitTask task : tasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
    }

    /**
     * Entry point: schedule countdowns + final event for the given selection time.
     */
    public void scheduleCountdowns(Instant selectionTime) {
        clear();

        if (selectionTime == null) {
            plugin.getLogger().warning("[Countdown] selectionTime is null, not scheduling.");
            return;
        }

        long nowMs = System.currentTimeMillis();
        long targetMs = selectionTime.toEpochMilli();

        if (targetMs <= nowMs) {
            plugin.getLogger().warning("[Countdown] selectionTime is in the past, skipping schedule.");
            return;
        }

        plugin.getLogger().info("[Countdown] Scheduling RichMan countdowns for " + selectionTime);

        // Helper to schedule a single broadcast at (selectionTime - offset)
        scheduleOffset(selectionTime, Duration.ofHours(1), () ->
                broadcast("&6[Rich Man]&e event draws in &b1 hour&e, get your money up!")
        );

        scheduleOffset(selectionTime, Duration.ofMinutes(5), () ->
                broadcast("&6[Rich Man]&e event draws in &b5 minutes&e!")
        );

        scheduleOffset(selectionTime, Duration.ofMinutes(1), () ->
                broadcast("&6[Rich Man]&e event draws in &b1 minute&e!")
        );

        scheduleOffset(selectionTime, Duration.ofSeconds(30), () ->
                broadcast("&6[Rich Man]&e event draws in &b30 seconds&e!")
        );

        scheduleOffset(selectionTime, Duration.ofSeconds(10), () ->
                broadcast("&6[Rich Man]&e event draws in &b10 seconds&e!")
        );

        // 10 → 1 countdown
        for (int i = 9; i >= 1; i--) {
            final int num = i;
            scheduleOffset(selectionTime, Duration.ofSeconds(num), () ->
                    broadcast("&6[Rich Man]&e drawing in &b" + num + "&e...")
            );
        }

        // Final: run the actual event
        scheduleAt(selectionTime, () -> {
            plugin.getLogger().info("[Countdown] Reached selection time, running RichMan challenge now.");
            // This calls all your existing winner logic
            challengeManager.runNow(Bukkit.getConsoleSender());

            // After it runs, ask the API for the NEXT selection time and schedule again
            Instant next = api.getNextSelectionTime();
            if (next != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> scheduleCountdowns(next));
            } else {
                plugin.getLogger().warning("[Countdown] api.getNextSelectionTime() returned null after runNow.");
            }
        });
    }

    /**
     * Schedule a Runnable to run at (selectionTime - offset).
     */
    private void scheduleOffset(Instant selectionTime, Duration offset, Runnable action) {
        Instant when = selectionTime.minus(offset);
        scheduleAt(when, action);
    }

    /**
     * Core: schedule a Runnable at a specific Instant.
     */
    private void scheduleAt(Instant when, Runnable action) {
        long delayMs = when.toEpochMilli() - System.currentTimeMillis();
        if (delayMs <= 0) {
            // time already passed
            return;
        }

        long delayTicks = delayMs / 50L;
        if (delayTicks <= 0) delayTicks = 1;

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, action, delayTicks);
        tasks.add(task);
    }

    private void broadcast(String raw) {
        String msg = ChatColor.translateAlternateColorCodes('&', raw);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        plugin.getServer().getConsoleSender().sendMessage(msg);
    }
}
