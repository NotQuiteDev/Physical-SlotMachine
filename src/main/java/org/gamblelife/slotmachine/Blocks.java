package org.gamblelife.slotmachine;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.gamblelife.slotmachine.FireworkUtil.launchFirework;

public class Blocks {
    private Map<String, Map<Integer, Boolean>> blockStoppedMap = new HashMap<>();
    private Map<String, Map<Integer, BukkitTask>> taskMap = new HashMap<>();

    private Map<String, BukkitTask> taskMap1 = new HashMap<>(); // 첫 번째 블록 변경 작업을 관리하는 맵
    private Map<String, BukkitTask> taskMap2 = new HashMap<>(); // 두 번째 블록 변경 작업을 관리하는 맵
    private Map<String, BukkitTask> taskMap3 = new HashMap<>(); // 세 번째 블록 변경 작업을 관리하는 맵


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
    public double prizeMultiplierForDirt= 2.5;
    public double prizeMultiplierForDiamond=200;
    public double prizeMultiplierForEmerald=1777;
    public double prizeMultiplierForIron=15;
    public double prizeMultiplierForGold=30;
    private JavaPlugin plugin;

    private final String worldName = "city2";
    private BukkitTask task1, task2, task3;
    private Material lastBlockType = null; // 마지막으로 변경된 블록 종류를 저장
    private boolean isGameRunning = false;
    // 게임이 진행 중인지 확인하는 메소드
    private boolean isBlock1Stopped, isBlock2Stopped, isBlock3Stopped;
    private Player currentPlayer;

    private Map<String, Boolean> gameRunningMap = new HashMap<>();

    // 특정 슬롯머신의 게임 실행 상태를 확인하는 메소드
    public boolean isGameRunning(String machineKey) {
        // 기본값으로 false를 반환합니다. (게임이 실행 중이지 않다고 가정)
        return gameRunningMap.getOrDefault(machineKey, false);
    }

