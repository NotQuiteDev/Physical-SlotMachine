package org.gamblelife.slotmachine;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

public class StartButtonListener implements Listener {

    private JavaPlugin plugin;
    private Blocks blocks;
    private MoneyManager moneyManager;
    private long lastInteractTime = 0; // 마지막 상호작용 시간을 추적하기 위한 변수
    private final long debouncePeriod = 250; // 디바운스 기간을 250 밀리초로 설정
    public StartButtonListener(JavaPlugin plugin, Blocks blocks, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.blocks = blocks;
        this.moneyManager = moneyManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInteractTime < debouncePeriod) {
            // 디바운스 기간 내의 클릭은 무시
            return;
        }
        lastInteractTime = currentTime; // 마지막 상호작용 시간 업데이트

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;



        ConfigurationSection slotMachines = plugin.getConfig().getConfigurationSection("slotMachines");
        if (slotMachines == null) return;

        for (String key : slotMachines.getKeys(false)) {
            ConfigurationSection machine = slotMachines.getConfigurationSection(key);
            if (machine == null) continue;

            Location startButtonLocation = new Location(
                    Bukkit.getWorld(machine.getString("world")),
                    machine.getInt("start_button_location.x"),
                    machine.getInt("start_button_location.y"),
                    machine.getInt("start_button_location.z")
            );

            if (clickedBlock.getLocation().equals(startButtonLocation)) {
                startGameForMachine(key, event.getPlayer());
                double buttonClickVolume = plugin.getConfig().getDouble("soundSettings.buttonClickVolume", 0.3);  // 기본값은 0.3
                clickedBlock.getWorld().playSound(startButtonLocation, Sound.UI_BUTTON_CLICK, (float)buttonClickVolume, 1.0F);
                break; // 해당하는 버튼을 찾았으니 더 이상 순회할 필요 없음
            }
        }
    }

    private void startGameForMachine(String machineKey, Player player) {
        ConfigurationSection machineConfig = plugin.getConfig().getConfigurationSection("slotMachines." + machineKey);
        if (machineConfig == null) {
            player.sendMessage(ChatColor.RED + "슬롯머신 설정을 찾을 수 없습니다.");
            return;
        }

        if (blocks.isGameRunning(machineKey)) {
            //Bukkit.getLogger().info("[디버그] 게임이 진행 중입니다: " + machineKey);
            // 게임이 진행 중인 경우 다음 블록을 멈춥니다.
            if (blocks.isBlockChanging(machineKey, 1)) {
                //Bukkit.getLogger().info("[디버그] 블록 1이 변경 중입니다. 멈추는 중...");
                blocks.stopChangingBlock(machineKey, 1);
            } else if (blocks.isBlockChanging(machineKey, 2)) {
                //Bukkit.getLogger().info("[디버그] 블록 2가 변경 중입니다. 멈추는 중...");
                blocks.stopChangingBlock(machineKey, 2);
            } else if (blocks.isBlockChanging(machineKey, 3)) {
                //Bukkit.getLogger().info("[디버그] 블록 3이 변경 중입니다. 멈추는 중...");
                blocks.stopChangingBlock(machineKey, 3);

                blocks.processGameResult(machineKey); // 모든 블록이 멈춘 후 게임 결과 처리
            }
        } else {
            double betAmount = moneyManager.getCurrentBetAmountForMachine(machineKey);
            if (moneyManager.withdrawBet(player, betAmount)) {
                blocks.setGameRunning(machineKey, true);
                blocks.setCurrentPlayer(machineKey, player);

                // 확률 배열 로드
                double[] probabilities = blocks.loadProbabilities();

                // 각 블록 변경 시작
                blocks.startChangingBlock(machineKey, 1, machineConfig);
                blocks.startChangingBlock(machineKey, 2, machineConfig);
                blocks.startChangingBlock(machineKey, 3, machineConfig);

                player.sendMessage(ChatColor.GREEN + "게임이 시작됐습니다. 판돈: " + betAmount + " (슬롯머신: " + machineKey + ")");
            } else {
                player.sendMessage(ChatColor.RED + "게임을 시작하기 위한 돈이 충분치 않습니다.");
            }
        }
    }

}