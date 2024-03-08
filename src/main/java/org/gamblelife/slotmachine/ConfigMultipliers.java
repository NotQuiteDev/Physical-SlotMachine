package org.gamblelife.slotmachine;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigMultipliers {

    private Logger logger;
    private JavaPlugin plugin;
    private FileConfiguration config;
    private Map<Material, Double> doublesMultipliers = new HashMap<>();
    private Map<Material, Double> triplesMultipliers = new HashMap<>();
    private Map<String, Map<Material, Integer>> specialCombinations = new HashMap<>();
    private Map<String, Double> specialMultipliers = new HashMap<>();


    public Double getTripleMultiplier(Material material) {
        return triplesMultipliers.get(material);
    }
    public ConfigMultipliers(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig(); // 플러그인의 config.yml 파일로부터 설정을 로드
        this.logger = plugin.getLogger(); // Logger 인스턴스를 초기화
        loadConfig();
    }

    // 설정을 리로드하는 메소드
    public void reloadConfig() {
        plugin.reloadConfig();  // 플러그인의 config.yml 파일을 리로드
        this.config = plugin.getConfig(); // 리로드된 설정 파일을 다시 가져옴
        loadConfig();  // 설정값을 다시 로드
    }

    private void loadConfig() {
        logger.info("ConfigMultipliers: Loading configuration...");

        // 'doubles' 섹션 로드
        ConfigurationSection doublesSection = config.getConfigurationSection("rewardMultipliers.doubles");
        if (doublesSection != null) {
            logger.info("Loading 'doubles' multipliers...");
            for (String key : doublesSection.getKeys(false)) {
                Material material = Material.getMaterial(key);
                if (material != null) {
                    double multiplier = doublesSection.getDouble(key);
                    doublesMultipliers.put(material, multiplier);
                    logger.info(" - " + material.name() + ": " + multiplier);
                }
            }
        }

        // 'triples' 섹션 로드
        ConfigurationSection triplesSection = config.getConfigurationSection("rewardMultipliers.triples");
        if (triplesSection != null) {
            logger.info("Loading 'triples' multipliers...");
            for (String key : triplesSection.getKeys(false)) {
                Material material = Material.getMaterial(key);
                if (material != null) {
                    double multiplier = triplesSection.getDouble(key);
                    triplesMultipliers.put(material, multiplier);
                    logger.info(" - " + material.name() + ": " + multiplier);
                }
            }
        }

        // 'special' 섹션 로드
        ConfigurationSection specialSection = config.getConfigurationSection("rewardMultipliers.special");
        if (specialSection != null) {
            logger.info("Loading 'special' combinations...");
            for (String key : specialSection.getKeys(false)) {
                ConfigurationSection combinationSection = specialSection.getConfigurationSection(key);
                if (combinationSection != null) {
                    logger.info(" - Combination: " + key);
                    Map<Material, Integer> combination = new HashMap<>();
                    for (String materialKey : combinationSection.getKeys(false)) {
                        if (!materialKey.equals("multiplier")) {
                            Material material = Material.getMaterial(materialKey);
                            if (material != null) {
                                int amount = combinationSection.getInt(materialKey);
                                combination.put(material, amount);
                                logger.info("   - " + material.name() + ": " + amount);
                            }
                        }
                    }
                    double multiplier = combinationSection.getDouble("multiplier");
                    specialCombinations.put(key, combination);
                    specialMultipliers.put(key, multiplier);
                    logger.info("   - Multiplier: " + multiplier);
                }
            }
        }

        logger.info("ConfigMultipliers: Configuration loaded successfully.");
    }

    // 여기에 멀티플라이어 값을 가져오는 메소드들을 추가할 수 있어
}