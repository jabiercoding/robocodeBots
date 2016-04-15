package jab.movement;

import jab.module.Module;
import jab.module.Movement;
import robocode.Event;
import robocode.HitWallEvent;
import java.util.Random;

public class BlindFighterMovement extends Movement {

	public BlindFighterMovement(Module bot) {
		super(bot);
	}
	
	double x=0;
	double y=0;
	double heading;
	boolean first=true;
	Random generator = new Random();
	
	public void move() {
		if (first){
			x=bot.getX()-.5*bot.getBattleFieldWidth();
			y=bot.getY()-.5*bot.getBattleFieldHeight();
			heading=Math.rint(2*Math.atan2(x,y)/Math.PI);
			heading=(Math.PI/2)*heading;
			bot.turnRightRadians(angle_180(heading-bot.getHeadingRadians()));
			System.out.println("Angle Right: "+Math.toDegrees(angle_180(heading-bot.getHeadingRadians())));
			System.out.println(bot.getTurnRemaining() + "   " + bot.getTurnRemainingRadians());
			first=false;
		}

		if ((bot.getTime()%20) < 5)
			bot.setAhead(generator.nextInt(10));
		else
			bot.setAhead(Math.max(50,generator.nextInt(100)));
	}

	@Override
	public void listen(Event e) {
		if (e instanceof HitWallEvent){
			bot.turnRight(90);
		}
	}
	
	public double angle_180(double ang)
	{
		return Math.atan2(Math.sin(ang), Math.cos(ang));
	}
}