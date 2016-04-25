package jab.selectEnemy;

import jab.module.Module;
import jab.module.SelectEnemy;

public class AtackTheLeader extends SelectEnemy {

	public AtackTheLeader(Module bot) {
		super(bot);
	}

	public void select() {
		bot.enemy = bot.getEnemiesLeader();
	}

}
