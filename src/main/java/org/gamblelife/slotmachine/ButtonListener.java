package org.gamblelife.slotmachine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;

public class ButtonListener implements Listener {

    private JavaPlugin plugin;
    private Blocks blocks;
    private Map<Integer, BukkitTask> tasks = new HashMap<>();
    private long lastInteractTime = 0; // 마지막 상호작용 시간을 추적하기 위한 변수

    private MoneyManager moneyManager; // MoneyManager 참조 추가


    public ButtonListener(JavaPlugin plugin, Blocks blocks, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.blocks = blocks;
        this.moneyManager = moneyManager; // MoneyManager 인스턴스 저장
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        long currentTime = System.currentTimeMillis();
        // 마지막 상호작용 시간과 현재 시간의 차이가 100밀리초(0.1초) 미만이면 이벤트 처리를 무시합니다.
        if (currentTime - lastInteractTime < 100) {
            return;
        }

        lastInteractTime = currentTime;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.BIRCH_BUTTON) return;

        // config에서 슬롯머신 설정을 순회
        ConfigurationSection slotMachines = plugin.getConfig().getConfigurationSection("slotMachines");
        if (slotMachines == null) return;

        for (String key : slotMachines.getKeys(false)) {
            ConfigurationSection machine = slotMachines.getConfigurationSection(key);
            if (machine == null) continue;

            Location betButtonLocation = new Location(
                    Bukkit.getWorld(machine.getString("world")),
                    machine.getInt("betting_button_location.x"),
                    machine.getInt("betting_button_location.y"),
                    machine.getInt("betting_button_location.z")
            );

            // 클릭된 블록이 현재 순회 중인 슬롯머신의 베팅 버튼인지 확인
            if (clickedBlock.getLocation().equals(betButtonLocation)) {
                // 각 슬롯머신별로 판돈 순환
                moneyManager.cycleBetAmountForMachine(key);
                Player player = event.getPlayer();
                player.sendMessage(ChatColor.GREEN + "판돈이 " + moneyManager.getCurrentBetAmountForMachine(key) + "원으로 설정됐습니다. (슬롯머신: " + key + ")");
                break; // 해당하는 버튼을 찾았으니 더 이상 순회할 필요 없음
            }
        }
    }
}