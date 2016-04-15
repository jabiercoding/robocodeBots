package jab.movement;

import jab.module.BotInfo;
import jab.module.BulletInfoEnemy;
import jab.module.Module;
import jab.module.Movement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * DiamondMovement
 * 
 * @author Jabi
 */
public class DiamondMovement extends Movement {

	public static int[][][] brain;

	final static double BOT_WIDTH = 36;

	// Forces
	double xforce = 0;
	double yforce = 0;

	final static double BOT_FORCE = -5000;
	final static double MAX_LINE_FORCE = -1000;
	final static double WALL_FORCE = -10000;
	final static double LINE_POINTS_DISTANCE = 1;// BOT_WIDTH/3;

	final static double ENEMY_DISTANCE = 250;

	private static final double MAX_DISTANCE_VAR_SEGM = 800;
	private static final double MAX_DISTANCE_LINES = 200;
	private static final int VARIABLE_BINS = 6;
	private static final int TARGETING_BINS = 101;
	final static double MAX_ESCAPE_ANGLE = 1;// 0.7;

	// Vector<EnemyGravPoint> enemyGravPoints;
	Vector<Line2D.Double> laserBeams = new Vector<Line2D.Double>();

	Vector<GravPoint> gravPoints = new Vector<GravPoint>();
	Vector<EnemyWave> enemyWaves = new Vector<EnemyWave>();

	// A list of the botsInfo that hits me
	Vector<String> hitBy = new Vector<String>();

	Vector<BulletInfoEnemy> bulletHitsBullet = new Vector<BulletInfoEnemy>();
	Rectangle2D.Double myRect = new Rectangle2D.Double(0, 0, BOT_WIDTH,
			BOT_WIDTH);

	public DiamondMovement(Module bot) {
		super(bot);
	}

	@SuppressWarnings("static-access")
	@Override
	public void move() {
		if (brain == null) {
			brain = new int[bot.totalNumOfEnemies][VARIABLE_BINS][TARGETING_BINS];
		}
		gravPoints.removeAllElements();
		updateEnemyWaves();
		updateFriendFire();
		updateBulletHitsBullet();
		checkBulletHitsBullet();
		calculateForceAndMove();
	}

	private void updateFriendFire() {
		Enumeration<BulletInfoEnemy> friendFire = bot.enemyBullets.elements();
		while (friendFire.hasMoreElements()) {
			BulletInfoEnemy bul = friendFire.nextElement();
			if (bul.isFriendFire) {
				GravPoint gravPoint = new GravPoint();
				gravPoint.x = bul.x;
				gravPoint.y = bul.y;
				gravPoint.power = BOT_FORCE
						* Math.max(0, (1 - bul.distance(new Point2D.Double(bot
								.getX(), bot.getY())) / 500));
				gravPoints.add(gravPoint);
			}
		}
	}

	private void checkBulletHitsBullet() {
		for (int i = 0; i < bulletHitsBullet.size(); i++) {
			BulletInfoEnemy bullet = bulletHitsBullet.get(i);
			if (myRect.contains(new Point2D.Double(bullet.x, bullet.y))) {
				System.out.println("###### Virtual Hit #####");
				EnemyWave hitWave = getHitWave(bullet.velocity);
				if (hitWave != null) {
					logHit(hitWave, new Point2D.Double(bullet.x, bullet.y));
					enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
				}
				bulletHitsBullet.remove(bullet);
			}
		}
	}

	/**
	 * Look through the EnemyWaves, and find one that could've hit us
	 */
	private EnemyWave getHitWave(double velocity) {
		EnemyWave hitWave = null;
		for (int x = 0; x < enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(x);

			if (Math.abs(ew.travelledDistance
					- new Point2D.Double(bot.getX(), bot.getY())
							.distance(ew.origin)) < 50
					&& Math.round(velocity * 10) == Math
							.round(ew.velocity * 10)) {
				hitWave = ew;
				break;
			}
		}
		return hitWave;
	}

