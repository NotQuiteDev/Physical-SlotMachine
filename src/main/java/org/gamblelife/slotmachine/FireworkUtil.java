package org.gamblelife.slotmachine;


import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkUtil {

    // 이 메소드는 이제 정적 메소드로 사용할 수 있습니다.
    public static void launchFirework(Location location, FireworkEffect effect, int power) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(power);

        firework.setFireworkMeta(fireworkMeta);
    }
}