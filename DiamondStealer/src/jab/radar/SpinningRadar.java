package jab.radar;

import robocode.util.Utils;
import jab.module.Module;
import jab.module.Radar;

public class SpinningRadar extends Radar {
	
	public SpinningRadar(Module bot) {
		super(bot);
	}

	int direction=0;
	
	public void scan(){
		if (direction==0)
			direction= sign(calculateRelativeAngleToCenter(bot.getRadarHeadingRadians()));
		bot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY * direction);
	}

	private static int sign(double v) {
		return v > 0 ? 1 : -1;
	}

	private double calculateRelativeAngleToCenter(double heading) {
		return Utils.normalRelativeAngle(Math.atan2(bot.getBattleFieldWidth()/2 - bot.getX(), bot.getBattleFieldHeight()/2
				- bot.getY())
				- heading);
	}
	
}
