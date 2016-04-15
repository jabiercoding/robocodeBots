package jab.avk.selectEnemy;

import jab.avk.Enemy;
import jab.avk.Module;
import jab.avk.SelectEnemy;

import java.util.Iterator;



public class LastScanned extends SelectEnemy {

	public LastScanned(Module bot) {
		super(bot);
	}

	public void select() {
		Iterator<Enemy> iterator= bot.enemies.values().iterator();
		int maxTime= Integer.MIN_VALUE;
		Enemy selected=null;
		while (iterator.hasNext()){
			Enemy e= iterator.next();
			if (maxTime<e.timeScanned){
				selected=e;
				maxTime=e.timeScanned;
			}				
		}

		bot.enemy=selected;
	}

}
