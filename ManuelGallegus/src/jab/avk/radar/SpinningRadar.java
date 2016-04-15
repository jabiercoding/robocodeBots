package jab.avk.radar;

import jab.avk.Module;
import jab.avk.Radar;

public class SpinningRadar extends Radar {
	
	public SpinningRadar(Module bot) {
		super(bot);
	}

	public void scan(){
		bot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

}
