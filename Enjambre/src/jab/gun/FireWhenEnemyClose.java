package jab.gun;

import jab.module.Gun;
import jab.module.Module;

public class FireWhenEnemyClose extends Gun {
	
	
	public static final int CONSIDER_CLOSE = 200;

	public FireWhenEnemyClose(Module bot) {
		super(bot);
	}

	public void fire() {
		if (bot.enemy.distance <= CONSIDER_CLOSE) {
			bot.bulletPower = 3;
			bot.setFire(bot.bulletPower);
		} else {
			bot.bulletPower = 0;
		}
	}

}
