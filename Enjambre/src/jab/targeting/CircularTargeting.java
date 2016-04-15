package jab.targeting;

import java.awt.geom.Point2D;

import robocode.util.Utils;
import jab.module.Module;
import jab.module.Targeting;

public class CircularTargeting extends Targeting {

	
	public CircularTargeting(Module bot) {
		super(bot);
	}

	public void target() {
		double myX = bot.getX();
		double myY = bot.getY();
		double enemyX = bot.enemy.x;
		double enemyY = bot.enemy.y;
		double enemyHeading = bot.enemy.headingRadians;
		double oldEnemyHeading = bot.enemy.previousHeadingRadians;
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		enemyHeadingChange = enemyHeadingChange / bot.enemy.timeSinceLastScan;
		double enemyVelocity = bot.enemy.velocity;
		oldEnemyHeading = enemyHeading;

		double deltaTime = 0;
		double battleFieldHeight = bot.getBattleFieldHeight(),battleFieldWidth = bot.getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bot.bulletPower) < 
		      Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			enemyHeading += enemyHeadingChange;
			if(	predictedX < 18.0 
				|| predictedY < 18.0
				|| predictedX > battleFieldWidth - 18.0
				|| predictedY > battleFieldHeight - 18.0){

				predictedX = Math.min(Math.max(18.0, predictedX), 
				    battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), 
				    battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - bot.getX(), predictedY - bot.getY()));
		bot.setTurnGunRightRadians(Utils.normalRelativeAngle(theta - bot.getGunHeadingRadians()));
	}
	
}
