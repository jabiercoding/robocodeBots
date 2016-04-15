package jab.avk.targeting;

import jab.avk.Module;
import jab.avk.Targeting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import robocode.DeathEvent;
import robocode.Event;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

/**
 * GuessFactor
 * @author Jabi
 */
public class GuessFactor extends Targeting {

	final static double BOT_WIDTH= 36;
	Rectangle2D.Double enemyRect = new Rectangle2D.Double(0,0,BOT_WIDTH,BOT_WIDTH);
	
	public GuessFactor(Module bot) {
		super(bot);
	}
	
	final static int MAX_VISITS = 255;
	final static int VARIABLE_BINS = 11;
	final static int TARGETING_BINS = 101;
	final static double MAX_ESCAPE_ANGLE = 1;//0.7;
	final static double MAX_DISTANCE = 800;
	
	static int[][][][] brain = new int [VARIABLE_BINS][VARIABLE_BINS][VARIABLE_BINS][TARGETING_BINS];
	int bestOffset = TARGETING_BINS/2;
	int maxVisits = 0;
	
	int var1_Distance;
	int var2_LateralVelocity;
	int var3_DistanceToWalls;

	Vector<Wave> waves = new Vector<Wave>();

	static int wins = 0;
	boolean brainRestored = false;
	
	/**
	 * Module Part Target
	 */
	public void target() {
		enemyRect.x= bot.enemy.x - BOT_WIDTH/2;
		enemyRect.y= bot.enemy.y - BOT_WIDTH/2;
		
		if (maxVisits != 0) {
			double absoluteBearing = bot.getHeadingRadians()
					+ bot.enemy.bearingRadians
					- ((bestOffset * MAX_ESCAPE_ANGLE * 2 / TARGETING_BINS) - MAX_ESCAPE_ANGLE);
			bot.setTurnGunRightRadians(robocode.util.Utils
					.normalRelativeAngle(absoluteBearing
							- bot.getGunHeadingRadians()));
		}
		else {
			new LinearTargeting(bot).target();
		}
	}

	static boolean newEnemy =false;
	
