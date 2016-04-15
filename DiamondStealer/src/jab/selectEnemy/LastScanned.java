package jab.selectEnemy;

import jab.module.BotInfo;
import jab.module.Module;
import jab.module.SelectEnemy;

import java.util.Iterator;



public class LastScanned extends SelectEnemy {

	public LastScanned(Module bot) {
		super(bot);
	}

	public void select() {
		Iterator<BotInfo> iterator= bot.botsInfo.values().iterator();
		int maxTime= Integer.MIN_VALUE;
		BotInfo selected=null;
		while (iterator.hasNext()){
			BotInfo e= iterator.next();
			if (maxTime<e.timeScanned){
				selected=e;
				maxTime=e.timeScanned;
			}				
		}

		bot.enemy=selected;
	}

}
