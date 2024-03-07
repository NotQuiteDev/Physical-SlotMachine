package org.gamblelife.slotmachine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final long debouncePeriod = 500; // 디바운스 기간을 500 밀리초로 설정
    private MoneyManager moneyManager; // MoneyManager 참조 추가


    // 원하는 좌표 설정 (예시 좌표)
    private final int[][] blockCoords = {{-36, 65, -76}, {-36, 65, -77}, {-36, 65, -78}};
    private final int[] buttonStartCoords = {-35, 65, -75}; // 스톤 버튼 시작 좌표
    private final int[][] buttonStopCoords = {{-35, 64, -76}, {-35, 64, -77}, {-35, 64, -78}}; // 각 블록 멈춤 버튼 좌표
    private final String worldName = "city2"; // 고정된 월드 이름
    public ButtonListener(JavaPlugin plugin, Blocks blocks, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.blocks = blocks;
        this.moneyManager = moneyManager; // MoneyManager 인스턴스 저장
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 좌표 설정
        blocks.setBlock1Coords(blockCoords[0]);
        blocks.setBlock2Coords(blockCoords[1]);
        blocks.setBlock3Coords(blockCoords[2]);
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInteractTime < debouncePeriod) {
            // 디바운스 기간 내의 클릭은 무시
            return;
        }
        lastInteractTime = currentTime; // 마지막 상호작용 시간 업데이트

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // 스톤 버튼으로 시작하는 경우
        if (clickedBlock.getType() == Material.STONE_BUTTON && checkCoords(clickedBlock, buttonStartCoords)) {
            if (!blocks.isGameRunning()) {
                double betAmount = moneyManager.getBetAmount(); // 현재 판돈을 가져옵니다.
                if (moneyManager.withdrawBet(player, betAmount)) {
                    // 충분한 돈이 있으면 게임을 시작합니다.
                    blocks.setGameRunning(true);
                    blocks.setCurrentPlayer(player);
                    // 각 블록 변경 시작
                    blocks.startChangingBlock1(blockCoords[0][0], blockCoords[0][1], blockCoords[0][2], new double[]{0.4, 0.1, 0.05, 0.25, 0.2});
                    blocks.startChangingBlock2(blockCoords[1][0], blockCoords[1][1], blockCoords[1][2], new double[]{0.4, 0.1, 0.05, 0.25, 0.2});
                    blocks.startChangingBlock3(blockCoords[2][0], blockCoords[2][1], blockCoords[2][2], new double[]{0.4, 0.1, 0.05, 0.25, 0.2});
                    // 게임 시작 메시지
                    player.sendMessage(ChatColor.GREEN + "게임이 시작됐습니다. 판돈 : " + betAmount );
                } else {
                    // 돈이 부족하면 게임을 시작하지 않고 메시지를 보냅니다.
                    player.sendMessage(ChatColor.RED + "게임을 시작하기위한 돈이 충분치 않습니다.");
                }
            } else {
                // 게임이 이미 진행 중일 때 메시지 출력
                player.sendMessage(ChatColor.RED + "슬롯머신이 이미 작동중입니다.");
            }
        }



        for (int i = 0; i < buttonStopCoords.length; i++) {
            if (clickedBlock.getType() == Material.STONE_BUTTON && checkCoords(clickedBlock, buttonStopCoords[i])) {
                // tasks Map을 사용하는 코드는 제거됩니다
                // 아래의 코드는 실제로 정지 명령을 실행할 것입니다
                switch (i) {
                    case 0:
                        blocks.stopChangingBlock1();

                        break;
                    case 1:
                        blocks.stopChangingBlock2();

                        break;
                    case 2:
                        blocks.stopChangingBlock3();

                        break;
                }
                blocks.setGameRunning(false); // 게임이 멈췄으니 상태를 업데이트합니다
            }
        }
        if (clickedBlock.getType() == Material.STONE_BUTTON) {
            if (checkCoords(clickedBlock, new int[]{-35, 67, -75})) {
                moneyManager.setBetAmount(1000);
                player.sendMessage(ChatColor.GREEN + "판돈이 1,000원으로 설정됐습니다!");
            } else if (checkCoords(clickedBlock, new int[]{-35, 67, -76})) {
                moneyManager.setBetAmount(10000);
                player.sendMessage(ChatColor.GREEN + "판돈이 10,000원으로 설정됐습니다!");
            } else if (checkCoords(clickedBlock, new int[]{-35, 67, -77})) {
                moneyManager.setBetAmount(100000);
                player.sendMessage(ChatColor.GREEN + "판돈이 100,000원으로 설정됐습니다!");
            } else if (checkCoords(clickedBlock, new int[]{-35, 67, -78})) {
                moneyManager.setBetAmount(1000000);
                player.sendMessage(ChatColor.GREEN + "판돈이 1,000,000원으로 설정됐습니다!");
            } else if (checkCoords(clickedBlock, new int[]{-35, 67, -79})) {
                moneyManager.setBetAmount(10000000);
                player.sendMessage(ChatColor.GREEN + "판돈이 10,000,000원으로 설정됐습니다!");
            }
        }


    }



    // 좌표 체크 메소드
    private boolean checkCoords(Block block, int[] coords) {
        return block.getX() == coords[0] && block.getY() == coords[1] && block.getZ() == coords[2];
    }
}