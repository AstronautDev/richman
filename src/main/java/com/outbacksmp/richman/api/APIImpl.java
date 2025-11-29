package com.outbacksmp.richman.api;

import com.outbacksmp.richman.ConfigManager;
import com.outbacksmp.richman.DataStore;
import com.outbacksmp.richman.EconomyService;
import com.outbacksmp.richman.WinnerRecord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

public class APIImpl implements RichManAPI {

    private final DataStore dataStore;
    private final EconomyService economy;
    private final ConfigManager config;

    public APIImpl(DataStore dataStore, EconomyService economy, ConfigManager config) {
        this.dataStore = dataStore;
        this.economy = economy;
        this.config = config;
    }

    @Override
    public Optional<CurrentRichman> getCurrentRichman() {
        Optional<WinnerRecord> opt = dataStore.getLastWinner();
        if(opt.isEmpty()) {
            return Optional.empty();
        }

        WinnerRecord record = opt.get();

        Instant since = dataStore.getLastRun();
        if (since == null) {
            since = Instant.EPOCH;
        }

        double balance = record.amount();
        if(economy != null && economy.isReady()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(record.uuid());
            balance = economy.getBalance(offline);
        }

        final UUID uuid = record.uuid();
        final String name = record.name();
        final double balFinal = balance;
        final Instant sinceFinal = since;

        CurrentRichman current = new CurrentRichman() {
            @Override
            public UUID uuid() {
                return uuid;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public double balance() {
                return balFinal;
            }

            @Override
            public Instant since() {
                return sinceFinal;
            }
        };

        return Optional.of(current);
    }

    @Override
    public Instant getNextSelectionTime() {
        ZonedDateTime now = ZonedDateTime.now();

        DayOfWeek runDay = config.getRunDay();      // e.g. FRIDAY
        LocalTime runTime = config.getRunTime();    // e.g. 18:57 from config

        // First candidate: this week on the configured day + time
        ZonedDateTime nextRun = now
                .with(TemporalAdjusters.nextOrSame(runDay))
                .withHour(runTime.getHour())
                .withMinute(runTime.getMinute())
                .withSecond(0)
                .withNano(0);

        // If that candidate is now or in the past, bump it to next week
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusWeeks(1);
        }

        return nextRun.toInstant();
    }


    @Override
    public Optional<WinnerRecord> getLastWinner() {
        return dataStore.getLastWinner();
    }
}
