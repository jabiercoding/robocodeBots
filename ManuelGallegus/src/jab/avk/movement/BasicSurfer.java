package jab.avk.movement;

import jab.avk.BulletInfoEnemy;
import jab.avk.Module;
import jab.avk.Movement;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.DeathEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.RobocodeFileOutputStream;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.WinEvent;
import robocode.util.Utils;


/**
 * WaveSurfing
 * @author Voidious
 * @author Jabi
 */
public class BasicSurfer extends Movement {
	
	int var1_BulletPower;
	int var2_Distance;
	
	final static double MAX_DISTANCE = 500;
	final static int DISTANCE_BINS = 6;
	final static int BULLETPOWER_BINS = 2;
	
	public static int BINS = 47;
	public static double _surfStats[][][] = new double[BULLETPOWER_BINS][DISTANCE_BINS][BINS]; // we'll use 47 bins
	public Point2D.Double _myLocation; // our bot's location
	public Point2D.Double _enemyLocation; // enemy bot's location

	public ArrayList<EnemyWave> _enemyWaves;
	public ArrayList<Integer> _surfDirections;
	public ArrayList<Double> _surfAbsBearings;

	// We must keep track of the enemy's energy level to detect EnergyDrop,
	// indicating a bullet is fired
	public static double _oppEnergy = 100.0;

	public double previousVelocity = 0;
	
	// This is a rectangle that represents an 800x600 battle field,
	// used for a simple, iterative WallSmoothing method (by Kawigi).
	// If you're not familiar with WallSmoothing, the wall stick indicates
	// the amount of space we try to always have on either end of the tank
	// (extending straight out the front or back) before touching a wall.
	public static Rectangle2D.Double _fieldRect;
	public static double WALL_STICK = 300;
	
	Vector<BulletInfoEnemy> bulletHitsBullet = new Vector<BulletInfoEnemy>();
	final static double BOT_WIDTH= 36;
	Rectangle2D.Double myRect = new Rectangle2D.Double(0,0,BOT_WIDTH,BOT_WIDTH);
	
	public BasicSurfer(Module bot) {
		super(bot);
		_enemyWaves = new ArrayList<EnemyWave>();
		_surfDirections = new ArrayList<Integer>();
		_surfAbsBearings = new ArrayList<Double>();
	}

	public void move() {
		_fieldRect = new Rectangle2D.Double(BOT_WIDTH/2, BOT_WIDTH/2, bot.getBattleFieldWidth() - BOT_WIDTH, bot.getBattleFieldHeight() - BOT_WIDTH);
		updateWaves();
		updateBulletHitsBullet();
		checkBulletHitsBullet();
		doSurfing();

	}

//	static boolean newEnemy =false;
	static int wins = 0;
	public static boolean brainRestored = false;
	
	public void listen(Event e) {
		
		if (e instanceof SkippedTurnEvent){
			System.out.println("SKIPPED TURN ###################");
			System.out.println("SKIPPED TURN ###################");
			System.out.println("SKIPPED TURN ###################");
			System.out.println("SKIPPED TURN ###################");
			System.out.println("SKIPPED TURN ###################");
		}
		
		if (e instanceof WinEvent){
			wins++;
		}
		
		if ((bot.getRoundNum()+1 == bot.getNumRounds()) && (e instanceof WinEvent || e instanceof DeathEvent)){
			double winPercentage = ((double)wins / (double)bot.getNumRounds())*100;
			System.out.println("Win Percentage: "+winPercentage);
			if (winPercentage < 70 || brainRestored)
				saveBrain();
		}
		
		
		if (e instanceof ScannedRobotEvent) {
			
			
//			if (!newEnemy && bot.enemy.name!=null){
//				newEnemy=true;
//				restoreBrain();
//			}
			
			
			_myLocation = new Point2D.Double(bot.getX(), bot.getY());

			double lateralVelocity = bot.getVelocity()
					* Math.sin(((ScannedRobotEvent) e).getBearingRadians());
			double absBearing = ((ScannedRobotEvent) e).getBearingRadians()
					+ bot.getHeadingRadians();

			_surfDirections
					.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));
			_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

			
			double bulletPower = _oppEnergy
					- ((ScannedRobotEvent) e).getEnergy();

