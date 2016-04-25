package jab.gun;

import jab.module.BotInfo;
import jab.module.Gun;
import jab.module.Module;

public class FireWhenClosestClose extends Gun {
	public FireWhenClosestClose(Module bot) {
		super(bot);
	}

	public void fire() {
		BotInfo closest = bot.getClosestEnemy();
		if (closest != null && closest.distance <= 200) {
			bot.bulletPower = 3;
			bot.setFire(bot.bulletPower);
		} else {
			bot.bulletPower = 0;
		}
	}

}
