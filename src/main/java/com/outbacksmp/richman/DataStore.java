package com.outbacksmp.richman;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DataStore {

    private final RichManPlugin plugin;
    private File dataFile;
    private FileConfiguration data;

    public DataStore(RichManPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if(!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch(IOException e) {
                plugin.getLogger().severe("Could not create data file.");
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save to data.yml");
            e.printStackTrace();
        }
    }

    public Instant getLastRun() {
        String s = data.getString("last-run");
        if(s == null || s.isEmpty()) return null;
        return Instant.parse(s);
    }

    public void setLastRun(Instant instant) {
        data.set("last-run", instant.toString());
        save();
    }

    public void setLastWinner(WinnerRecord winner) {
        data.set("last-winner.uuid", winner.uuid().toString());
        data.set("last-winner.name", winner.name());
        data.set("last-winner.amount", winner.amount());
        save();
    }

    public Optional<WinnerRecord> getLastWinner() {
        String uuidStr = data.getString("last-winner.uuid");
        if(uuidStr == null) return Optional.empty();
        UUID uuid = UUID.fromString(uuidStr);
        String name = data.getString("last-winner.name", "Unknown");
        double amount = data.getDouble("last-winner.amount", 0.0);
        return Optional.of(new WinnerRecord(uuid, name, amount));
    }
    
}
