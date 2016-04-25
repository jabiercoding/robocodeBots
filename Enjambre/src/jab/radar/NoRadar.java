package jab.radar;

import jab.module.Module;
import jab.module.Radar;

public class NoRadar extends Radar {

	public NoRadar(Module bot) {
		super(bot);
	}

	public void scan() {
		// do nothing
	}

}
