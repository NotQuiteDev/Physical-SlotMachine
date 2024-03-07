package org.gamblelife.slotmachine;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotMachineCommandExecutor implements CommandExecutor {

    private JavaPlugin plugin;

    public SlotMachineCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("slotmachine")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                // 설정 파일 리로드 로직
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "SlotMachine 설정이 리로드되었습니다.");
                return true;
            }
        }
        return false;
    }
}