	@SuppressWarnings("static-access")
	private void updateBulletHitsBullet() {
		for (int i = 0; i < bulletHitsBullet.size(); i++) {
			BulletInfoEnemy bullet = bulletHitsBullet.get(i);
			bullet.x = Math.sin(bullet.headingRadians) * bullet.velocity
					+ bullet.x;
			bullet.y = Math.cos(bullet.headingRadians) * bullet.velocity
					+ bullet.y;

			if (!bot.battleField
					.contains(new Point2D.Double(bullet.x, bullet.y))) {
				bulletHitsBullet.remove(bullet);
			}

			myRect.x = bot.getX() - BOT_WIDTH / 2;
			myRect.y = bot.getY() - BOT_WIDTH / 2;
		}
	}

	private void updateEnemyWaves() {
		for (int i = 0; i < enemyWaves.size(); i++) {
			EnemyWave ew = enemyWaves.get(i);
			ew.updateWave();
			if (hitBy.contains(ew.name) || bot.getOthers() == 1)
				createGravLine(ew);
			if (ew.travelledDistance > ew.origin.distance(new Point2D.Double(
					bot.getX(), bot.getY()))
					+ BOT_WIDTH) {
				enemyWaves.remove(ew);
			}
		}
	}

	// private Line2D.Double createLine(Point2D.Double origin, double angle){
	// Line2D.Double line = new Line2D.Double();
	// line.x1 = origin.x;
	// line.x2 = origin.y;
	// line.y1 = origin.x+Math.sin(angle)*1000;
	// line.y2 = origin.y+Math.cos(angle)*1000;
	// return line;
	// }

	@SuppressWarnings("static-access")
	private void createGravLine(EnemyWave ew) {

		GravPoint closestGP = null;

		Point2D.Double point = (Point2D.Double) ew.origin.clone();
		Point2D.Double me = new Point2D.Double(bot.getX(), bot.getY());
		while (bot.battleField.contains(point)) {

			// TODO Mathematical method please!
			
			if (ew.travelledDistance < ew.origin.distance(me)) {
				// if (ew.travelledDistance<ew.origin.distance(point)){
				if (closestGP == null) {
					closestGP = new GravPoint();
					closestGP.setLocation((Point2D.Double) point.clone());
				}
				
				if (closestGP.distance(me) >= point.distance(me)) {
					closestGP.setLocation((Point2D.Double) point.clone());
				}

				// GravPoint gp = new GravPoint();
				// gp.setLocation(point.x/*+Math.random()*5*/,point.y/*+Math.random()*5*/);
				// double distancePointToMe = point.distance(new
				// Point2D.Double(bot.getX(),bot.getY()));
				// double forcePerOne =
				// 1-Math.min(1,distancePointToMe/MAX_DISTANCE_LINES);
				// double distanceWaveToMe = ew.origin.distance(new
				// Point2D.Double(bot.getX(),bot.getY()))-ew.travelledDistance;
				// double modificator =
				// 1-Math.min(1,distanceWaveToMe/MAX_DISTANCE_VAR_SEGM);
				// gp.power= MAX_LINE_FORCE*modificator*forcePerOne;
				// gravPoints.add(gp);
			}
			point.setLocation(point.x + Math.sin(ew.lineAngle)
					* LINE_POINTS_DISTANCE, point.y + Math.cos(ew.lineAngle)
					* LINE_POINTS_DISTANCE);
		}
		if (closestGP != null) {
			double distancePointToMe = closestGP.distance(new Point2D.Double(
					bot.getX(), bot.getY()));
			double forcePerOne = 1 - Math.min(1, distancePointToMe
					/ MAX_DISTANCE_LINES);
			double distanceWaveToMe = ew.origin.distance(new Point2D.Double(bot
					.getX(), bot.getY()))
					- ew.travelledDistance;
			double modificator = 1 - Math.min(1, distanceWaveToMe
					/ MAX_DISTANCE_VAR_SEGM);
			closestGP.power = MAX_LINE_FORCE * modificator * forcePerOne;
			gravPoints.add(closestGP);
		}
	}

