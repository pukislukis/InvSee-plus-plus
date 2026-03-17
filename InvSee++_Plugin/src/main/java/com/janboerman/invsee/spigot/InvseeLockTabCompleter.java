package com.janboerman.invsee.spigot;

import static com.janboerman.invsee.utils.Compat.emptyList;
import static com.janboerman.invsee.utils.Compat.listCopy;

import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tab completer for the /invseelock command.
 */
public class InvseeLockTabCompleter implements TabCompleter {

    private static final List<String> TOGGLES = Arrays.asList("on", "off");

    private final InvseePlusPlus plugin;

    public InvseeLockTabCompleter(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            // Complete toggle: "on" / "off"
            String prefix = args[0];
            List<String> result = new ArrayList<>();
            for (String toggle : TOGGLES) {
                if (StringHelper.startsWithIgnoreCase(toggle, prefix)) {
                    result.add(toggle);
                }
            }
            return result;
        }

        if (args.length == 2 && sender.hasPermission(InvseeLockManager.INVSEELOCK_ADMIN_PERMISSION)) {
            // Complete player name for admin usage
            String prefix = args[1];
            Player senderPlayer = sender instanceof Player ? (Player) sender : null;
            Collection<? extends Player> onlinePlayers = sender.getServer().getOnlinePlayers();

            List<String> onlineNames = new ArrayList<>();
            for (Player onlinePlayer : onlinePlayers) {
                String name = onlinePlayer.getName();
                if ((senderPlayer == null || senderPlayer.canSee(onlinePlayer))
                        && StringHelper.startsWithIgnoreCase(name, prefix)) {
                    onlineNames.add(name);
                }
            }

            if (plugin.offlinePlayerSupport() && plugin.tabCompleteOfflinePlayers()) {
                InvseeAPI api = plugin.getApi();
                SortedSet<String> allNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                allNames.addAll(onlineNames);
                for (String offlineName : api.getUuidCache().keySet()) {
                    if (StringHelper.startsWithIgnoreCase(offlineName, prefix)) {
                        allNames.add(offlineName);
                    }
                }
                return listCopy(allNames);
            } else {
                onlineNames.sort(String.CASE_INSENSITIVE_ORDER);
                return onlineNames;
            }
        }

        return emptyList();
    }
}
