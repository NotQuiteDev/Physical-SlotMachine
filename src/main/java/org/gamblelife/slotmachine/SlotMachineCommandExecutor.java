package org.gamblelife.slotmachine;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotMachineCommandExecutor implements CommandExecutor {

    private JavaPlugin plugin;
    private Blocks blocks;
    /*public SlotMachineCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    } */
    // 생성자에서 Blocks 객체도 전달받도록 수정
    public SlotMachineCommandExecutor(JavaPlugin plugin, Blocks blocks) {
        this.plugin = plugin;
        this.blocks = blocks;  // 전달받은 Blocks 객체를 필드에 저장
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("slotmachine")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                // 설정 파일 리로드 로직
                plugin.reloadConfig();
                blocks.reloadConfigMultipliers();  // Blocks를 통해 ConfigMultipliers의 설정 리로드
                blocks.updateSlotChangeSpeed();
                sender.sendMessage(ChatColor.GREEN + "SlotMachine 설정이 리로드되었습니다.");
                return true;
            }
        }
        return false;
    }
}