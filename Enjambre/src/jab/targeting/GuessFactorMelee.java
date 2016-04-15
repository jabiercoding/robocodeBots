package jab.targeting;

import jab.module.BotInfo;
import jab.module.Module;
import jab.module.Targeting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import robocode.Event;
import robocode.MessageEvent;
import robocode.util.Utils;

/**
 * GuessFactorMelee
 * @author Jabi
 */
public class GuessFactorMelee extends Targeting {

	final static double BOT_WIDTH= 36;
	
	public GuessFactorMelee(Module bot) {
		super(bot);
	}
	
	final static int MAX_VISITS = 255;
	final static int VARIABLE_BINS = 11;//11;
	final static int TARGETING_BINS = 101;
	final static double MAX_ESCAPE_ANGLE = 1;//0.7;
	final static double MAX_DISTANCE = 800;
	
	static int[][][][][] brain;
	int bestOffset = TARGETING_BINS/2;
	int maxVisits = 0;
	
	int var1_Distance;
	int var2_LateralVelocity;
	int var3_DistanceToWalls;

	Vector<Wave> waves = new Vector<Wave>();
	
	/**
	 * Module Part Target
	 */
	@SuppressWarnings("static-access")
	public void target() {
		if (brain == null){
			brain = new int [bot.totalNumOfEnemies][VARIABLE_BINS][VARIABLE_BINS][VARIABLE_BINS][TARGETING_BINS];
		}
	
		
		Enumeration<Wave> waveEnum = waves.elements();
		while (waveEnum.hasMoreElements()) {
			Wave wave = waveEnum.nextElement();
			wave.updateWave();
			BotInfo waveEnemy = bot.botsInfo.get(wave.enemyName);
			if (waveEnemy==null){
				waves.remove(wave);
			}
			else{
			if (wave.isOnTheCrest(waveEnemy.getBotRectangle())) {
				double thetaFireTime = Utils.normalAbsoluteAngle(Math
						.atan2(wave.enemy.x - wave.origin.x, wave.enemy.y
								- wave.origin.y));
				double thetaBreak = Utils.normalAbsoluteAngle(Math.atan2(
						waveEnemy.x - wave.origin.x, waveEnemy.y
								- wave.origin.y));
				double offset = Utils.normalRelativeAngle(thetaFireTime
						- thetaBreak);				
				int targeting_offset=(int)((offset+MAX_ESCAPE_ANGLE)/(MAX_ESCAPE_ANGLE*2/(TARGETING_BINS-1)));
				// Out of bounds prevention
				//System.out.println(targeting_offset);
				targeting_offset = betweenMinMax(targeting_offset, 0, TARGETING_BINS-1);				
				wave.offsets.add(targeting_offset);

			} else if (wave.isOut(waveEnemy.getBotRectangle())){
				// The wave was on the crest but now is out.
				updateBrain(wave);			
				waves.remove(wave);
			}
		}
		}
		
		if (bot.enemy!=null){
			//System.out.println("Current Enemy: "+bot.enemy.name +","+bot.botsInfo.get(bot.enemy.name));
			calculateSegments(bot.botsInfo.get(bot.enemy.name));
			getTheMostVisitedTargetingBin();
		}

		// I only store data if I'm firing because robots use to detect that
		// I'm firing and change their behavior.
		// I always fire when the gun is cold enough and I'm not disabled.
		if (bot.getGunHeat() <= 0.1 && bot.getEnergy()>0) {
			//System.out.println("My prediction for this DataSet("+bot.getEnemyAssignedNum(bot.enemy.name)+","+var1_Distance+","+var2_LateralVelocity+","+var3_DistanceToWalls+")"+"->"+bestOffset + "  Visits: "+maxVisits);
			throwWave();
		}
		
		
		
		
		if (maxVisits != 0) {
			double absoluteBearing = bot.getHeadingRadians()
					+ bot.enemy.bearingRadians
					- ((bestOffset * MAX_ESCAPE_ANGLE * 2 / (TARGETING_BINS-1)) - MAX_ESCAPE_ANGLE);
			bot.setTurnGunRightRadians(robocode.util.Utils
					.normalRelativeAngle(absoluteBearing
							- bot.getGunHeadingRadians()));
		}
		else {
			new CircularTargeting(bot).target();
		}
		

		
		
	}

	/**
	 * Get the most visited targeting bin for the next bullet
	 */
	private void getTheMostVisitedTargetingBin(){
		int[] offsetVisits = new int[TARGETING_BINS];
		offsetVisits= brain[bot.getEnemyAssignedNum(bot.enemy.name)][var1_Distance][var2_LateralVelocity][var3_DistanceToWalls].clone();
		
		// Same bots in a team
		Enumeration<BotInfo> botsInfoEnum = bot.botsInfo.elements();
		while (botsInfoEnum.hasMoreElements()){
			BotInfo botInfo = botsInfoEnum.nextElement();
			if (!bot.enemy.name.equals(botInfo.name) && bot.isTheSameBot(bot.enemy.name, botInfo.name)){
				for (int i=0;i<TARGETING_BINS;i++){
					offsetVisits[i]+=brain[bot.getEnemyAssignedNum(botInfo.name)][var1_Distance][var2_LateralVelocity][var3_DistanceToWalls][i];
				}
			}
		}
		
		maxVisits=0;
		bestOffset=TARGETING_BINS/2;
		for (int i=0;i<TARGETING_BINS;i++){
			if (maxVisits<offsetVisits[i]){
				maxVisits=offsetVisits[i];
				bestOffset=i;
			}
		}
	}
	
