package jab.avk.movement;

import jab.avk.Module;
import jab.avk.Movement;

public class Quiet extends Movement {

	public Quiet(Module bot) {
		super(bot);
	}

	public void move() {
		bot.setAhead(0.0001);
	}

}