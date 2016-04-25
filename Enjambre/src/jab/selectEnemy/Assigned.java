package jab.selectEnemy;

import jab.module.BotInfo;
import jab.module.Module;
import jab.module.SelectEnemy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Enumeration;

import robocode.Event;
import robocode.MessageEvent;

public class Assigned extends SelectEnemy {

	public Assigned(Module bot) {
		super(bot);
	}

	public void select() {
	}

	public void listen(Event e) {
		if (e instanceof MessageEvent) {
			MessageEvent message = (MessageEvent) e;
			if (message.getMessage() instanceof String[]) {
				String[] m = (String[]) message.getMessage();
				for (int i = 4; i <= 7; i++) {
					if (m[i].equals(bot.getName())) {
						if (m[i - 4] != null) {
							// I have an assignation
							bot.enemy = bot.botsInfo.get(m[i - 4]);
						} else if (bot.getEnemiesLeader() != null) {
							// My enemy is the leader
							bot.enemy = bot.getEnemiesLeader();
						} else {
							// My enemy is the closest enemy (or there is no
							// enemy)
							BotInfo closest = null;
							Enumeration<BotInfo> enemies = bot.botsInfo
									.elements();
							double minDistance = Double.MAX_VALUE;
							while (enemies.hasMoreElements()) {
								BotInfo botInfo = enemies.nextElement();
								if (!botInfo.teammate) {
									if (minDistance > botInfo.distance) {
										closest = botInfo;
										minDistance = botInfo.distance;
									}
								}
							}
							bot.enemy = closest;
						}
					}
				}
			}
		}
	}

	public void onPaint(Graphics2D g) {
		if (bot.enemy != null) {
			g.setColor(Color.RED);
			g.drawOval((int) bot.enemy.x - 20, (int) bot.enemy.y - 20, 40, 40);
		}
	}
}
