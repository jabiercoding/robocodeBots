package jab.avk.special;

import java.awt.Color;
import java.awt.Graphics2D;

import robocode.DeathEvent;
import robocode.Event;
import robocode.HitRobotEvent;
import robocode.WinEvent;
import jab.avk.Module;
import jab.avk.Special;

public class RamBotFlag extends Special {
	
	public RamBotFlag(Module bot) {
		super(bot);
	}

	public static boolean isRamBot;
	
	private static int ram=0;
	
	public void doIt() {
		isRamBot = ram >= (bot.getRoundNum()+1) * 5 && bot.enemy!=null && bot.enemy.distance < 250;
	}
	
	public void listen(Event e){
		if (e instanceof HitRobotEvent){
			if (!((HitRobotEvent)e).isMyFault()){
				ram++;
			}
		}
		else if (e instanceof WinEvent || e instanceof DeathEvent){
			ram=Math.min(ram,(bot.getRoundNum()+2) * 5);
		}
	};
	
	public void onPaint(Graphics2D g){
		g.setColor(Color.white);
		g.drawString("Enemy is a RamBot: "+isRamBot+" Ram: "+ram+" times", (int)bot.getBattleFieldWidth()/2, (int)bot.getBattleFieldHeight()/2);
	};
}
