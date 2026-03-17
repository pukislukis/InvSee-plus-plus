package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.InvseeAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the /invseelock command.
 * <p>
 * Usage:
 * <ul>
 *   <li>/invseelock on|off — toggle your own inventory lock (requires invseeplusplus.invseelock)</li>
 *   <li>/invseelock on|off [player] — toggle another player's inventory lock (requires invseeplusplus.invseelock.admin)</li>
 * </ul>
 */
public class InvseeLockCommandExecutor implements CommandExecutor {

    private final InvseePlusPlus plugin;

    public InvseeLockCommandExecutor(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String toggle = args[0];
        if (!toggle.equalsIgnoreCase("on") && !toggle.equalsIgnoreCase("off")) {
            return false;
        }
        boolean lock = toggle.equalsIgnoreCase("on");

        // Self-lock: /invseelock on|off
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players when no target is specified!");
                return true;
            }

            if (!sender.hasPermission(InvseeLockManager.INVSEELOCK_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            plugin.getLockManager().setLocked(uuid, lock);

            if (lock) {
                sender.sendMessage(ChatColor.GREEN + "Your inventory is now locked. Others cannot spectate it.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Your inventory is now unlocked. Others can spectate it.");
            }
            return true;
        }

        // Admin-lock: /invseelock on|off [player]
        if (!sender.hasPermission(InvseeLockManager.INVSEELOCK_ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to toggle invseelock for other players.");
            return true;
        }

        String targetName = args[1];

        // Try online player first
        Player onlineTarget = plugin.getServer().getPlayerExact(targetName);
        if (onlineTarget != null) {
            plugin.getLockManager().setLocked(onlineTarget.getUniqueId(), lock);
            sendAdminToggleMessage(sender, onlineTarget.getName(), lock);
            return true;
        }

        // Try UUID parse
        UUID targetUuid = null;
        try {
            targetUuid = UUID.fromString(targetName);
        } catch (IllegalArgumentException ignored) {
        }

        if (targetUuid != null) {
            final UUID finalUuid = targetUuid;
            plugin.getLockManager().setLocked(finalUuid, lock);
            // Try to get display name from cache
            InvseeAPI api = plugin.getApi();
            String displayName = api.getUserNameCache().getOrDefault(finalUuid, targetName);
            sendAdminToggleMessage(sender, displayName, lock);
            return true;
        }

        // Offline player — resolve UUID asynchronously
        InvseeAPI api = plugin.getApi();
        CompletableFuture<Optional<UUID>> uuidFuture = api.fetchUniqueId(targetName);

        uuidFuture.whenComplete((optUuid, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "An error occurred while looking up " + targetName + ".");
                return;
            }
            if (!optUuid.isPresent()) {
                sender.sendMessage(ChatColor.RED + "Player " + targetName + " does not exist or has never joined this server.");
                return;
            }
            UUID resolvedUuid = optUuid.get();
            plugin.getLockManager().setLocked(resolvedUuid, lock);
            sendAdminToggleMessage(sender, targetName, lock);
        });

        return true;
    }

    private static void sendAdminToggleMessage(CommandSender sender, String targetName, boolean lock) {
        if (lock) {
            sender.sendMessage(ChatColor.GREEN + targetName + "'s inventory has been locked.");
        } else {
            sender.sendMessage(ChatColor.GREEN + targetName + "'s inventory has been unlocked.");
        }
    }
}
