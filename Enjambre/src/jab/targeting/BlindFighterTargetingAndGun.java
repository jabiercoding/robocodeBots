package jab.targeting;

import jab.module.Module;
import jab.module.Targeting;

import java.io.IOException;
import java.util.Random;

import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.MessageEvent;

public class BlindFighterTargetingAndGun extends Targeting {

	double timeOut = 30;

	double foundEnemyTimeout = -timeOut;
	double tx = 0;
	double ty = 0;
	double target;
	boolean first = true;
	Random generator = new Random();

	public BlindFighterTargetingAndGun(Module bot) {
		super(bot);
	}

	public double angle_180(double ang) {
		return Math.atan2(Math.sin(ang), Math.cos(ang));
	}

	public void target() {

		target = Math.atan2((tx - bot.getX()), (ty - bot.getY()));
		if ((bot.getTime() - foundEnemyTimeout) > timeOut) {
			// No enemy
			double angle = angle_180(bot.getHeadingRadians() + Math.PI / 2
					- bot.getGunHeadingRadians());
			angle += (Math.PI / 8 * (generator.nextDouble() - 0.5));
			bot.setTurnGunRightRadians(angle);
			bot.fire(.1);
		} else {
			// Enemy
			bot.setTurnGunRightRadians(angle_180(target
					- bot.getGunHeadingRadians()));
			bot.fire(3);
		}

	}

	public void listen(Event e) {

		// One of my bullets hits a bot
		if (e instanceof BulletHitEvent) {
			if (!bot.isTeammate(((BulletHitEvent) e).getName())) {
				foundEnemyTimeout = bot.getTime();
				Bullet b = ((BulletHitEvent) e).getBullet();
				tx = b.getX();
				ty = b.getY();
				try {
					bot.broadcastMessage(new MyEnemy(tx, ty));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		
		// A bullet hits me
		else if (e instanceof HitByBulletEvent){
			if (!bot.isTeammate(((HitByBulletEvent)e).getName())) {
				foundEnemyTimeout = bot.getTime();
				double absoluteBearing = bot.getHeadingRadians()
						+ ((HitByBulletEvent) e).getBearingRadians();
				tx = bot.getX() + 100 * Math.sin(absoluteBearing);
				ty = bot.getY() + 100 * Math.cos(absoluteBearing);
				try {
					bot.broadcastMessage(new MyEnemy(tx, ty));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		// I hit another bot
		else if (e instanceof HitRobotEvent) {
			if (!bot.isTeammate(((HitRobotEvent) e).getName())) {
				foundEnemyTimeout = bot.getTime();
				double absoluteBearing = bot.getHeadingRadians()
						+ ((HitRobotEvent) e).getBearingRadians();
				tx = bot.getX() + bot.BOT_WIDTH / 2 * Math.sin(absoluteBearing);
				ty = bot.getY() + bot.BOT_WIDTH / 2 * Math.cos(absoluteBearing);
				try {
					bot.broadcastMessage(new MyEnemy(tx, ty));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		// A Teammate sends me an enemy position
		else if (e instanceof MessageEvent) {
			if (((MessageEvent) e).getMessage() instanceof MyEnemy) {
				foundEnemyTimeout = bot.getTime();
				MyEnemy ep = (MyEnemy) ((MessageEvent) e).getMessage();
				tx = ep.x;
				ty = ep.y;
			}
		}
	}
};