			// Enemy is shooting
			 if (bulletPower < 3.01 && bulletPower > 0.09 
					 && _surfDirections.size() > 2
					 && !(bot.enemy.velocity==0 && bulletPower==Rules.getWallHitDamage(previousVelocity))) {
				 
				 
				//* BulletPower
				var1_BulletPower = (bulletPower<2)?0:1;
				
				//* Distance
				var2_Distance = (int) ((Math.min(MAX_DISTANCE, bot.enemy.distance) / (MAX_DISTANCE/(DISTANCE_BINS-1))));
				var2_Distance = betweenMinMax(var2_Distance, 0, DISTANCE_BINS);			
				
				EnemyWave ew = new EnemyWave();
				ew.vars[0] = var1_BulletPower;
				ew.vars[1] = var2_Distance;
				ew.fireTime = bot.getTime() - 1;
				ew.bulletVelocity = Rules.getBulletSpeed(bulletPower);
				ew.distanceTraveled = Rules.getBulletSpeed(bulletPower);
				ew.direction = ((Integer) _surfDirections.get(2)).intValue();
				ew.directAngle = ((Double) _surfAbsBearings.get(2))
				.doubleValue();
				ew.origin = (Point2D.Double) _enemyLocation.clone(); // last tick
				_enemyWaves.add(ew);
			}

			_oppEnergy = ((ScannedRobotEvent) e).getEnergy();
			previousVelocity = ((ScannedRobotEvent) e).getVelocity();
			