	/**
	 * Module Part Listening
	 */
	public void listen(Event e) {
		
		if (e instanceof WinEvent){
			wins++;
		}
		
		if ((bot.getRoundNum()+1 == bot.getNumRounds()) && (e instanceof WinEvent || e instanceof DeathEvent)){
			double winPercentage = ((double)wins / (double)bot.getNumRounds())*100;
			System.out.println("Win Percentage: "+winPercentage);
			if (winPercentage < 70 || brainRestored)
				saveBrain();
		}
		
		else if (e instanceof ScannedRobotEvent) {
			
			if (!newEnemy && bot.enemy.name!=null){
				newEnemy=true;
				restoreBrain();
			}
			
			Enumeration<Wave> waveEnum = waves.elements();
			while (waveEnum.hasMoreElements()) {
				Wave wave = waveEnum.nextElement();
				wave.updateWave();
				if (wave.isOnTheCrest(enemyRect)) {
					double thetaFireTime = Utils.normalAbsoluteAngle(Math
							.atan2(wave.enemy.x - wave.origin.x, wave.enemy.y
									- wave.origin.y));
					double thetaBreak = Utils.normalAbsoluteAngle(Math.atan2(
							bot.enemy.x - wave.origin.x, bot.enemy.y
									- wave.origin.y));
					double offset = Utils.normalRelativeAngle(thetaFireTime
							- thetaBreak);				
					int targeting_offset=(int)((offset+MAX_ESCAPE_ANGLE)/(MAX_ESCAPE_ANGLE*2/(TARGETING_BINS-1)));
					// Out of bounds prevention
					targeting_offset = betweenMinMax(targeting_offset, 0, TARGETING_BINS-1);				
					wave.offsets.add(targeting_offset);

				} else if (wave.isOut(enemyRect)){
					// The wave was on the crest but now is out.
					updateBrain(wave);			
					waves.remove(wave);
				}
			}

			
			//Calculate vars
			calculateSegments();
			

			// Get the most visited targeting bin for the next bullet
			maxVisits=0;
			bestOffset=TARGETING_BINS/2;
			for (int i=0;i<TARGETING_BINS;i++){
				if (maxVisits<brain[var1_Distance][var2_LateralVelocity][var3_DistanceToWalls][i]){
					maxVisits=brain[var1_Distance][var2_LateralVelocity][var3_DistanceToWalls][i];
					bestOffset=i;
				}
			}
		
			// I only store data if I'm firing because robots use to detect that
			// I'm firing and change their behavior.
			// I always fire when the gun is cold enough and I'm not disabled.
			if (bot.getGunHeat() == 0 && bot.getEnergy()>0) {
				System.out.println("My prediction for this DataSet("+var1_Distance+","+var2_LateralVelocity+","+var3_DistanceToWalls+")"+"-> "+bestOffset + "    Visits: "+maxVisits);
				throwWave();
			}
		}
	}

	
	/**
	 * Throw a wave
	 */
	private void throwWave(){
		waves.add(new Wave(new Point2D.Double(bot.getX(), bot.getY()),
				robocode.Rules.getBulletSpeed(bot.bulletPower),
				new Point2D.Double(bot.enemy.x, bot.enemy.y),
				new int[] { var1_Distance,
						var2_LateralVelocity,
						var3_DistanceToWalls }));
	}
	
	
	/**
	 * Calculate the variables for segmentation
	 */
	private void calculateSegments(){
		//* Distance
		var1_Distance = (int) ((Math.min(MAX_DISTANCE, bot.enemy.distance) / (MAX_DISTANCE/(VARIABLE_BINS-1))));
		var1_Distance = betweenMinMax(var1_Distance, 0, VARIABLE_BINS-1);				
		
		//* Lateral velocity
		double latvel= bot.enemy.velocity * Math.sin(bot.enemy.headingRadians - (bot.enemy.bearingRadians + bot.getHeadingRadians()));
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
				.contains(new Point2D.Double(bot.enemy.x, bot.enemy.y))) {
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
		System.out.println("The enemy surfed our wave. New Data:");
		
		for (int i = 0; i < wave.offsets.size(); i++) {
			int current = wave.offsets.get(i);
			System.out.println("Offset: "+current);
		}
		
		int minOffset = Integer.MAX_VALUE;
		int maxOffset = Integer.MIN_VALUE;
		int visits[] = new int[TARGETING_BINS];
		for (int i = 0; i < wave.offsets.size(); i++) {
			int current = wave.offsets.get(i);
			
			// Calculate minOffset and maxOffset
			if (current < minOffset) {
				if (current > maxOffset) {
					maxOffset = current;
				}
				minOffset = current;
			} else if (current > maxOffset){
				maxOffset = current;		
			}
			if (maxOffset==Integer.MIN_VALUE){
				maxOffset=minOffset;
			}
			
			// Calculate visits for each offset
			if (i==0) {
				visits[current]++;
			}
			if (i < wave.offsets.size()-1) {
				int next = wave.offsets.get(i+1);
				if (current==next){
					visits[current]++;
				}else if (current<next){
					for (int w = current+1; w<=next && w<TARGETING_BINS; w++){
						visits[w]++;
					}
				}else { // current>next
					for (int w = current-1; w>=next && w>=0; w--){
						visits[w]++;
					}
				}
			}
		}
		
		// Adding
		int maxVisit= Integer.MIN_VALUE;
		int maxVisitIndex = 0;
		for (int i=0; i<TARGETING_BINS; i++){
			if (maxVisit<visits[i]) {
				maxVisit= visits[i];
				maxVisitIndex = i;
			}
			if (visits[i]>2) {
				visits[i]=2;
			}
			if (visits[i]!=0) {
				//System.out.println("  Adding: "+i +" Number: "+visits[i]);
				brain[wave.vars[0]][wave.vars[1]][wave.vars[2]][i] += visits[i];
			}
		}
		
		// Adding Extra
		if (maxVisit>1){
			//System.out.println("  Adding Max Extra to: "+maxVisitIndex +" MaxVisits: "+visits[maxVisitIndex]);
			brain[wave.vars[0]][wave.vars[1]][wave.vars[2]][maxVisitIndex] ++;
		}
		else{ //(max==1)
			//System.out.println("  Adding Extra to Middle Point: "+ (minOffset + ((maxOffset-minOffset)/2)));
			brain[wave.vars[0]][wave.vars[1]][wave.vars[2]][maxVisitIndex] ++;
		}
	}
	
	
	private void deleteBrainOldVersions(){
		System.out.println("Deleting brain old versions \n"+bot.enemy.name);
    	File dir = bot.getDataDirectory();
    	if (dir.exists()){
    		File[] listOfFiles = dir.listFiles();
    	    for (int i = 0; i < listOfFiles.length; i++) {
    	    	String fileName = listOfFiles[i].getName();
    	    	if (fileName.contains("_Movement.zip"))
    	    		fileName = fileName.substring(0,fileName.lastIndexOf("_Movement.zip"));
    	    	else if (fileName.contains("_Targeting.zip"))
    	    		fileName = fileName.substring(0,fileName.lastIndexOf("_Targeting.zip"));
    	    	//System.out.println("->  " + fileName);
    	        if (isOldVersion(bot.enemy.name,fileName)){
    	        	System.out.println("Removing: " + fileName+".zip");
    	        	bot.getDataFile(fileName+".zip").delete();
    	        }
    	    }
    	}
	}
	
