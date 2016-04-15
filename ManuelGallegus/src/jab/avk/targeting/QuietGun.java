package jab.avk.targeting;

import jab.avk.Module;
import jab.avk.Targeting;

public class QuietGun extends Targeting {
	
	public QuietGun(Module bot) {
		super(bot);
	}

	public void target(){
		bot.setTurnGunRightRadians(0.0001);
	}
	
}