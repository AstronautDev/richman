package com.outbacksmp.richman;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class RichManCommand implements CommandExecutor, TabCompleter {

    private final RichManPlugin plugin;
    private final ConfigManager config;
    private final DataStore dataStore;
    private final ChallengeManager challengeManager;
    private final BaltopChallenge baltop;
    private final EconomyService economy;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yy ha")
                    .withLocale(Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private DayOfWeek runDay;
    private int runHour;

    private ZonedDateTime now ;
    private ZonedDateTime nextRun;



    public RichManCommand(RichManPlugin plugin,
                          ConfigManager config,
                          DataStore dataStore,
                          ChallengeManager challengeManager,
                          BaltopChallenge baltop,
                          EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.challengeManager = challengeManager;
        this.baltop = baltop;
        this.economy = economy;

        this.runDay = config.getRunDay();
        this.runHour = config.getRunHour();

        this.now = ZonedDateTime.now();
        this.nextRun = now.with(TemporalAdjusters.nextOrSame(runDay))
                .withHour(runHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("richman")) {
            return false;
        }

        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            // /richman forcerun
            if (sub.equals("forcerun")) {
                if (!sender.hasPermission("richman.admin")) {
                    sender.sendMessage(config.msg("no-permission"));
                    return true;
                }
                challengeManager.runNow(sender);
                return true;
            }

            // /richman top [page]
            if (sub.equals("top")) {
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Math.max(1, Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
                sendTopScreen(sender, page);
                return true;
            }
        }

        // Default: main summary screen
        sendMainScreen(sender);
        return true;
    }

    private void sendMainScreen(CommandSender sender) {
        // Optional header line
        String headerRaw = config.msgRaw("header");
        if (headerRaw != null && !headerRaw.trim().isEmpty()) {
            sender.sendMessage(headerRaw);
        }

        // Existing "last winner" behaviour
        Optional<WinnerRecord> lastOpt = dataStore.getLastWinner();
        if (lastOpt.isEmpty()) {
            sender.sendMessage(config.msg("no-winner"));
        } else {
            WinnerRecord last = lastOpt.get();
            String amount = baltop.formatAmount(last.amount());
            Instant when = dataStore.getLastRun();

            String base = config.msgRaw("last-winner")
                    .replace("{player}", last.name())
                    .replace("{amount}", amount);
            sender.sendMessage(base);
            if (when != null) {
                sender.sendMessage("§7Won on: §f" + dateFormatter.format(when));
            }
        }

        // Next run info (based on config run day/hour)
        String nextRunLine = buildNextRunLine();
        if (nextRunLine != null && !nextRunLine.isEmpty()) {
            sender.sendMessage(nextRunLine);
        }

        // Hint so players discover /richman top
        String usage = config.msgRaw("usage-main");
        if (usage == null || usage.isEmpty()) {
            usage = "§7Use §e/richman top§7 to view top balances.";
        }
        sender.sendMessage(usage);
    }

    public String getNextRun() {

        // If we've already passed this week's run time, next is next week
        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusWeeks(1);
        }

        Duration diff = Duration.between(now, nextRun);
        long totalMinutes = diff.toMinutes();
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || sb.length() == 0) sb.append(minutes).append("m");

        return sb.toString();
    }

    private String buildNextRunLine() {
        try {
            String whenStr = dateFormatter.format(nextRun.toInstant());
            String raw = config.msgRaw("next-run");
            if (raw == null || raw.isEmpty()) {
                return "§7Next selection in §f" + getNextRun() + " §7(on §f" + whenStr + "§7)";
            }
            return raw
                    .replace("{time_left}", getNextRun().trim())
                    .replace("{datetime}", whenStr);
        } catch (Exception e) {
            // If config is missing something, just fail silently
            return null;
        }
    }

    private void sendTopScreen(CommandSender sender, int page) {
        if (!economy.isReady()) {
            sender.sendMessage(config.msg("economy-not-ready"));
            return;
        }

        final int pageSize = 10;
        if (page < 1) page = 1;

        int pageRequested = page;

        // Heavy work async so we don't freeze the server
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer[] all = economy.getAllOfflinePlayers();
            List<OfflinePlayer> players = new ArrayList<>();
            Collections.addAll(players, all);

            List<BalanceEntry> balances = new ArrayList<>();
            for (OfflinePlayer p : players) {
                String name = p.getName();
                if (name == null || name.isEmpty()) continue;
                double bal = economy.getBalance(p);
                if (bal <= 0) continue; // ignore broke players
                balances.add(new BalanceEntry(name, bal));
            }

            balances.sort(Comparator.comparingDouble(BalanceEntry::balance).reversed());

            int total = balances.size();
            int totalPages = (int) Math.ceil(total / (double) pageSize);
            if (totalPages == 0) totalPages = 1;

            int pageToUse = Math.min(pageRequested, totalPages);
            int startIndex = (pageToUse - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, total);

            List<BalanceEntry> pageEntries = balances.subList(startIndex, endIndex);

            int finalTotalPages = totalPages;
            int finalPageToUse = pageToUse;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Header
                String header = config.msgRaw("top-header");
                if (header == null || header.isEmpty()) {
                    header = "§6[ Rich Man Top Balances ] §7(Page " + finalPageToUse + "/" + finalTotalPages + ")";
                } else {
                    header = header
                            .replace("{page}", String.valueOf(finalPageToUse))
                            .replace("{pages}", String.valueOf(finalTotalPages));
                }
                sender.sendMessage(header);

                if (pageEntries.isEmpty()) {
                    String emptyMsg = config.msgRaw("top-empty");
                    if (emptyMsg == null || emptyMsg.isEmpty()) {
                        emptyMsg = "§7No players have any balance yet.";
                    }
                    sender.sendMessage(emptyMsg);
                    return;
                }

                // Entries
                for (int i = 0; i < pageEntries.size(); i++) {
                    int position = startIndex + i + 1;
                    BalanceEntry entry = pageEntries.get(i);
                    String amountStr = baltop.formatAmount(entry.balance());

                    String line = config.msgRaw("top-line");
                    if (line == null || line.isEmpty()) {
                        if (position == 1) {
                            line = "§e§l#" + position + " §f" + entry.name() + " §a" + amountStr;
                        } else {
                            line = "§7#" + position + " §f" + entry.name() + " §a" + amountStr;
                        }
                    } else {
                        if (position == 1) {
                            line = "§e§l#" + position + " §f" + entry.name() + " §a" + amountStr;
                        } else {
                            line = line
                                    .replace("{position}", String.valueOf(position))
                                    .replace("{player}", entry.name())
                                    .replace("{amount}", amountStr);
                        }
                    }
                    sender.sendMessage(line);
                }

                int prev = Math.max(1, finalPageToUse - 1);
                int next = Math.min(finalTotalPages, finalPageToUse + 1);

                String footer = config.msgRaw("top-footer");
                if (footer == null || footer.isEmpty()) {
                    footer = "§7Use §e/richman top " + prev + "§7 or §e/richman top " + next + "§7 to change page.";
                } else {
                    footer = footer
                            .replace("{prev}", String.valueOf(prev))
                            .replace("{next}", String.valueOf(next));
                }
                sender.sendMessage(footer);
            });
        });
    }

    private record BalanceEntry(String name, double balance) {}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("richman")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String typed = args[0].toLowerCase(Locale.ROOT);

            if ("top".startsWith(typed)) {
                suggestions.add("top");
            }
            if (sender.hasPermission("richman.admin") && "forcerun".startsWith(typed)) {
                suggestions.add("forcerun");
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("1");
            suggestions.add("2");
            suggestions.add("3");
            return suggestions;
        }

        return Collections.emptyList();
    }
}
