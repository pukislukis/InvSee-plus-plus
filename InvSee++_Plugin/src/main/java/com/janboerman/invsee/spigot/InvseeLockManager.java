package com.janboerman.invsee.spigot;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the invseelock state for players.
 * Players with invseelock enabled cannot have their inventory or ender chest spectated
 * unless the spectator has the bypass permission.
 */
public class InvseeLockManager {

    public static final String INVSEELOCK_PERMISSION = "invseeplusplus.invseelock";
    public static final String INVSEELOCK_ADMIN_PERMISSION = "invseeplusplus.invseelock.admin";

    private final JavaPlugin plugin;
    private final Set<UUID> lockedPlayers = new HashSet<>();
    private final File lockFile;

    public InvseeLockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lockFile = new File(plugin.getDataFolder(), "invseelock.yml");
        load();
    }

    public void load() {
        lockedPlayers.clear();
        if (!lockFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(lockFile);
        List<String> uuids = config.getStringList("locked-players");
        for (String uuidStr : uuids) {
            try {
                lockedPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in invseelock.yml: " + uuidStr);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> uuids = new ArrayList<>();
        for (UUID uuid : lockedPlayers) {
            uuids.add(uuid.toString());
        }
        config.set("locked-players", uuids);
        try {
            config.save(lockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save invseelock.yml: " + e.getMessage());
        }
    }

    /**
     * Check whether the given player has locked their inventory.
     *
     * @param uuid the unique ID of the player to check
     * @return true if the player's inventory is locked, otherwise false
     */
    public boolean isLocked(UUID uuid) {
        return lockedPlayers.contains(uuid);
    }

    /**
     * Set the locked state for a player.
     *
     * @param uuid   the unique ID of the player
     * @param locked true to lock, false to unlock
     */
    public void setLocked(UUID uuid, boolean locked) {
        if (locked) {
            lockedPlayers.add(uuid);
        } else {
            lockedPlayers.remove(uuid);
        }
        save();
    }
}
