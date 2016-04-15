package jab.micro;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * Sanguijuela. A simple micro RamBot
 * 
 * @author jab
 */

public class Sanguijuela extends AdvancedRobot {

	static Rectangle2D.Double territory;

	// Host information
	static Point2D.Double enemy;

	int previousDirection;
	static boolean changingDirection;
	boolean boost;

	public void run() {

		territory = new Rectangle2D.Double(17, 17, getBattleFieldWidth() - 34,
				this.getBattleFieldHeight() - 34);
		enemy = new Point2D.Double(getBattleFieldWidth() / 2,
				getBattleFieldHeight() / 2);

		// Adjustments
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Start looking around
		int direction = sign(calculateRelativeAngleToEnemy(getRadarHeadingRadians()));

		setTurnGunRightRadians(Double.POSITIVE_INFINITY * direction);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY * direction);

		do {
			double angle = calculateRelativeAngleToEnemy(getHeadingRadians());
			direction = 1;
			if (Math.abs(angle) > Math.PI / 2) {
				angle += (direction = -1) * Math.PI * sign(angle);
			}
			if (direction != previousDirection) {
				changingDirection = true;
			}
			if (changingDirection && Math.abs(getVelocity()) >= 2) {
				//Acceleration Trick
				setMaxVelocity(0.00001);
			} else {
				setMaxVelocity((Math.abs(getTurnRemaining()) < 45 ? 8
						: (boost ? 4 : 1))
						* getOthers());
				changingDirection = false;
			}
			previousDirection = direction;
			setTurnRightRadians(Utils.normalRelativeAngle(angle));
			setAhead(Double.MAX_VALUE * direction);
			scan();
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {

		// Bearing
		double eBearingHeadingTheta = e.getBearingRadians();

		// Follow Host Movement
		setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians()
				+ eBearingHeadingTheta - getRadarHeadingRadians()));

		double eDistance = e.getDistance();

		enemy.x = getX() + eDistance
				* Math.sin(getHeadingRadians() + eBearingHeadingTheta);
		enemy.y = getY() + eDistance
				* Math.cos(getHeadingRadians() + eBearingHeadingTheta);

		double realAngle = angleAhead(calculateRelativeAngleToEnemy(getHeadingRadians()));

		Point2D.Double predicted = enemy;

		// Heading
		eBearingHeadingTheta = e.getHeadingRadians();
		// The 11 comes from (20.0 - 3.0 * bulletPower) Sanguijuela uses to
		// bite the maximum 3
		double deltaTime = 0;
		while ((++deltaTime) * 11 < Point2D.Double.distance(getX(), getY(),
				predicted.x, predicted.y)) {
			predicted.x += Math.sin(eBearingHeadingTheta) * e.getVelocity();
			predicted.y += Math.cos(eBearingHeadingTheta) * e.getVelocity();

			if (!territory.contains(predicted)) {
				predicted.x = Math.min(Math.max(17, predicted.x),
						getBattleFieldWidth() - 17);
				predicted.y = Math.min(Math.max(17, predicted.y),
						getBattleFieldHeight() - 17);
				break;
			}
		}
		// Theta
		eBearingHeadingTheta = Utils.normalAbsoluteAngle(Math.atan2(predicted.x
				- getX(), predicted.y - getY()));

		// Targeting
		setTurnGunRightRadians(Utils.normalRelativeAngle(eBearingHeadingTheta
				- getGunHeadingRadians()));

		// Bite
		if (eDistance < 350 && getGunHeat() == 0.0)
			setFire(Math.min(3, getEnergy()));

		enemy = predicted;

		double predictedAngle = angleAhead(calculateRelativeAngleToEnemy(getHeadingRadians()));

		eDistance = Math.abs(predictedAngle);
		eBearingHeadingTheta = Math.abs(realAngle);
		boost = eDistance < 1.2217 /* 70 degrees */ &&
		((sign(predictedAngle) != sign(realAngle) && eBearingHeadingTheta > 0.349 /* 20 degrees */) 
				|| 
		(sign(predictedAngle) == sign(realAngle) && eDistance < eBearingHeadingTheta));
	}

	private static int sign(double v) {
		return v > 0 ? 1 : -1;
	}

	private double calculateRelativeAngleToEnemy(double heading) {
		return Utils.normalRelativeAngle(Math.atan2(enemy.x - getX(), enemy.y
				- getY())
				- heading);
	}

	private double angleAhead(double angle) {
		if (Math.abs(angle) > Math.PI / 2) {
			angle += Math.PI * sign(angle);
		}
		return Utils.normalRelativeAngle(angle);
	}

//	 public void onPaint(Graphics2D g) {
//	 g.fillOval((int)enemy.x-5, (int)enemy.y-5, 10, 10);
//	 g.drawLine((int)enemy.x, (int)enemy.y, (int)getX(),(int)getY());
//	 }

}