			// update after EnemyWave detection, because that needs the previous
			// enemy location as the source of the wave
			_enemyLocation = project(_myLocation, absBearing,
					((ScannedRobotEvent) e).getDistance());

		}

		else if (e instanceof HitByBulletEvent) {
			// If the _enemyWaves collection is empty, we must have missed the
			// detection of this wave somehow.
			if (!_enemyWaves.isEmpty()) {
				Point2D.Double hitBulletLocation = new Point2D.Double(
						((HitByBulletEvent) e).getBullet().getX(),
						((HitByBulletEvent) e).getBullet().getY());
				EnemyWave hitWave = getHitWave(Rules.getBulletSpeed(((HitByBulletEvent) e)
									.getBullet().getPower()));

				if (hitWave != null) {
					logHit(hitWave, hitBulletLocation);

					// We can remove this wave now, of course.
					_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
				}
			}
		}
		
		else if (e instanceof BulletHitBulletEvent){

			System.out.println("###### BULLET HIT BULLET #####");
			Bullet hitBullet = ((BulletHitBulletEvent)e).getHitBullet();
			
			BulletInfoEnemy enemyBullet = new BulletInfoEnemy();
			enemyBullet.headingRadians = hitBullet.getHeadingRadians();
			enemyBullet.velocity = hitBullet.getVelocity();
			enemyBullet.x = hitBullet.getX();
			enemyBullet.y = hitBullet.getY();
			bulletHitsBullet.add(enemyBullet);			
		}
	};

	
	private void updateBulletHitsBullet(){
		for (int i = 0; i < bulletHitsBullet.size(); i++){
			BulletInfoEnemy bullet = bulletHitsBullet.get(i);
			bullet.x = Math.sin(bullet.headingRadians) * bullet.velocity + bullet.x;
			bullet.y = Math.cos(bullet.headingRadians) * bullet.velocity + bullet.y;
			
			if (!_fieldRect.contains(new Point2D.Double(bullet.x,bullet.y))){
				bulletHitsBullet.remove(bullet);
			}
			
			myRect.x= bot.getX() - BOT_WIDTH/2;
			myRect.y= bot.getY() - BOT_WIDTH/2;
		}
	}
	
	/**
	 * Look through the EnemyWaves, and find one that could've hit us
	 */
	private EnemyWave getHitWave(double velocity){
		EnemyWave hitWave = null;
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

			if (Math.abs(ew.distanceTraveled
					- _myLocation.distance(ew.origin)) < 50
					&& Math.round(velocity * 10) == Math
							.round(ew.bulletVelocity * 10)) {
				hitWave = ew;
				break;
			}
		}
		return hitWave;
	}
	
	private void checkBulletHitsBullet(){
		for (int i = 0; i < bulletHitsBullet.size(); i++){
			BulletInfoEnemy bullet = bulletHitsBullet.get(i);
			if (myRect.contains(new Point2D.Double(bullet.x,bullet.y))){
				System.out.println("###### Virtual Hit #####");
				
				EnemyWave hitWave = getHitWave(bullet.velocity);

				if (hitWave != null){
					logHit(hitWave, new Point2D.Double(bullet.x,bullet.y));
					_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
				}
				bulletHitsBullet.remove(bullet);
			}
		}
	}
	
	
	private void updateWaves() {
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

			ew.distanceTraveled = (bot.getTime() - ew.fireTime)
					* ew.bulletVelocity;
			if (ew.distanceTraveled > _myLocation.distance(ew.origin) + 50) {
				_enemyWaves.remove(x);
				x--;
			}
		}
	}

	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I just use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);
			double distance = _myLocation.distance(ew.origin)
					- ew.distanceTraveled;

			
			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (absoluteBearing(ew.origin, targetLocation) - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int) limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < BINS; x++) {
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...		
			_surfStats[var1_BulletPower][var2_Distance][x] += 1.0 / (Math.pow(index - x, 2) + 1);
			
			// jab: also add in the closest distance segments
			// for the spot bin that we were hit on, add 1 / 3;
			// for the bins next to it, add 1 / 4;
			// the next one, add 1 / 7; and so on...	
			if (var2_Distance-1>=0){
				_surfStats[var1_BulletPower][var2_Distance-1][x] += 1.0 / (Math.pow(index - x, 2) + 3);
			}
			if (var2_Distance+1<DISTANCE_BINS){
				_surfStats[var1_BulletPower][var2_Distance+1][x] += 1.0 / (Math.pow(index - x, 2) + 3);
			}
		}
	}
	
	/**
	 * A stitch in time saves nine.
	 * Index out of bounds prevention.
	 */
	private static int betweenMinMax(int number, int min, int max){
		int aux=number;
		aux = Math.min(max, Math.max(min, number));
		if (aux!=number)
			System.out.println("ERROR: "+number+" was out of bounds! Min: "+min+" Max: "+max);
		return aux;
	}

	// CREDIT: mini sized predictor from Apollon, by rozu
	// http://robowiki.net?Apollon
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) _myLocation.clone();
		double predictedVelocity = bot.getVelocity();
		double predictedHeading = bot.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(predictedPosition, absoluteBearing(
					surfWave.origin, predictedPosition)
					+ (direction * (Math.PI / 2)), direction)
					- predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d
					* (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to break down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir
					: moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.origin) < surfWave.distanceTraveled
					+ (counter * surfWave.bulletVelocity)
					+ surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);

		return predictedPosition;
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		Point2D.Double myFutureLocation = predictPosition(surfWave,
				direction);
		int index = getFactorIndex(surfWave, myFutureLocation);
		
		return _surfStats[surfWave.vars[0]][surfWave.vars[1]][index];
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) {
			return;
		}

		surfWave.isBeingSurfed = true;
		
		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);
		
		double goAngle = absoluteBearing(surfWave.origin, _myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI / 2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI / 2), 1);
		}

		setBackAsFront(bot, goAngle);
	}

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point2D.Double botLocation, double angle,
			int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	// CREDIT: from CassiusClay, by PEZ
	// - returns point length away from sourceLocation, at angle
	// robowiki.net?CassiusClay
	public static Point2D.Double project(Point2D.Double sourceLocation,
			double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
				sourceLocation.y + Math.cos(angle) * length);
	}

	// got this from RaikoMicro, by Jamougha, but I think it's used by many
	// authors
	// - returns the absolute angle (in radians) from source to target points
	public static double absoluteBearing(Point2D.Double source,
			Point2D.Double target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle
				- robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1 * angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	public void onPaint(java.awt.Graphics2D g) {
		g.setColor(Color.red);
		for (int i = 0; i < _enemyWaves.size(); i++) {
			EnemyWave w = (EnemyWave) (_enemyWaves.get(i));
			int radius = (int) w.distanceTraveled;
			Point2D.Double center = w.origin;
			
			if (w.isBeingSurfed) 
				g.setColor(Color.red);
			else
				g.setColor(Color.green);
				
			if (radius - 40 < center.distance(_myLocation))
				g.drawOval((int) (center.x - radius),
						(int) (center.y - radius), radius * 2, radius * 2);
		}
		
		// BulletHitsBullet
		g.setColor(Color.white);
		for (int i = 0; i < bulletHitsBullet.size(); i++){
			g.draw(myRect);
			g.drawOval((int)bulletHitsBullet.get(i).x-2,(int)bulletHitsBullet.get(i).y-2,4,4);
		}
		
	}
	
	
	
	final static String ZIPENTRY_NAME = "movementBrain";
	
	/**
	 * Restore Brain
	 */
	public void restoreBrain() {
        try {
            ZipInputStream zipin = new ZipInputStream(new
                FileInputStream(bot.getDataFile(bot.enemy.name + "_Movement.zip")));
            zipin.getNextEntry();
            ObjectInputStream in = new ObjectInputStream(zipin);
            byte[][] smallBrain = (byte[][])in.readObject();
            for (int v1=0; v1<BULLETPOWER_BINS; v1++){
                for (int v2=0; v2<DISTANCE_BINS; v2++){
                    	int offset = smallBrain[v1][v2];
                    	// offset==BINS means that there was no data
                    	if (offset != BINS){
                    		for (int x = 0; x < BINS; x++) {
                    			// for the spot bin that we were hit on, add 1;
                    			// for the bins next to it, add 1 / 2;
                    			// the next one, add 1 / 5; and so on...		
                    			_surfStats[v1][v2][x] += 1.0 / (Math.pow(offset - x, 2) + 1);
                    			
                    			// jab: also add in the closest distance segments
                    			// for the spot bin that we were hit on, add 1 / 3;
                    			// for the bins next to it, add 1 / 4;
                    			// the next one, add 1 / 7; and so on...	
                    			if (v2-1>=0){
                    				_surfStats[v1][v2][x] += 1.0 / (Math.pow(offset - x, 2) + 3);
                    			}
                    			if (v2+1<DISTANCE_BINS){
                    				_surfStats[v1][v2][x] += 1.0 / (Math.pow(offset - x, 2) + 3);
                    			}
                    		}
                    	}
                }
            }
            in.close();
            brainRestored = true;
            System.out.println("I know you. Movement");
        }
        catch (IOException e) {
            System.out.println("Ah! A new aquaintance. I'll be watching you " + bot.enemy.name + ".");
            _surfStats = new double[BULLETPOWER_BINS][DISTANCE_BINS][BINS];
        }
        catch (ClassNotFoundException e) {
            System.out.println("Error reading enemy aim factors:" + e);
        }
    }

	
	/**
	 * Save Brain
	 */
    private void saveBrain() {
        try {
        	ZipOutputStream zipout;
            zipout = new ZipOutputStream(new RobocodeFileOutputStream(bot.getDataFile(bot.enemy.name + "_Movement.zip")));
            zipout.putNextEntry(new ZipEntry(ZIPENTRY_NAME));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            byte[][] smallBrain = new byte[BULLETPOWER_BINS][DISTANCE_BINS];
            for (int v1=0; v1<BULLETPOWER_BINS; v1++){
                for (int v2=0; v2<DISTANCE_BINS; v2++){
            			// Get the most visited targeting bin
            			double maxRisk=0;
            			int riskOffset = BINS/2;
            			for (int i=0;i<BINS;i++){
            				if (maxRisk<_surfStats[v1][v2][i]){
            					maxRisk=_surfStats[v1][v2][i];
            					riskOffset=i;
            				}
            			}
            			if (maxRisk==0)
            				// No data
            				smallBrain[v1][v2]=new Integer(BINS).byteValue();
            			else
            				smallBrain[v1][v2]=new Integer(riskOffset).byteValue();
                }
            }
            out.writeObject(smallBrain);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error writing factors:" + e);
        }
    }
	
	

	class EnemyWave  {
		Point2D.Double origin;
		int[] vars = new int[2];
		long fireTime;
		double bulletVelocity, directAngle, distanceTraveled;
		int direction;
		boolean isBeingSurfed;
	}
}
