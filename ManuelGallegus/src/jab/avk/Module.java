package jab.avk;

import robocode.*;
import robocode.util.Utils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Module
 * @author jab
 */
public abstract class Module extends AdvancedRobot {

	// Bot's parts
	public Radar radar;
	public Targeting targeting;
	public Movement movement;
	public Gun gun;
	public SelectEnemy selectEnemy;
	public Vector<Special> specials = new Vector<Special>();

	// The power of the next bullet
	public double bulletPower;

	// The current Enemy
	public Enemy enemy = new Enemy();

	// A Hash-table of all the scanned Enemies
	public Hashtable<String, Enemy> enemies = new Hashtable<String, Enemy>();

	// A Vector of all the fired bullets
	public Vector<BulletInfo> bullets = new Vector<BulletInfo>();
	public Vector<BulletInfoEnemy> enemyBullets = new Vector<BulletInfoEnemy>();

	// Debug
	private static int debugOption;

	public void run() {
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Creating the custom event EnemyFires
		addCustomEvent(new Condition("EnemyFires") {
			public boolean test() {
				return (enemy != null
						&& enemy.previousEnergy > enemy.energy
						&& enemy.previousEnergy - enemy.energy <= robocode.Rules.MAX_BULLET_POWER
						&& !Utils.isNear((enemy.previousEnergy - enemy.energy),
								robocode.Rules.getBulletDamage(bulletPower)) && enemy.distance > 55);
			};
		});

		initialize();

		while (true) {
			updateEnemyBullets();
			selectBehavior();
			executeBehavior();
		}
	}

	protected abstract void selectBehavior();

	protected abstract void initialize();

	private void executeBehavior() {
		selectEnemy.select();
		radar.scan();
		gun.fire();
		targeting.target();
		movement.move();
		Iterator<Special> i = specials.iterator();
		while (i.hasNext())
			i.next().doIt();
		execute();
	}

	private void listenEvent(Event e) {
		selectEnemy.listen(e);
		radar.listen(e);
		gun.listen(e);
		targeting.listen(e);
		movement.listen(e);
		Iterator<Special> i = specials.iterator();
		while (i.hasNext())
			i.next().listen(e);
	}

	private void listenInputEvent(InputEvent e) {
		if (selectEnemy != null)
			selectEnemy.listenInput(e);
		if (radar != null)
			radar.listenInput(e);
		if (gun != null)
			gun.listenInput(e);
		if (targeting != null)
			targeting.listenInput(e);
		if (movement != null)
			movement.listenInput(e);
		Iterator<Special> i = specials.iterator();
		while (i.hasNext()) {
			Special special = i.next();
			if (special != null)
				special.listenInput(e);
		}
	}

	public void registerBullet(Bullet bullet) {
		BulletInfo bulletInfo = new BulletInfo();
		bulletInfo.bullet = bullet;
		bulletInfo.toName = enemy.name;
		bulletInfo.targeting = targeting.getClass().getSimpleName();
		bulletInfo.timeFire = (int) getTime();
		bullets.add(bulletInfo);
	}

	private void updateEnemyBullets() {
		Iterator<BulletInfoEnemy> i = enemyBullets.iterator();
		while (i.hasNext()) {
			BulletInfoEnemy bullet = i.next();
			bullet.x = -1 * Math.sin(bullet.headingRadians) * bullet.velocity
					+ bullet.x;
			bullet.y = -1 * Math.cos(bullet.headingRadians) * bullet.velocity
					+ bullet.y;
		}
	}

	public void activate(Special special) {
		if (!specials.contains(special))
			specials.add(special);
	}