	@Override
	public void listen(Event e) {

		/** ScannedRobotEvent * */
		if (e instanceof ScannedRobotEvent) {
			String name = ((ScannedRobotEvent) e).getName();

			// Check if it fired
			BotInfo enemy = bot.botsInfo.get(name);

			if (!bot.isTeammate(name)) {

				double energyDrop = enemy.previousEnergy - enemy.energy;

				// TODO exception when hit the wall
				if (energyDrop > 0 && energyDrop <= 3) {
					int var1_Distance = (int) ((Math.min(MAX_DISTANCE_VAR_SEGM,
							enemy.distance) / (MAX_DISTANCE_VAR_SEGM / (VARIABLE_BINS - 1))));
					var1_Distance = betweenMinMax(var1_Distance, 0,
							VARIABLE_BINS - 1);
					int vars[] = { var1_Distance };

					int maxVisits = 0;
					int maxOffset = TARGETING_BINS / 2;
					for (int i = 0; i < TARGETING_BINS; i++) {
						int current = brain[bot.getEnemyAssignedNum(name)][var1_Distance][i];
						if (current > maxVisits) {
							maxVisits = current;
							maxOffset = i;
						}
					}

					double relativeAngle = ((maxOffset * MAX_ESCAPE_ANGLE * 2 / (TARGETING_BINS - 1)) - MAX_ESCAPE_ANGLE);
					double lineAngle = relativeAngle
							+ absBearing(enemy.x, enemy.y, bot.getX(), bot
									.getY());
					enemyWaves.add(new EnemyWave(enemy.name,
							new Point2D.Double(enemy.x, enemy.y),
							robocode.Rules.getBulletSpeed(energyDrop),
							new Point2D.Double(bot.getX(), bot.getY()), vars,
							lineAngle));
				}
			}
		}

		/** HitByBulletEvent * */
		else if (e instanceof HitByBulletEvent) {
			EnemyWave ew = getHitWave((HitByBulletEvent) e);
			if (ew.name != null) {
				ew.hit = true;
				logHit(ew, new Point2D.Double(bot.getX(), bot.getY()));
				enemyWaves.remove(ew);
			}
		}

		/** BulletHitBulletEvent * */
		else if (e instanceof BulletHitBulletEvent) {

			System.out.println("###### BULLET HIT BULLET #####");
			Bullet hitBullet = ((BulletHitBulletEvent) e).getHitBullet();

			BulletInfoEnemy enemyBullet = new BulletInfoEnemy();
			enemyBullet.headingRadians = hitBullet.getHeadingRadians();
			enemyBullet.velocity = hitBullet.getVelocity();
			enemyBullet.x = hitBullet.getX();
			enemyBullet.y = hitBullet.getY();
			bulletHitsBullet.add(enemyBullet);
		}
	}

	private void logHit(EnemyWave wave, Point2D.Double myPosition) {

		System.out.println("LogHit: " + wave.name);

		if (!hitBy.contains(wave.name)) {
			hitBy.add(wave.name);
			System.out.println("Take into account: " + wave.name);
		}

		double thetaFireTime = Utils.normalAbsoluteAngle(Math.atan2(wave.me.x
				- wave.origin.x, wave.me.y - wave.origin.y));
		double thetaBreak = Utils.normalAbsoluteAngle(Math.atan2(bot.getX()
				- wave.origin.x, bot.getY() - wave.origin.y));

		double offset = Utils.normalRelativeAngle(thetaFireTime - thetaBreak);
		int targeting_offset = (int) ((offset + MAX_ESCAPE_ANGLE) / (MAX_ESCAPE_ANGLE * 2 / (TARGETING_BINS - 1)));
		// Out of bounds prevention
		targeting_offset = betweenMinMax(targeting_offset, 0,
				TARGETING_BINS - 1);
		brain[bot.getEnemyAssignedNum(wave.name)][wave.vars[0]][targeting_offset]++;
	}

	/**
	 * Get the Wave that corresponds to a Hit by Bullet
	 */
	private EnemyWave getHitWave(HitByBulletEvent event) {
		EnemyWave hitWave = new EnemyWave();
		double maxDistanceTraveled = 0;
		for (int i = 0; i < enemyWaves.size(); i++) {
			EnemyWave ew = enemyWaves.get(i);
			if (ew.name.equals(event.getName())) {
				if (ew.travelledDistance > maxDistanceTraveled) {
					maxDistanceTraveled = ew.time * ew.velocity;
					hitWave = ew;
				}
			}
		}
		return hitWave;
	}