	/**
	 * Module Part Listening
	 */
	public void listen(Event e) {

		if (e instanceof MessageEvent){
			MessageEvent event = (MessageEvent)e;
			if (event.getMessage() instanceof Visits){
				Visits visits = (Visits)event.getMessage();
				//System.out.println("Visits:"+visits.enemyNum+","+visits.var1+","+visits.var2+","+visits.var3+" ->"+visits.vector);
				for (int i = 0; i<TARGETING_BINS; i++){
					brain[visits.enemyNum][visits.var1][visits.var2][visits.var3][i] += visits.vector[i];
				}
			}
		}
	}

	
	/**
	 * Throw a wave
	 */
	private void throwWave(){
		waves.add(new Wave(bot.enemy.name,new Point2D.Double(bot.getX(), bot.getY()),
				robocode.Rules.getBulletSpeed(bot.bulletPower),
				new Point2D.Double(bot.enemy.x, bot.enemy.y),
				new int[] { var1_Distance,
						var2_LateralVelocity,
						var3_DistanceToWalls }));
		
		// Enemies of a team with the same name
		// TODO variables!!!!!!!!!
		Enumeration<BotInfo> botsInfo = bot.botsInfo.elements();
		while (botsInfo.hasMoreElements()){
			BotInfo botInfo = botsInfo.nextElement();
			if (!bot.isTeammate(botInfo.name) && !botInfo.name.equals(bot.enemy.name) && bot.isTheSameBot(bot.enemy.name, botInfo.name)){
				calculateSegments(botInfo);
				waves.add(new Wave(botInfo.name,new Point2D.Double(bot.getX(), bot.getY()),
						robocode.Rules.getBulletSpeed(bot.bulletPower),
						botInfo,
						new int[] { var1_Distance,
								var2_LateralVelocity,
								var3_DistanceToWalls }));
			}
		}
	}
	
	
	/**
	 * Calculate the variables for segmentation
	 */
	private void calculateSegments(BotInfo botInfo){
		//* Distance
		var1_Distance = (int) ((Math.min(MAX_DISTANCE, botInfo.distance) / (MAX_DISTANCE/(VARIABLE_BINS-1))));
		var1_Distance = betweenMinMax(var1_Distance, 0, VARIABLE_BINS-1);				
		
		//* Lateral velocity
		double latvel= botInfo.velocity * Math.sin(botInfo.headingRadians - (botInfo.bearingRadians + bot.getHeadingRadians()));
		latvel= latvel + 8;
		var2_LateralVelocity = (int)((latvel/16)*(VARIABLE_BINS-1));
		var2_LateralVelocity = betweenMinMax(var2_LateralVelocity, 0, VARIABLE_BINS-1);
		
		//* Distance to walls
		var3_DistanceToWalls = VARIABLE_BINS-1;
		double goX = bot.getBattleFieldWidth() / ((VARIABLE_BINS-1)*2);
		double goY = bot.getBattleFieldHeight() / ((VARIABLE_BINS-1)*2);
		double x = bot.getBattleFieldWidth() / 2, y = bot
				.getBattleFieldHeight() / 2, w = 0, h = 0;
		while (!(new Rectangle2D.Double(x, y, w, h))
				.contains(new Point2D.Double(botInfo.x, botInfo.y))) {
			x = x - goX;
			y = y - goY;
			w = w + goX * 2;
			h = h + goY * 2;
			var3_DistanceToWalls--;
		}
		var3_DistanceToWalls = betweenMinMax(var3_DistanceToWalls, 0, VARIABLE_BINS-1);
	}
	

	
	/**
	 * Update Brain with the results of a wave
	 * @param wave
	 */
	private void updateBrain(Wave wave){
		// Only add one visit per offset between min and max.
		// Limit file size.
//		System.out.println("The enemy surfed our wave. New Data:");
		
//		for (int i = 0; i < wave.offsets.size(); i++) {
//			int current = wave.offsets.get(i);
//			System.out.println("Offset: "+current);
//		}
		//TODO exception
		//java.lang.ArrayIndexOutOfBoundsException: 2147483647
		//java.lang.ArrayIndexOutOfBoundsException: 2147483647
		//    at jab.targeting.GuessFactorMelee.updateBrain(GuessFactorMelee.java:303)
		int minOffset = Integer.MAX_VALUE;
		int maxOffset = Integer.MIN_VALUE;
		Visits visits = new Visits();
		visits.enemyNum = bot.getEnemyAssignedNum(wave.enemyName);
		visits.var1 = wave.vars[0];
		visits.var2 = wave.vars[1];
		visits.var3 = wave.vars[2];

		for (int i = 0; i < wave.offsets.size(); i++) {
			int current = wave.offsets.get(i);
			
			if (current <= minOffset){
				minOffset = current;
			}
			if (current >= maxOffset){
				maxOffset = current;
			}
			
			// Calculate visits for each offset
			if (i==0) {
				visits.vector[current]++;
			}
			if (i < wave.offsets.size()-1) {
				int next = wave.offsets.get(i+1);
				if (current==next){
					visits.vector[current]++;
				}else if (current<next){
					for (int w = current+1; w<=next && w<TARGETING_BINS; w++){
						visits.vector[w]++;
					}
				}else { // current>next
					for (int w = current-1; w>=next && w>=0; w--){
						visits.vector[w]++;
					}
				}
			}
		}
		
		// Adding
		int maxVisit= Integer.MIN_VALUE;
		int maxVisitIndex = 0;
		for (int i=0; i<TARGETING_BINS; i++){
			if (maxVisit<visits.vector[i]) {
				maxVisit= visits.vector[i];
				maxVisitIndex = i;
			}
			if (visits.vector[i]>2) {
				visits.vector[i]=2;
			}
			if (visits.vector[i]!=0) {
				//System.out.println("  Adding: "+i +" Number: "+visits.vector[i]);
				brain[visits.enemyNum][visits.var1][visits.var2][visits.var3][i] += visits.vector[i];
			}
		}
		
		// Adding Extra
		if (maxVisit>1){
			//System.out.println("  Adding Max Extra to: "+maxVisitIndex +" MaxVisits: "+visits.vector[maxVisitIndex]);
			brain[bot.getEnemyAssignedNum(wave.enemyName)][wave.vars[0]][wave.vars[1]][wave.vars[2]][maxVisitIndex] ++;
			visits.vector[maxVisitIndex]++;
		}
		else if (maxVisit==1){
			//System.out.println("  Adding Extra to Middle Point: "+ (minOffset + ((maxOffset-minOffset)/2)));
			brain[bot.getEnemyAssignedNum(wave.enemyName)][wave.vars[0]][wave.vars[1]][wave.vars[2]][(minOffset + ((maxOffset-minOffset)/2))] ++;
			visits.vector[(minOffset + ((maxOffset-minOffset)/2))]++;
		}
		
		try {
			bot.broadcastMessage(visits);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * A stitch in time saves nine.
	 * Index out of bounds prevention.
	 */
	private static int betweenMinMax(int number, int min, int max){
		int aux=number;
		aux = Math.min(max, Math.max(min, number));
		if (aux!=number)
			System.out.println("ERROR: "+number+" was out of bounds! Min: "+min+" Max: "+max);
		return aux;
	}
    

	
	/**
	 * Draw the waves
	 */
	public void onPaint(Graphics2D g) {
		
		Enumeration<Wave> e = waves.elements();
		while (e.hasMoreElements()) {
			Wave wave = e.nextElement();
			if (bot.botsInfo.get(wave.enemyName)!= null && wave.isOnTheCrest(bot.botsInfo.get(wave.enemyName).getBotRectangle()))
				g.setColor(Color.WHITE);
			else
				g.setColor(Color.BLUE);			
			g.draw(wave);
		}
	}
	
    
	
	/**
	 * Wave
	 */
	public class Wave extends Ellipse2D.Double {
		private static final long serialVersionUID = 1L;
		
		public String enemyName;
		public double time;
		public Point2D.Double origin;
		public double velocity;
		public Point2D.Double enemy;
		public int[] vars;
		Vector<Integer> offsets;
		
		public Wave(String enemyName, Point2D.Double origin, double velocity, Point2D.Double enemy, int[] vars) {
			this.enemyName = enemyName;
			this.origin = origin;
			this.velocity = velocity;
			this.time = 0;
			this.enemy = enemy;
			this.vars = vars;
			this.x = origin.x - BOT_WIDTH/2;
			this.y = origin.y - BOT_WIDTH/2;
			offsets = new Vector<Integer>();
		}

		public void updateWave() {
			time++;
			double travelledDistance = time * velocity;
			x = origin.x - travelledDistance;
			y = origin.y - travelledDistance;
			width = height = travelledDistance * 2;
		}
		
		public boolean isOnTheCrest(Rectangle2D.Double enemyRect) {
			return intersects(enemyRect) && !isOut(enemyRect);
		}
		
		public boolean isOut (Rectangle2D.Double enemyRect) {
			Point2D.Double bottomLeft = new Point2D.Double(enemyRect.x,enemyRect.y);
			Point2D.Double bottomRight = new Point2D.Double(enemyRect.x + enemyRect.width,enemyRect.y);
			Point2D.Double upLeft = new Point2D.Double(enemyRect.x,enemyRect.y + enemyRect.height);
			Point2D.Double upRight = new Point2D.Double(enemyRect.x + enemyRect.width,enemyRect.y + enemyRect.height);
			return contains(bottomLeft) && contains(bottomRight) && contains(upLeft) && contains(upRight);
		}
	}

}