	public void deactivate(Special special) {
		specials.remove(special);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		Enemy scanned = enemies.get(e.getName());
		if (scanned == null)
			scanned = new Enemy();
		scanned.name = e.getName();
		scanned.bearing = e.getBearing();
		scanned.bearingRadians = e.getBearingRadians();
		scanned.previousHeadingRadians = scanned.headingRadians;
		scanned.headingRadians = e.getHeadingRadians();
		scanned.distance = e.getDistance();
		scanned.x = getX() + e.getDistance()
				* Math.sin(getHeadingRadians() + e.getBearingRadians());
		scanned.y = getY() + e.getDistance()
				* Math.cos(getHeadingRadians() + e.getBearingRadians());
		scanned.velocity = e.getVelocity();
		scanned.previousEnergy = scanned.energy;
		scanned.energy = e.getEnergy();
		scanned.timeSinceLastScan = (int) e.getTime() - scanned.timeScanned;
		scanned.timeScanned = (int) e.getTime();
		enemies.put(e.getName(), scanned);

		listenEvent(e);
	}

	// Handling the custom event
	public void onCustomEvent(CustomEvent e) {
		Condition condition = e.getCondition();
		if (condition.getName().equals("EnemyFires")) {
			BulletInfoEnemy enemyBullet = new BulletInfoEnemy();
			enemyBullet.fromName = enemy.name;
			enemyBullet.x = enemy.x;
			enemyBullet.y = enemy.y;
			enemyBullet.power = enemy.previousEnergy - enemy.energy;
			enemyBullet.headingRadians = Utils.normalAbsoluteAngle(Math.atan2(
					enemy.x - getX(), enemy.y - getY()));
			enemyBullet.velocity = robocode.Rules
					.getBulletSpeed(enemyBullet.power);
			enemyBullets.add(enemyBullet);
		}
		listenEvent(e);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		listenEvent(e);
	}

	public void onHitRobot(HitRobotEvent e) {
		listenEvent(e);
	}

	public void onHitWall(HitWallEvent e) {
		listenEvent(e);
	}

	public void onBulletHit(BulletHitEvent e) {
		listenEvent(e);
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		listenEvent(e);
	}

	public void onBulletMissed(BulletMissedEvent e) {
		listenEvent(e);
	}

	public void onRobotDeath(RobotDeathEvent e) {
		listenEvent(e);
		enemies.remove(e.getName());
		selectEnemy.select();
	}

	public void onWin(WinEvent e) {
		listenEvent(e);
	}

	public void onDeath(DeathEvent e) {
		listenEvent(e);
	}

	public void onSkippedTurn(SkippedTurnEvent e) {
		listenEvent(e);
	}

	public void onKeyPressed(KeyEvent e) {
		int key = e.getKeyCode() - 48;
		if (key >= 0 && key <= 6) {
			debugOption = key;
		}
		listenInputEvent(e);
	}

	public void onKeyReleased(KeyEvent e) {
		listenInputEvent(e);
	}

	public void onMouseMoved(MouseEvent e) {
		listenInputEvent(e);
	}

	public void onMousePressed(MouseEvent e) {
		listenInputEvent(e);
	}

	public void onMouseReleased(MouseEvent e) {
		listenInputEvent(e);
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.WHITE);
		g
				.drawString(
						"Debug option= "
								+ debugOption
								+ "      0: All      1: SelectEnemy      2: Radar      3: Gun      4: Targeting      5: Movement      6: Specials",
						15, 15);
		switch (debugOption) {
		case 0:
			selectEnemy.onPaint(g);
			radar.onPaint(g);
			gun.onPaint(g);
			targeting.onPaint(g);
			movement.onPaint(g);
			Iterator<Special> i = specials.iterator();
			while (i.hasNext())
				i.next().onPaint(g);
			break;
		case 1:
			selectEnemy.onPaint(g);
			break;
		case 2:
			radar.onPaint(g);
			break;
		case 3:
			gun.onPaint(g);
			break;
		case 4:
			targeting.onPaint(g);
			break;
		case 5:
			movement.onPaint(g);
			break;
		case 6:
			Iterator<Special> it = specials.iterator();
			while (it.hasNext())
				it.next().onPaint(g);
			break;
		}
	}

}
