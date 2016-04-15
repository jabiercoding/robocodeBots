package jab.radar;

import jab.module.Module;
import jab.module.Radar;
import robocode.Event;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * Credits
 * RadarBot by PEZ
 **/
public class WideLock extends Radar {
	
	public WideLock(Module bot) {
		super(bot);
	}

	private int timeSinceLastScan = 10;
	private double enemyAbsoluteBearing;
	
	public void scan(){
		timeSinceLastScan++;
		double radarOffset = Double.NEGATIVE_INFINITY;
		if (timeSinceLastScan < 3) {
			radarOffset = Utils
					.normalRelativeAngle(bot.getRadarHeadingRadians()
							- enemyAbsoluteBearing);
			radarOffset += Math.signum(radarOffset) * 0.2;
		}
		bot.setTurnRadarLeftRadians(radarOffset);
	}
	
	public void listen(Event e){
		if (e instanceof ScannedRobotEvent){
			if (!bot.isTeammate(((ScannedRobotEvent)e).getName())){
				enemyAbsoluteBearing = bot.getHeadingRadians() + ((ScannedRobotEvent)e).getBearingRadians();
				timeSinceLastScan = 0;
			}
		}
	}
	
}
