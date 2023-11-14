package org.gamblelife.slotmachine;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class Blocks {
    private int[] block1Coords;
    private int[] block2Coords;
    private int[] block3Coords;

    // 좌표를 설정하는 메서드들...
    public void setBlock1Coords(int[] coords) {
        this.block1Coords = coords;
    }
    public void setBlock2Coords(int[] coords) {
        this.block2Coords = coords;
    }
    public void setBlock3Coords(int[] coords) {
        this.block3Coords = coords;
    }
    private boolean block1Stopped = false;
    private boolean block2Stopped = false;
    private boolean block3Stopped = false;
    private MoneyManager moneyManager;
    // 각 블록 타입별 상금 배율
    public double prizeMultiplierForDirt= 12;
    public double prizeMultiplierForDiamond=900;
    public double prizeMultiplierForEmerald=7777;
    public double prizeMultiplierForIron=60;
    public double prizeMultiplierForGold=120;
    private JavaPlugin plugin;

    private final String worldName = "city2";
    private BukkitTask task1, task2, task3;
    private Material lastBlockType = null; // 마지막으로 변경된 블록 종류를 저장
    private boolean isGameRunning = false;
    // 게임이 진행 중인지 확인하는 메소드
    private boolean isBlock1Stopped, isBlock2Stopped, isBlock3Stopped;
    private Player currentPlayer;

    public boolean isGameRunning() {
        return isGameRunning;
    }
    // 게임 상태를 설정하는 메소드
    public void setGameRunning(boolean running) {
        this.isGameRunning = running;
    }

    public void checkGameResult() {
        if (!isGameRunning && !isBlock1Stopped && !isBlock2Stopped && !isBlock3Stopped) {
            // 모든 블록이 멈췄을 때 결과를 확인하고 상금 지급
            // 결과 확인 로직...
        }
    }



    public Blocks(JavaPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
        // ... 초기화 코드 ...
    }

    // 각 블록 변경을 시작하는 메소드
    public void startChangingBlock1(int x, int y, int z, double[] probabilities) {
        if (task1 == null || task1.isCancelled()) {
            task1 = changeBlockWithProbability(x, y, z, probabilities);
        }

    }

    public void startChangingBlock2(int x, int y, int z, double[] probabilities) {
        if (task2 == null || task2.isCancelled()) {
            task2 = changeBlockWithProbability(x, y, z, probabilities);
        }

    }
    public void startChangingBlock3(int x, int y, int z, double[] probabilities) {
        if (task3 == null || task3.isCancelled()) {
            task3 = changeBlockWithProbability(x, y, z, probabilities);
        }

    }

    // 각 블록 변경을 멈추는 메소드
    public void stopChangingBlock1() {
        if (task1 != null && !task1.isCancelled()) {
            task1.cancel();
            task1 = null; // 태스크를 null로 설정하여 참조를 제거
        }
        block1Stopped = true; // 블록1이 멈춤 상태로 설정
        processGameResult();
    }

    public void stopChangingBlock2() {
        if (task2 != null && !task2.isCancelled()) {
            task2.cancel();
            task2 = null; // 태스크를 null로 설정하여 참조를 제거
        }
        block2Stopped = true; // 블록1이 멈춤 상태로 설정
        processGameResult();
    }

    public void stopChangingBlock3() {
        if (task3 != null && !task3.isCancelled()) {
            task3.cancel();
            task3 = null; // 태스크를 null로 설정하여 참조를 제거
        }
        block3Stopped = true; // 블록1이 멈춤 상태로 설정
        processGameResult();
    }



    // 확률에 따라 블록을 변경하는 메소드
    public BukkitTask changeBlockWithProbability(int x, int y, int z, double[] probabilities) {
        Material[] blocks = {
                Material.DIRT, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK
        };
        Random random = new Random();

        // 이 메소드에서 생성한 태스크를 반환합니다.
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Block block = world.getBlockAt(x, y, z);
                Material newBlockType;

                do {
                    // 확률에 따라 블록을 선택
                    double rand = random.nextDouble();
                    double cumulativeProbability = 0.0;
                    newBlockType = Material.AIR; // 초기 값

                    for (int i = 0; i < blocks.length; i++) {
                        cumulativeProbability += probabilities[i];
                        if (rand <= cumulativeProbability) {
                            newBlockType = blocks[i];
                            break;
                        }
                    }
                } while (newBlockType == lastBlockType); // 이전 블록과 다를 때까지 반복

                // 블록을 새로운 블록으로 변경
                block.setType(newBlockType);
                lastBlockType = newBlockType; // 마지막으로 변경된 블록 종류 업데이트

                // 블록 위치에서 소리 재생
                world.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.0f);
            }
        }, 0L, 3L); // 0L은 즉시 시작, 20L은 1초마다 반복
    }

    // 지정된 좌표의 블록 유형을 반환하는 메소드
    public Material getBlockType(int x, int y, int z) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Block block = world.getBlockAt(x, y, z);
            return block.getType(); // 블록의 유형 반환
        }
        return null; // 월드가 없거나 다른 문제가 발생한 경우
    }


    // 각 블록의 상태를 확인하고 결과를 처리하는 메소드
    public void checkBlocksStopped() {
        if (block1Stopped && block2Stopped && block3Stopped) {
            processGameResult();
            resetBlockStates();
        }
    }
    // 블록의 상태를 초기화하는 메소드
    private void resetBlockStates() {
        block1Stopped = false;
        block2Stopped = false;
        block3Stopped = false;
        setGameRunning(false);
    }
    // 각 블록 타입에 따라 상금을 계산하는 메소드
    private double calculatePrize(Material blockType, double betAmount) {
        double prizeMultiplier = 0.0;
        switch (blockType) {
            case DIRT:
                prizeMultiplier = prizeMultiplierForDirt;
                break;
            case DIAMOND_BLOCK:
                prizeMultiplier = prizeMultiplierForDiamond;
                break;
            case EMERALD_BLOCK:
                prizeMultiplier = prizeMultiplierForDiamond;
                break;
            case IRON_BLOCK:
                prizeMultiplier = prizeMultiplierForDiamond;
                break;
            case GOLD_BLOCK:
                prizeMultiplier = prizeMultiplierForDiamond;
                break;
        }
        return betAmount * prizeMultiplier;
    }
    // 게임 결과를 처리하고 상금을 지급하는 메소드
    public void processGameResult() {
        // 모든 블록이 멈췄는지 확인
        if (block1Stopped && block2Stopped && block3Stopped) {
            Material type1 = getBlockType(block1Coords[0], block1Coords[1], block1Coords[2]);
            Material type2 = getBlockType(block2Coords[0], block2Coords[1], block2Coords[2]);
            Material type3 = getBlockType(block3Coords[0], block3Coords[1], block3Coords[2]);

            // 플레이어 객체를 가져옵니다.
            Player player = getCurrentPlayer();
            if (player != null && type1 != null && type1 == type2 && type2 == type3) {
                // 모든 블록이 같은 타입일 경우 상금 배율을 적용하여 상금을 계산합니다.
                double prizeMultiplier = getPrizeMultiplier(type1);
                double prizeAmount = moneyManager.getBetAmount() * prizeMultiplier;

                // 상금 지급 로직
                moneyManager.depositPrize(player, prizeAmount); // 가정: depositPrize 메소드가 상금을 입금함
                player.sendMessage(ChatColor.GREEN + "Congratulations! You won " + prizeAmount + "!");

                // 게임 상태를 false로 설정하여 게임을 종료합니다.
                setGameRunning(false);
            } else if (player != null) {
                // 블록 타입이 모두 같지 않을 경우
                player.sendMessage(ChatColor.RED + "No match this time. Try again!");
                setGameRunning(false);
            }

            // 게임 상태와 블록 상태를 초기화합니다.
            resetBlockStates();
        }
    }
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player; // 플레이어 객체를 저장합니다.
    }
    // 현재 게임을 진행하는 플레이어 객체를 가져오는 메소드
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }
    // 주어진 블록 타입에 대한 상금 배율을 가져오는 메소드
    public double getPrizeMultiplier(Material type) {
        switch (type) {
            case DIRT: return prizeMultiplierForDirt;
            case DIAMOND_BLOCK: return prizeMultiplierForDiamond;
            case EMERALD_BLOCK: return prizeMultiplierForEmerald;
            case IRON_BLOCK: return prizeMultiplierForIron;
            case GOLD_BLOCK: return prizeMultiplierForGold;
            default: return 0; // 기본값
        }
    }



}
