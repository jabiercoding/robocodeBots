package jab.gun;

import jab.module.Gun;
import jab.module.Module;


public class FireWhenClose extends Gun {
	public FireWhenClose(Module bot) {
		super(bot);
	}

	public void fire(){
		if (bot.enemy.distance<=200){
			bot.bulletPower= 3;
			bot.setFire(bot.bulletPower);
		}
		else bot.bulletPower= 0;
	}
	
}