	private boolean isOldVersion(String enemy,String data){
		if (!enemy.equals(data) && enemy.contains(" ")){
			enemy = enemy.substring(0,enemy.lastIndexOf(" "));
			data = data.substring(0, data.lastIndexOf(" "));
			if (enemy.equals(data)){
				return true;
			}
		}
		return false;
	}
	
	final static String ZIPENTRY_NAME = "targetingBrain";
	
	/**
	 * Restore Brain
	 */
	private void restoreBrain() {
        try {
        	deleteBrainOldVersions();
            ZipInputStream zipin = new ZipInputStream(new
                FileInputStream(bot.getDataFile(bot.enemy.name + "_Targeting.zip")));
            zipin.getNextEntry();
            ObjectInputStream in = new ObjectInputStream(zipin);
            byte[][][] smallBrain = (byte[][][])in.readObject();
            for (int v1=0; v1<VARIABLE_BINS; v1++){
                for (int v2=0; v2<VARIABLE_BINS; v2++){
                    for (int v3=0; v3<VARIABLE_BINS; v3++){
                    	int offset = smallBrain[v1][v2][v3];
                    	// offset==TARGETING_BINS means that there was no data
                    	if (offset != TARGETING_BINS)
                    		brain[v1][v2][v3][offset]=1;
                    }
                }
            }
            in.close();
            brainRestored = true;
            System.out.println("I know you. Targeting");
        }
        catch (IOException e) {
            System.out.println("Ah! A new aquaintance. I'll be watching you " + bot.enemy.name + ".");
            brain = new int[VARIABLE_BINS][VARIABLE_BINS][VARIABLE_BINS][TARGETING_BINS];
        }
        catch (ClassNotFoundException e) {
            System.out.println("Error reading enemy aim factors:" + e);
        }
    }

	
	/**
	 * Save Brain
	 */
    private void saveBrain() {
        try {
        	ZipOutputStream zipout;
            zipout = new ZipOutputStream(new RobocodeFileOutputStream(bot.getDataFile(bot.enemy.name + "_Targeting.zip")));
            zipout.putNextEntry(new ZipEntry(ZIPENTRY_NAME));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            byte[][][] smallBrain = new byte[VARIABLE_BINS][VARIABLE_BINS][VARIABLE_BINS];
            for (int v1=0; v1<VARIABLE_BINS; v1++){
                for (int v2=0; v2<VARIABLE_BINS; v2++){
                    for (int v3=0; v3<VARIABLE_BINS; v3++){
            			// Get the most visited targeting bin
            			int maxVisits=0;
            			int bestOffset=TARGETING_BINS/2;
            			for (int i=0;i<TARGETING_BINS;i++){
            				if (maxVisits<brain[v1][v2][v3][i]){
            					maxVisits=brain[v1][v2][v3][i];
            					bestOffset=i;
            				}
            			}
            			if (maxVisits==0)
            				// No data
            				smallBrain[v1][v2][v3]=TARGETING_BINS;
            			else
            				smallBrain[v1][v2][v3]=new Integer(bestOffset).byteValue();
                    }
                }
            }
            out.writeObject(smallBrain);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error writing factors:" + e);
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
		g.drawRect((int)enemyRect.x, (int)enemyRect.y, (int)enemyRect.width, (int)enemyRect.height);
		Enumeration<Wave> e = waves.elements();
		while (e.hasMoreElements()) {
			Wave wave = e.nextElement();
			if (wave.isOnTheCrest(enemyRect))
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
		
		public double time;
		public Point2D.Double origin;
		public double velocity;
		public Point2D.Double enemy;
		public int[] vars;
		Vector<Integer> offsets;
		
		public Wave(Point2D.Double origin, double velocity, Point2D.Double enemy, int[] vars) {
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
