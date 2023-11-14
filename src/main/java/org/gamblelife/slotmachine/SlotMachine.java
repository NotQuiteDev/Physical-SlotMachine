package org.gamblelife.slotmachine;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlotMachine extends JavaPlugin {
    private MoneyManager moneyManager;
    private Economy econ = null;
    @Override
    public void onEnable() {
        moneyManager = new MoneyManager(econ);

        getLogger().info("슬롯머신 플러그인 활성화됨.");
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // MoneyManager 인스턴스를 생성합니다.
        moneyManager = new MoneyManager(econ);

        // Blocks 클래스 인스턴스화
        Blocks blocks = new Blocks(this, moneyManager);

        // ButtonListener 클래스 인스턴스화 및 리스너 등록
        ButtonListener buttonListener = new ButtonListener(this, blocks, moneyManager); // ButtonListener 인스턴스 생성
        getServer().getPluginManager().registerEvents(buttonListener, this);
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
