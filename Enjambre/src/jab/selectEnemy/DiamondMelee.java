package jab.selectEnemy;

import java.util.Iterator;

import jab.module.BotInfo;
import jab.module.Module;
import jab.module.SelectEnemy;

public class DiamondMelee extends SelectEnemy {

	public DiamondMelee(Module bot) {
		super(bot);
	}

	final static int CLOSE_ENEMIES_DISTANCE = 60;
	
	
	public void select() {
		
		BotInfo previousEnemy = bot.enemy;
		bot.enemy=null;
		Iterator<BotInfo> iterator= bot.botsInfo.values().iterator();
		double minDistance= Double.MAX_VALUE;
		BotInfo selected=null;
		while (iterator.hasNext()){
			BotInfo e= iterator.next();
			if (!bot.isTeammate(e.name)){
				if (minDistance>e.distance){
					selected=e;
					minDistance=e.distance;
				}
			}
		}
		if (previousEnemy!=null && selected!=null &&
				!previousEnemy.name.equals(selected.name) &&
				bot.botsInfo.get(previousEnemy.name)!=null &&
				bot.botsInfo.get(selected.name)!=null &&
				Math.abs(bot.botsInfo.get(previousEnemy.name).distance - bot.botsInfo.get(selected.name).distance)<=CLOSE_ENEMIES_DISTANCE 
				){
			bot.enemy = bot.botsInfo.get(previousEnemy.name);
		} else {
			bot.enemy=selected;
		}
		
	}
	
}
