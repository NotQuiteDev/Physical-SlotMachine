package org.gamblelife.slotmachine;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static org.gamblelife.slotmachine.FireworkUtil.launchFirework;

public class Blocks {
    private List<Material> blockMaterials = new ArrayList<>();
    private List<Double> blockProbabilities = new ArrayList<>();
    private Map<String, Map<Integer, Boolean>> blockStoppedMap = new HashMap<>();
    private Map<String, Map<Integer, BukkitTask>> taskMap = new HashMap<>();
    private MoneyManager moneyManager;
    // 각 블록 타입별 상금 배율
    public double prizeMultiplierForDirt= 2.5;
    public double prizeMultiplierForDiamond=200;
    public double prizeMultiplierForEmerald=1777;
    public double prizeMultiplierForIron=15;
    public double prizeMultiplierForGold=30;
    private JavaPlugin plugin;


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





    public Blocks(JavaPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
        // ... 초기화 코드 ...
    }


    // 특정 슬롯머신의 특정 블록 변경을 시작하는 메소드
    public void startChangingBlock(String machineKey, int blockNumber, ConfigurationSection machineConfig) {
        final int[] slotLocation = {
                machineConfig.getInt("slot_location.x"),
                machineConfig.getInt("slot_location.y"),
                machineConfig.getInt("slot_location.z")
        };

        final String direction = machineConfig.getString("direction");

        final int[] finalBlockLocation = blockNumber > 1
                ? calculateOtherBlockLocations(slotLocation, direction)[blockNumber - 2]
                : slotLocation;

        // 블록 유형과 확률 로드
        loadBlockProbabilities(); // Config에서 블록 유형과 확률 로드하는 메소드 호출

        Random random = new Random();
        String taskIdentifier = machineKey + ":" + blockNumber;
        BukkitTask existingTask = blockChangeTasks.get(taskIdentifier);
        if (existingTask != null) existingTask.cancel();

        BukkitTask newTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld(machineConfig.getString("world"));
            if (world != null) {
                Block block = world.getBlockAt(finalBlockLocation[0], finalBlockLocation[1], finalBlockLocation[2]);
                Material newBlockType;

                do {
                    double rand = random.nextDouble();
                    double cumulativeProbability = 0.0;
                    newBlockType = Material.AIR;

                    for (int i = 0; i < blockMaterials.size(); i++) {
                        cumulativeProbability += blockProbabilities.get(i);
                        if (rand <= cumulativeProbability) {
                            newBlockType = blockMaterials.get(i);
                            break;
                        }
                    }
                } while (newBlockType == block.getType());

                block.setType(newBlockType);
                double slotMachineVolume = plugin.getConfig().getDouble("soundSettings.slotMachineVolume", 0.3);  // 기본값은 0.3
                world.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, (float)slotMachineVolume, 1.0f);
            }
        }, 0L, 2L);

        blockChangeTasks.put(taskIdentifier, newTask);
    }

    public boolean isBlockChanging(String machineKey, int blockNumber) {
        String taskIdentifier = machineKey + ":" + blockNumber;
        BukkitTask task = blockChangeTasks.get(taskIdentifier);
        return task != null && !task.isCancelled();
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
            //Bukkit.getLogger().info("[디버그] " + taskIdentifier + " 태스크가 취소되었습니다.");

            blockChangeTasks.remove(taskIdentifier); // 취소된 태스크를 맵에서 제거

            // 해당 블록의 멈춤 상태를 true로 설정합니다.
            Map<Integer, Boolean> stoppedMap = blockStoppedMap.getOrDefault(machineKey, new HashMap<>());
            stoppedMap.put(blockNumber, true);
            blockStoppedMap.put(machineKey, stoppedMap);

            // 필요한 경우 게임 결과 처리 로직을 추가할 수 있습니다.
            //Bukkit.getLogger().info("[디버그] " + machineKey + "의 블록 " + blockNumber + "이(가) 멈췄습니다.");
        }
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

    public void loadBlockProbabilities() {
        ConfigurationSection blockProbs = plugin.getConfig().getConfigurationSection("blockProbabilities");
        if (blockProbs == null) {
            plugin.getLogger().warning("Block probabilities section is missing in the config.");
            return;
        }

        blockMaterials.clear();
        blockProbabilities.clear();

        for (String key : blockProbs.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase());
            if (material != null) {
                blockMaterials.add(material);
                blockProbabilities.add(blockProbs.getDouble(key));
            } else {
                plugin.getLogger().warning("Invalid material in config: " + key);
            }
        }
    }
    public Material getRandomBlock() {
        double totalProbability = blockProbabilities.stream().mapToDouble(Double::doubleValue).sum();
        double random = Math.random() * totalProbability;
        double current = 0;
        for (int i = 0; i < blockProbabilities.size(); i++) {
            current += blockProbabilities.get(i);
            if (random <= current) {
                return blockMaterials.get(i);
            }
        }
        return blockMaterials.get(blockMaterials.size() - 1); // Fallback
    }


}