    // 특정 슬롯머신의 게임 실행 상태를 설정하는 메소드
    public void setGameRunning(String machineKey, boolean isRunning) {
        gameRunningMap.put(machineKey, isRunning);
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
    // 슬롯머신별 각 블록 변경 작업을 관리하는 Map
    private Map<String, Map<Integer, BukkitTask>> blockTaskMap = new HashMap<>();

    // 특정 슬롯머신의 특정 블록 변경을 시작하는 메소드
    public void startChangingBlock(String machineKey, int blockNumber, ConfigurationSection machineConfig, double[] probabilities) {
        // 첫 번째 블록 위치 가져오기
        final int[] slotLocation = {
                machineConfig.getInt("slot_location.x"),
                machineConfig.getInt("slot_location.y"),
                machineConfig.getInt("slot_location.z")
        };

        // 방향 가져오기
        final String direction = machineConfig.getString("direction");

        // 나머지 두 블록의 위치 계산 (첫 번째 블록이 아닐 경우)
        final int[] finalBlockLocation;
        if (blockNumber > 1) {
            int[][] otherBlockLocations = calculateOtherBlockLocations(slotLocation, direction);
            finalBlockLocation = otherBlockLocations[blockNumber - 2]; // 블록 번호에 따라 위치 조정
        } else {
            finalBlockLocation = slotLocation; // 첫 번째 블록 위치
        }

        Material[] blocks = {
                Material.DIRT, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK
        };

        Random random = new Random();

        // 태스크 식별자 생성 (슬롯머신 키와 블록 번호를 결합)
        String taskIdentifier = machineKey + ":" + blockNumber;

        // 이전에 실행 중인 동일한 태스크가 있는지 확인하고 취소
        BukkitTask existingTask = blockChangeTasks.get(taskIdentifier);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // 블록 변경 작업을 수행하는 태스크 생성 및 시작
        BukkitTask newTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld(machineConfig.getString("world"));
            if (world != null) {
                Block block = world.getBlockAt(finalBlockLocation[0], finalBlockLocation[1], finalBlockLocation[2]);
                Material newBlockType;

                do {
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
                } while (newBlockType == block.getType()); // 이전 블록과 다를 때까지 반복

                // 블록을 새로운 블록으로 변경
                block.setType(newBlockType);

                // 블록 위치에서 소리 재생
                world.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.0f);
            }
        }, 0L, 20L); // 0L은 즉시 시작, 20L은 1초마다 반복

        // 새 태스크를 맵에 저장
        blockChangeTasks.put(taskIdentifier, newTask);
    }

    public boolean isBlockChanging(String machineKey, int blockNumber) {
        String taskIdentifier = machineKey + ":" + blockNumber;
        BukkitTask task = blockChangeTasks.get(taskIdentifier);
        return task != null && !task.isCancelled();
    }

    public void cancelBlockChangeTask(String machineKey, int blockNumber) {
        // 태스크 식별자 생성
        String taskIdentifier = machineKey + ":" + blockNumber;

        // 해당 태스크가 있으면 취소하고 맵에서 제거
        BukkitTask task = blockChangeTasks.get(taskIdentifier);
        if (task != null) {
            task.cancel();
            blockChangeTasks.remove(taskIdentifier);
        }
    }


    private Map<String, BukkitTask> blockChangeTasks = new HashMap<>();
    // 각 블록 변경을 멈추는 메소드
    public void stopChangingBlock(String machineKey, int blockNumber) {
        // 태스크 식별자 생성
        String taskIdentifier = machineKey + ":" + blockNumber;

        // 해당 태스크가 있으면 취소하고 맵에서 제거
        BukkitTask task = blockChangeTasks.get(taskIdentifier);
        if (task != null && !task.isCancelled()) {
            task.cancel(); // 태스크를 취소하여 블록 변경을 멈춥니다.
            Bukkit.getLogger().info("[디버그] " + taskIdentifier + " 태스크가 취소되었습니다.");

            blockChangeTasks.remove(taskIdentifier); // 취소된 태스크를 맵에서 제거

            // 해당 블록의 멈춤 상태를 true로 설정합니다.
            Map<Integer, Boolean> stoppedMap = blockStoppedMap.getOrDefault(machineKey, new HashMap<>());
            stoppedMap.put(blockNumber, true);
            blockStoppedMap.put(machineKey, stoppedMap);

            // 필요한 경우 게임 결과 처리 로직을 추가할 수 있습니다.
            Bukkit.getLogger().info("[디버그] " + machineKey + "의 블록 " + blockNumber + "이(가) 멈췄습니다.");
        }
    }


    // 각 블록이 멈췄는지 여부를 확인하는 메소드
    public boolean isBlockStopped(String machineKey, int blockNumber) {
        Map<Integer, Boolean> stoppedMap = blockStoppedMap.getOrDefault(machineKey, new HashMap<>());
        return stoppedMap.getOrDefault(blockNumber, false);
    }

    // 각 슬롯머신 및 블록의 상태를 초기화하는 메소드 (게임 재시작 시 사용)
    public void resetMachineState(String machineKey) {
        Map<Integer, Boolean> stoppedMap = new HashMap<>();
        stoppedMap.put(1, false);
        stoppedMap.put(2, false);
        stoppedMap.put(3, false);
        blockStoppedMap.put(machineKey, stoppedMap);

        Map<Integer, BukkitTask> machineTasks = taskMap.get(machineKey);
        if (machineTasks != null) {
            machineTasks.forEach((blockNumber, task) -> {
                if (task != null && !task.isCancelled()) {
                    task.cancel();  // 모든 태스크를 취소합니다.
                }
            });
            machineTasks.clear();
        }
    }


    // 확률에 따라 블록을 변경하는 메소드
    public void changeBlockWithProbability(String machineKey, int blockNumber, int[] location, double[] probabilities) {
        int x = location[0];
        int y = location[1];
        int z = location[2];
        Material[] blocks = {
                Material.DIRT, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK
        };
        Random random = new Random();

        // 이 메소드에서 생성한 태스크를 반환합니다.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
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
    public void processGameResult(String machineKey) {
        // 슬롯머신의 각 블록 위치를 가져옵니다.
        ConfigurationSection machineConfig = plugin.getConfig().getConfigurationSection("slotMachines." + machineKey);
        if (machineConfig == null) return; // 슬롯머신 설정이 없으면 리턴

        int[][] blockLocations = getBlockLocations(machineConfig);
        World world = Bukkit.getWorld(machineConfig.getString("world"));
        Material blockType = null;
        boolean allMatch = true;

        // 각 블록의 타입을 확인합니다.
        for (int[] location : blockLocations) {
            Block block = world.getBlockAt(location[0], location[1], location[2]);
            if (blockType == null) {
                blockType = block.getType();
            } else if (block.getType() != blockType) {
                allMatch = false;
                break;
            }
        }

        // 슬롯머신을 사용한 플레이어 객체를 가져옵니다.
        Player player = getCurrentPlayer(machineKey);
        if (player == null) return; // 플레이어가 없으면 리턴

        if (allMatch && blockType != null) {
            // 모든 블록이 같은 타입일 경우 상금 배율을 적용하여 상금을 계산합니다.
            double prizeMultiplier = getPrizeMultiplier(blockType);
            double prizeAmount = moneyManager.getCurrentBetAmountForMachine(machineKey) * prizeMultiplier;

            // 상금 지급 로직
            moneyManager.depositPrize(player, prizeAmount);

            // 당첨되었을 때 폭죽 발사
            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(Color.RED)
                    .withFade(Color.ORANGE)
                    .with(FireworkEffect.Type.BALL)
                    .trail(true)
                    .flicker(true)
                    .build();
            launchFirework(player.getLocation(), effect, 1);

            player.sendMessage(ChatColor.GREEN + "축하합니다! " + prizeAmount + "원을 당첨되셨습니다!");

        } else {
            // 블록 타입이 모두 같지 않을 경우
            player.sendMessage(ChatColor.RED + "아쉽게도 맞추지 못했습니다. 다시 시도해보세요!");
        }

        // 게임 상태와 블록 상태를 초기화합니다.
        setGameRunning(machineKey, false);
        resetMachineState(machineKey);
    }
    // 슬롯머신별 현재 플레이어를 저장하는 Map
    private Map<String, Player> currentPlayerMap = new HashMap<>();

    // 특정 슬롯머신에 대한 현재 플레이어를 설정하는 메소드
    public void setCurrentPlayer(String machineKey, Player player) {
        currentPlayerMap.put(machineKey, player);
    }

    // 특정 슬롯머신의 현재 플레이어를 가져오는 메소드
    public Player getCurrentPlayer(String machineKey) {
        return currentPlayerMap.get(machineKey);
    }
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

    public double[] loadProbabilities() {
        ConfigurationSection probabilitiesSection = plugin.getConfig().getConfigurationSection("probabilities");
        if (probabilitiesSection == null) return new double[]{0.4, 0.1, 0.05, 0.25, 0.2}; // 기본 확률 값

        double[] probabilities = new double[5];
        probabilities[0] = probabilitiesSection.getDouble("a");
        probabilities[1] = probabilitiesSection.getDouble("b");
        probabilities[2] = probabilitiesSection.getDouble("c");
        probabilities[3] = probabilitiesSection.getDouble("d");
        probabilities[4] = probabilitiesSection.getDouble("e");
        return probabilities;
    }


    // 나머지 블록 위치 계산 메소드
    public int[][] calculateOtherBlockLocations(int[] slotLocation, String direction) {
        int[][] locations = new int[2][3]; // 두 번째 및 세 번째 블록 위치 저장
        // slotLocation 배열의 구조: {x, y, z}
        int x = slotLocation[0];
        int y = slotLocation[1];
        int z = slotLocation[2];

        switch (direction) {
            case "EAST":
                locations[0] = new int[]{x, y, z - 1};
                locations[1] = new int[]{x, y, z - 2};
                break;
            case "WEST":
                locations[0] = new int[]{x, y, z + 1};
                locations[1] = new int[]{x, y, z + 2};
                break;
            case "NORTH":
                locations[0] = new int[]{x - 1, y, z};
                locations[1] = new int[]{x - 2, y, z};
                break;
            case "SOUTH":
                locations[0] = new int[]{x + 1, y, z};
                locations[1] = new int[]{x + 2, y, z};
                break;
        }

        return locations;
    }
    public int[][] getBlockLocations(ConfigurationSection machineConfig) {
        // 첫 번째 블록 위치를 가져옵니다.
        int[] slotLocation = new int[]{
                machineConfig.getInt("slot_location.x"),
                machineConfig.getInt("slot_location.y"),
                machineConfig.getInt("slot_location.z")
        };

        // 슬롯 머신이 바라보는 방향을 가져옵니다.
        String direction = machineConfig.getString("direction");

        // 나머지 두 블록의 위치를 계산합니다.
        int[][] otherBlockLocations = calculateOtherBlockLocations(slotLocation, direction);

        // 모든 블록의 위치를 저장할 배열을 생성합니다.
        int[][] blockLocations = new int[3][3];

        // 첫 번째 블록 위치는 이미 알고 있으므로 배열에 추가합니다.
        blockLocations[0] = slotLocation;

        // 나머지 두 블록의 위치를 추가합니다.
        blockLocations[1] = otherBlockLocations[0];
        blockLocations[2] = otherBlockLocations[1];

        return blockLocations;
    }


}
