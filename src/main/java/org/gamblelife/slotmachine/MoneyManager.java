package org.gamblelife.slotmachine;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class MoneyManager {
    private Economy economy;

    public MoneyManager(Economy economy) {
        this.economy = economy;
    }
    private int betAmount = 1000; // 기본 판돈 설정

    public void setBetAmount(int amount) {
        this.betAmount = amount;
        // 판돈 설정 로직...
    }

    public int getBetAmount() {
        return this.betAmount;
    }

    // 플레이어의 잔액을 확인하고, 판돈을 차감하는 메소드
    public boolean withdrawBet(Player player, double betAmount) {
        if (economy.has(player, betAmount)) {
            economy.withdrawPlayer(player, betAmount);
            return true; // 충분한 잔액이 있어서 차감에 성공했습니다.
        }
        return false; // 잔액이 부족합니다.
    }
    public void depositPrize(Player player, double amount) {
        economy.depositPlayer(player, amount);
        // 실제 게임에서는 경제 시스템과 통합되어야 하며,
        // 여기서는 Economy API를 사용하여 플레이어에게 돈을 입금합니다.
    }

}