	private void calculateForceAndMove() {
		xforce = 0;
		yforce = 0;
		double force;
		double ang;

		Enumeration<BotInfo> others = bot.botsInfo.elements();

		// Other bots
		while (others.hasMoreElements()) {
			BotInfo botInfo = others.nextElement();
			if (!botInfo.name.equals(bot.getName())) {
				GravPoint botGravPoint = new GravPoint();
				botGravPoint.x = botInfo.x;
				botGravPoint.y = botInfo.y;
				if (bot.enemy != null && bot.enemy.name.equals(botInfo.name)) {
					// botGravPoint.power =
					// (bot.enemy.distance<=250)?BOT_FORCE*2:-BOT_FORCE*5;
					botGravPoint.power = /* 10* */5 * BOT_FORCE
							* (1 - bot.enemy.distance / ENEMY_DISTANCE);
				} else {
					botGravPoint.power = BOT_FORCE;
				}
				gravPoints.add(botGravPoint);
			}
		}

		Iterator<GravPoint> i = gravPoints.iterator();
		while (i.hasNext()) {
			GravPoint gravPoint = i.next();
			force = gravPoint.power
					/ Math.pow(getRange(bot.getX(), bot.getY(), gravPoint.x,
							gravPoint.y), 2);
			// Find the bearing from the point to us
			ang = normaliseBearing(Math.PI
					/ 2
					- Math.atan2(bot.getY() - gravPoint.y, bot.getX()
							- gravPoint.x));
			// Add the components of this force to the total force in their
			// respective directions
			xforce += Math.sin(ang) * force;
			yforce += Math.cos(ang) * force;
		}

		/**
		 * The following four lines add wall avoidance.
		 */
		xforce += (-WALL_FORCE)
				/ Math.pow(getRange(bot.getX(), bot.getY(), bot
						.getBattleFieldWidth(), bot.getY()), 2);
		xforce -= (-WALL_FORCE)
				/ Math.pow(getRange(bot.getX(), bot.getY(), 0, bot.getY()), 2);
		yforce += (-WALL_FORCE)
				/ Math.pow(getRange(bot.getX(), bot.getY(), bot.getX(), bot
						.getBattleFieldHeight()), 2);
		yforce -= (-WALL_FORCE)
				/ Math.pow(getRange(bot.getX(), bot.getY(), bot.getX(), 0), 2);

		// Move in the direction of our resolved force.
		xforce *= 2;
		yforce *= 2;
		goTo(bot.getX() - xforce, bot.getY() - yforce);
	}

	/** Move towards an x and y coordinate * */
	private void goTo(double x, double y) {
		double dist = 20;
		double angle = Math.toDegrees(absBearing(bot.getX(), bot.getY(), x, y));
		double r = turnTo(angle);
		bot.setAhead(dist * r);
	}

	/**
	 * Turns the shortest angle possible to come to a heading, then returns the
	 * direction the the bot needs to move in.
	 */
	private int turnTo(double angle) {
		double ang;
		int dir;
		ang = normaliseBearing(bot.getHeading() - angle);
		if (ang > 90) {
			ang -= 180;
			dir = -1;
		} else if (ang < -90) {
			ang += 180;
			dir = -1;
		} else {
			dir = 1;
		}
		bot.setTurnLeft(ang);
		return dir;
	}

	// if a bearing is not within the -pi to pi range, alters it to provide the
	// shortest angle
	private double normaliseBearing(double ang) {
		if (ang > Math.PI)
			ang -= 2 * Math.PI;
		if (ang < -Math.PI)
			ang += 2 * Math.PI;
		return ang;
	}

	// returns the distance between two x,y coordinates
	private double getRange(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double h = Math.sqrt(xo * xo + yo * yo);
		return h;
	}

