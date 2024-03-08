package org.gamblelife.slotmachine;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoneyManager {
    private Economy economy;
    private List<Integer> betAmounts; // 판돈 순환 배열
    private Map<String, Integer> machineBetIndexMap = new HashMap<>(); // 각 슬롯머신별 판돈 인덱스를 저장하는 Map 초기화
    private int currentBetIndex = 0;

    public MoneyManager(Economy economy) {
        if (economy == null) {
            System.out.println("Economy instance is null!");
        }
        this.economy = economy;
    }

    public MoneyManager(Economy economy, List<Integer> betAmounts) {
        this.economy = economy;
        this.betAmounts = betAmounts; // config에서 로드한 판돈 배열 초기화
        // machineBetIndexMap은 위에서 이미 초기화되었습니다.
    }

    // 현재 판돈 가져오기
    public int getBetAmount() {
        if (betAmounts == null || betAmounts.isEmpty()) {
            return 0; // betAmounts가 null이거나 비어있는 경우 0 반환
        }
        return betAmounts.get(currentBetIndex);
    }

    // 판돈 순환적으로 변경
    public void cycleBetAmount() {
        if (betAmounts == null || betAmounts.isEmpty()) {
            return; // betAmounts가 null이거나 비어있는 경우 메소드 종료
        }
        // 다음 판돈으로 인덱스 업데이트
        currentBetIndex = (currentBetIndex + 1) % betAmounts.size();
    }

    // 슬롯머신별 판돈 인덱스를 가져오거나 기본값(0)을 반환
    private int getMachineBetIndex(String machineId) {
        return machineBetIndexMap.getOrDefault(machineId, 0);
    }

    // 슬롯머신별 판돈 인덱스를 업데이트
    private void updateMachineBetIndex(String machineId, int newIndex) {
        machineBetIndexMap.put(machineId, newIndex);
    }

    // 특정 슬롯머신의 판돈을 순환
    public void cycleBetAmountForMachine(String machineId) {
        if (betAmounts == null || betAmounts.isEmpty()) {
            return; // betAmounts가 null이거나 비어있는 경우 메소드 종료
        }
        int currentIndex = getMachineBetIndex(machineId);
        int newIndex = (currentIndex + 1) % betAmounts.size();
        updateMachineBetIndex(machineId, newIndex);
    }

    // 특정 슬롯머신의 현재 판돈을 가져옴
    public int getCurrentBetAmountForMachine(String machineId) {
        if (betAmounts == null || betAmounts.isEmpty()) {
            return 0; // betAmounts가 null이거나 비어있는 경우 0 반환
        }
        int index = getMachineBetIndex(machineId);
        return betAmounts.get(index);
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