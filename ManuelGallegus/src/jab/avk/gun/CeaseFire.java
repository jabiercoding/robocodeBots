package jab.avk.gun;

import jab.avk.Gun;
import jab.avk.Module;

public class CeaseFire extends Gun {
	public CeaseFire(Module bot) {
		super(bot);
	}

	public void fire(){
		bot.bulletPower= 0;
	}
}