	// gets the absolute bearing between to x,y coordinates
	private double absBearing(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double h = getRange(x1, y1, x2, y2);
		if (xo > 0 && yo > 0) {
			return Math.asin(xo / h);
		}
		if (xo > 0 && yo < 0) {
			return Math.PI - Math.asin(xo / h);
		}
		if (xo < 0 && yo < 0) {
			return Math.PI + Math.asin(-xo / h);
		}
		if (xo < 0 && yo > 0) {
			return 2.0 * Math.PI - Math.asin(-xo / h);
		}
		return 0;
	}

	/**
	 * Paint
	 */
	public void onPaint(Graphics2D g) {
		g.setColor(Color.red);

		// Waves
		for (int i = 0; i < enemyWaves.size(); i++) {
			EnemyWave ew = enemyWaves.get(i);
			// if (ew.hit){
			// g.setColor(Color.red);
			// }
			// else {
			// g.setColor(Color.white);
			// }
			if (hitBy.contains(ew.name))
				g.drawLine((int) ew.origin.x, (int) ew.origin.y,
						(int) (ew.origin.x + Math.sin(ew.lineAngle) * 1000),
						(int) (ew.origin.y + Math.cos(ew.lineAngle) * 1000));
			// g.draw(ew);
		}

		// Grav Points
		g.setColor(new Color(255, 0, 0,/* 85 */100));
		for (int i = 0; i < gravPoints.size(); i++) {
			GravPoint point = gravPoints.get(i);
			double size = point.power / MAX_LINE_FORCE;
			if (size <= 1)
				g.fillOval((int) (point.getX() - 16 * size), (int) (point
						.getY() - 16 * size), (int) (32 * size),
						(int) (32 * size));
			else
				g.drawOval((int) (point.getX() - 16 * size), (int) (point
						.getY() - 16 * size), (int) (32 * size),
						(int) (32 * size));
		}

		g.setColor(Color.red);
		for (int i = 0; i < laserBeams.size(); i++) {
			g.draw(laserBeams.get(i));
		}

		// Force
		g.setColor(Color.white);
		// g.drawString(xforce+","+yforce, 100, 100);
		g.drawLine((int) bot.getX(), (int) bot.getY(),
				(int) (10 * -xforce + bot.getX()), (int) (10 * -yforce + bot
						.getY()));

		// BulletHitsBullet
		g.setColor(Color.white);
		for (int i = 0; i < bulletHitsBullet.size(); i++) {
			g.draw(myRect);
			g.drawOval((int) bulletHitsBullet.get(i).x - 2,
					(int) bulletHitsBullet.get(i).y - 2, 4, 4);
		}

	}

	/**
	 * A stitch in time saves nine. Index out of bounds prevention.
	 */
	private static int betweenMinMax(int number, int min, int max) {
		int aux = number;
		aux = Math.min(max, Math.max(min, number));
		if (aux != number)
			System.out.println("ERROR: " + number + " was out of bounds! Min: "
					+ min + " Max: " + max);
		return aux;
	}

	/** Holds the x, y, and strength info of a gravity point* */
	class GravPoint extends Point2D.Double {
		private static final long serialVersionUID = 1L;
		public double power;
	}

	public class EnemyWave extends Ellipse2D.Double {
		private static final long serialVersionUID = 1L;

		public double lineAngle;
		public String name;
		public double time;
		public Point2D.Double origin;
		public double velocity;
		public Point2D.Double me;
		public double travelledDistance;
		public int[] vars;
		public boolean hit = false;

		public EnemyWave() {

		}

		public EnemyWave(String name, Point2D.Double origin, double velocity,
				Point2D.Double me, int[] vars, double lineAngle) {
			this.name = name;
			this.origin = origin;
			this.velocity = velocity;
			this.time = 0;
			this.me = me;
			this.vars = vars;
			this.x = origin.x - BOT_WIDTH / 2;
			this.y = origin.y - BOT_WIDTH / 2;
			this.lineAngle = lineAngle;
		}

		public void updateWave() {
			time++;
			travelledDistance = time * velocity;
			x = origin.x - travelledDistance;
			y = origin.y - travelledDistance;
			width = height = travelledDistance * 2;
		}
	}

}