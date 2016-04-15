package jab.movement;

import jab.module.BotInfo;
import jab.module.Module;
import jab.module.Movement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;


public class AntiGravity extends Movement {

	Vector<GravPoint> gravPoints = new Vector<GravPoint>();
	
	public AntiGravity(Module bot) {
		super(bot);
	}
	
	public void move() {
		
		gravPoints.clear();
		
		// The others
		Enumeration<BotInfo> others = bot.botsInfo.elements();
		while (others.hasMoreElements()){
			BotInfo other = others.nextElement();
			if (!other.name.equals(bot.getName())){
				if (bot.enemy!=null && bot.enemy.name!=null && bot.enemy.name.equals(other.name)){
					
					double myX = bot.getX();
					double myY = bot.getY();
					double enemyX = bot.enemy.x;
					double enemyY = bot.enemy.y;
					double enemyHeading = bot.enemy.headingRadians;
					double enemyVelocity = bot.enemy.velocity;
					
					
					double deltaTime = 0;
					double battleFieldHeight = bot.getBattleFieldHeight(), battleFieldWidth = bot.getBattleFieldWidth();
					double predictedX = enemyX, predictedY = enemyY;
					while((++deltaTime) * 11 < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
						predictedX += Math.sin(enemyHeading) * enemyVelocity;	
						predictedY += Math.cos(enemyHeading) * enemyVelocity;
						if(	predictedX < 18.0 
							|| predictedY < 18.0
							|| predictedX > battleFieldWidth - 18.0
							|| predictedY > battleFieldHeight - 18.0){
							predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
							predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
							break;
						}
					}
					
					
					
					gravPoints.add(new GravPoint(predictedX,predictedY,50000));
				}
				else {
					gravPoints.add(new GravPoint(other.x,other.y,-1000));
				}
			}
		}
		
		
		// Calculate Forces And Move
		double force;
	    double ang;
	    double xforce=0;
	    double yforce=0;
		Iterator<GravPoint> i = gravPoints.iterator();
		while (i.hasNext()){
			GravPoint p = i.next();
			force = p.power/Math.pow(getRange(bot.getX(),bot.getY(),p.x,p.y),2);
	        //Find the bearing from the point to us			
			ang = normaliseBearing(Math.PI/2 - Math.atan2(bot.getY() - p.y, bot.getX() - p.x));
	        //Add the components of this force to the total force in their respective directions
	        xforce += Math.sin(ang) * force;
	        yforce += Math.cos(ang) * force;
		}
		
	    /**The following four lines add wall avoidance.  They will only affect us if the bot is close 
	    to the walls due to the force from the walls decreasing at a power 3.**/
	    xforce += 5000/Math.pow(getRange(bot.getX(), bot.getY(), bot.getBattleFieldWidth(), bot.getY()), 3);
	    xforce -= 5000/Math.pow(getRange(bot.getX(), bot.getY(), 0, bot.getY()), 3);
	    yforce += 5000/Math.pow(getRange(bot.getX(), bot.getY(), bot.getX(), bot.getBattleFieldHeight()), 3);
	    yforce -= 5000/Math.pow(getRange(bot.getX(), bot.getY(), bot.getX(), 0), 3);
	    
	    //Move in the direction of our resolved force.
	    goTo(bot.getX()-xforce,bot.getY()-yforce);
	}
	
	
	/**Move towards an x and y coordinate**/
	private void goTo(double x, double y) {
	    double dist = 20; 
	    double angle = Math.toDegrees(absbearing(bot.getX(),bot.getY(),x,y));
	    double ang;
    	int dir;
	    ang = normaliseBearing(bot.getHeading() - angle);
	    if (ang > 90) {
	        ang -= 180;
	        dir = -1;
	    }
	    else if (ang < -90) {
	        ang += 180;
	        dir = -1;
	    }
	    else {
	        dir = 1;
	    }
	    
	    bot.setTurnLeft(ang);
	    
	    if (Math.abs(ang)>=Math.PI/4)
	    	bot.setMaxVelocity(6);
	    else
	    	bot.setMaxVelocity(8);
	    
	    bot.setAhead(dist * dir);
	}
	
	/**Turns the shortest angle possible to come to a heading, then returns the direction the
	the bot needs to move in.**/
//	private int turnTo(double angle) {
//	    double ang;
//    	int dir;
//	    ang = normaliseBearing(bot.getHeading() - angle);
//	    if (ang > 90) {
//	        ang -= 180;
//	        dir = -1;
//	    }
//	    else if (ang < -90) {
//	        ang += 180;
//	        dir = -1;
//	    }
//	    else {
//	        dir = 1;
//	    }
//	    bot.setTurnLeft(ang);
//	    return dir;
//	}
	
	//gets the absolute bearing between to x,y coordinates
	private double absbearing( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = getRange( x1,y1, x2,y2 );
		if( xo > 0 && yo > 0 )
		{
			return Math.asin( xo / h );
		}
		if( xo > 0 && yo < 0 )
		{
			return Math.PI - Math.asin( xo / h );
		}
		if( xo < 0 && yo < 0 )
		{
			return Math.PI + Math.asin( -xo / h );
		}
		if( xo < 0 && yo > 0 )
		{
			return 2.0*Math.PI - Math.asin( -xo / h );
		}
		return 0;
	}
	
	//returns the distance between two x,y coordinates
	private double getRange( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = Math.sqrt( xo*xo + yo*yo );
		return h;	
	}
	
	//if a bearing is not within the -pi to pi range, alters it to provide the shortest angle
	private double normaliseBearing(double ang) {
		if (ang > Math.PI)
			ang -= 2*Math.PI;
		if (ang < -Math.PI)
			ang += 2*Math.PI;
		return ang;
	}
	
	
	public void onPaint(Graphics2D g){
		g.setColor(Color.WHITE);
		Iterator<GravPoint> i = gravPoints.iterator();
		while (i.hasNext()){
			GravPoint point= i.next();
			g.fillOval((int)point.x-10, (int)point.y-10, 20, 20);
			g.drawOval((int)point.x-20, (int)point.y-20, 40, 40);
			g.drawOval((int)point.x-40, (int)point.y-40, 80, 80);
			g.drawOval((int)point.x-80, (int)point.y-80, 160, 160);
		}
	}
	
}
