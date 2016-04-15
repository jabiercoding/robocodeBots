package jab.radar;

import jab.module.Module;
import jab.module.Radar;

import java.io.IOException;

import robocode.Event;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class SuperWideLock extends Radar {
	
	public String[] radarAssignations;
	
	public SuperWideLock(Module bot) {
		super(bot);
		
	}
	
	private int timeSinceLastScan = 10;
	private double enemyAbsoluteBearing;
	
	
	
	public void scan(){
		if (radarAssignations == null){
			radarAssignations = new String[bot.getTeammates().length];
		}
		timeSinceLastScan++;
		double radarOffset = Double.NEGATIVE_INFINITY;
		if (timeSinceLastScan < 3) {
			radarOffset = Utils
					.normalRelativeAngle(bot.getRadarHeadingRadians()
							- enemyAbsoluteBearing);
			radarOffset += Math.signum(radarOffset) * 0.2;
		}
		bot.setTurnRadarLeftRadians(radarOffset);
		//printAssignations();
	}
	
	public void listen(Event e){
		
		if (e instanceof MessageEvent){
			MessageEvent message = ((MessageEvent)e);
			if (message.getMessage() instanceof RadarAssignation){
				RadarAssignation assign = (RadarAssignation)message.getMessage();
				radarAssignations[getMateNum(message.getSender())]=assign.focusOnEnemy;
			}
		}
		
		else if (e instanceof ScannedRobotEvent){
			String scannedName = ((ScannedRobotEvent)e).getName();
			if (!bot.isTeammate(scannedName) && !isAssigned(scannedName)){
				RadarAssignation radarAssignation = new RadarAssignation();
				radarAssignation.focusOnEnemy = scannedName;
				try {
					bot.broadcastMessage(radarAssignation);
				} catch (IOException e1) {}
				enemyAbsoluteBearing = bot.getHeadingRadians() + ((ScannedRobotEvent)e).getBearingRadians();
				timeSinceLastScan = 0;
			}
		}
		
		else if (e instanceof RobotDeathEvent){
			String deathName = ((RobotDeathEvent)e).getName();
			if (bot.isTeammate(deathName)){
				radarAssignations[getMateNum(deathName)]=null;
			}
		}
	}
	
	boolean isAssigned(String enemyName){
		for (int x=0; x<radarAssignations.length; x++){
			if (radarAssignations[x]!=null && radarAssignations[x].equals(enemyName)){
				return true;
			}
		}
		return false;
	}
	
	int getMateNum(String name){
		String[] mates = bot.getTeammates();
		for (int x=0; x<mates.length; x++){
			if (mates[x].equals(name)){
				return x;
			}
		}
		return 0;
	}
	
	void printAssignations(){
		System.out.println("_________");
		for (int x=0; x<radarAssignations.length; x++){
			System.out.print(radarAssignations[x]+" , ");
		}System.out.println("");
		System.out.println("_________");
	}
	
}
