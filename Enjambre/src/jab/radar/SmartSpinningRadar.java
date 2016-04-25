package jab.radar;

import java.util.Enumeration;

import robocode.Event;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import jab.module.BotInfo;
import jab.module.Module;
import jab.module.Radar;

public class SmartSpinningRadar extends Radar {

	public SmartSpinningRadar(Module bot) {
		super(bot);
	}

	int clockwise = 0;

	public void scan() {

		// Clockwise initialization
		if (clockwise == 0) {
			clockwise = sign(calculateRelativeAngleToCenter(bot.getRadarHeadingRadians()));
		}

		bot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY * clockwise);

	}

	public void listen(Event e) {
		if (e instanceof ScannedRobotEvent) {
			Enumeration<BotInfo> enemies = bot.botsInfo.elements();
			boolean nextFound = false;
			while (enemies.hasMoreElements()) {
				BotInfo botInfo = enemies.nextElement();
				if (!botInfo.name.equals(((ScannedRobotEvent) e).getName()) && !bot.isTeammate(botInfo.name)) {
					double absoluteBearing = bot.getHeadingRadians() + botInfo.bearingRadians;
					double angle = Utils.normalRelativeAngle(absoluteBearing - bot.getRadarHeadingRadians());
					if (clockwise == 1 && angle >= 0) {
						nextFound = true;
						break;
					} else if (clockwise == -1 && angle <= 0) {
						nextFound = true;
						break;
					}
				}
			}
			if (!nextFound) {
				if (clockwise == 1) {
					clockwise = -1;
				} else {
					clockwise = 1;
				}
			}
		}
	}

	private static int sign(double v) {
		if (v > 0) {
			return 1;
		} else {
			return -1;
		}
	}

	private double calculateRelativeAngleToCenter(double heading) {
		return Utils.normalRelativeAngle(Math.atan2(bot.getBattleFieldWidth() / 2 - bot.getX(),
				bot.getBattleFieldHeight() / 2 - bot.getY())
				- heading);
	}

}
