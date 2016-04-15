package jab.targeting;

import jab.module.Module;
import jab.module.Targeting;

public class QuietGun extends Targeting {
	
	public QuietGun(Module bot) {
		super(bot);
	}

	public void target(){
		bot.setTurnGunRightRadians(0.0001);
	}
	
}