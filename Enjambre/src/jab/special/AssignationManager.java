package jab.special;


import jab.module.BotInfo;
import jab.module.Module;
import jab.module.Special;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Enumeration;

public class AssignationManager extends Special {
	
	BotInfo[] we;
	BotInfo[] they;
	double minGlobalDistance;
	BotInfo[] minGlobalDistancePermutation;
	
	
	
	public AssignationManager(Module bot){
		super(bot);
	}
	
	public void doIt(){
		minGlobalDistance= Double.MAX_VALUE;
		minGlobalDistancePermutation= new BotInfo[5];
		
		we = new BotInfo[5];
		we[0] = bot.botsInfo.get(bot.getTeammates()[0]);
		we[1] = bot.botsInfo.get(bot.getTeammates()[1]);
		we[2] = bot.botsInfo.get(bot.getTeammates()[2]);
		we[3] = bot.botsInfo.get(bot.getTeammates()[3]);
		we[4] = bot.botsInfo.get(bot.getName());
		
		int x= 0;
		they = new BotInfo[5];
		Enumeration<BotInfo> others = bot.botsInfo.elements();
		while (others.hasMoreElements()){
			BotInfo other = others.nextElement();
			if (!other.teammate){
				if (other.leader)
					they[4] = other;
				else {
					they[x] = other;
					x++;
				}
			}
		}
		
		permute(they,0,they.length-1);
		
		// Now we have the minGlobalDistancePermutation
	
		String[] assignations = new String[8];
		for (int i=0; i<=3; i++){
			if ( minGlobalDistancePermutation[i]!=null)
				assignations[i]= minGlobalDistancePermutation[i].name;
		}
		for (int a=4; a<=7; a++){
			assignations[a]=bot.getTeammates()[a-4];
		}
		
		try {
			bot.broadcastMessage(assignations);
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
	
	
	//returns the distance between two x,y coordinates
	private double getDistance( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		return Math.sqrt( xo*xo + yo*yo );
	}
	
	private double getGlobalDistance(BotInfo[] enemies){
		double global=0;
		for (int i=0; i<=4; i++){
			if (enemies[i]!=null && we[i]!=null)
				global+=getDistance(enemies[i].x,enemies[i].y,we[i].x,we[i].y);
			else global+=100000000;
		}		
		return global;
	}
	
	/** 
	 * Llama al método doSomething() con todas las permutaciones
	 * posibles del conjunto de enteros ps.
	 *
	 * Llamada inicial: permute(ps, 0, ps.length);
	 *
	 * @param bots Array de enteros, conjunto de elementos a
	 * permutar.
	 * @param start Índice de inicio de la permutación.
	 * @param n Número de elementos de ps.
	 */
	public /*static*/ void permute(BotInfo[] bots, int start, int n) {
	    //doSomething(ps);
		
		double globalDistance= getGlobalDistance(bots);
		if (minGlobalDistance>globalDistance){
			minGlobalDistance=globalDistance;
			minGlobalDistancePermutation=bots.clone();
		}
		
	    BotInfo tmp;

	    if (start < n) {
	      for (int i = n - 2; i >= start; i--) {
	        for (int j = i + 1; j < n; j++) {
	          // swap ps[i] <--> ps[j]
	          tmp = bots[i];
	          bots[i] = bots[j];
	          bots[j] = tmp;

	          permute(bots, i + 1, n);
	        }

	        // Undo all modifications done by
	        // recursive calls and swapping
//	        tmp = bots[i];
//	        for (int k = i; k < n - 1;)
//	          bots[k] = bots[++k];
//	        bots[n - 1] = tmp;
	      }
	    }
	  }
	
	public void onPaint(Graphics2D g){
		for (int i=0; i<=3; i++){
			g.drawString(i+" -> ", 170, 200+(i*20));
			g.drawString((we[i]==null?"null":we[i].name).concat(" <--> " + ((minGlobalDistancePermutation[i]==null)?"null":minGlobalDistancePermutation[i].name)), 200, 200+(i*20));
			if (we[i]!=null && minGlobalDistancePermutation[i]!=null)
					g.drawLine((int)we[i].x,(int)we[i].y, (int)minGlobalDistancePermutation[i].x,(int)minGlobalDistancePermutation[i].y);
		}
	};
}
