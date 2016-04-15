package jab;

import jab.gun.*;
import jab.module.Gun;
import jab.module.Module;
import jab.module.Movement;
import jab.module.Radar;
import jab.module.SelectEnemy;
import jab.module.Targeting;
import jab.movement.*;
import jab.radar.*;
import jab.selectEnemy.*;
import jab.targeting.*;

import java.awt.Color;



public class DiamondStealer extends Module {

	protected void initialize() {
		this.setColors(Color.black, Color.black, Color.black);
	}

	// All the used parts
	SelectEnemy diamondMelee = new DiamondMelee(this);
	SelectEnemy atackTheLeader = new AtackTheLeader(this);
	Radar spinningRadar = new SpinningRadar(this);
	Radar smartSpinningRadar = new SmartSpinningRadar(this);
	Radar wideLock = new WideLock(this);
	Radar superWideLock = new SuperWideLock(this);
	Gun ceaseFire = new CeaseFire(this);
	Gun maximum = new Maximum(this);
	Targeting guessFactor = new GuessFactorMelee(this);
	Targeting linear = new LinearTargeting(this);
	Targeting headOn = new HeadOnTargeting(this);
	Targeting quietGun = new QuietGun(this);
	Movement quiet = new Quiet(this);
	Movement diamondMovement = new DiamondMovement(this);
	
	boolean allScanned = false;
	
	protected void selectBehavior() {
		// Radar
		
		if (!allScanned && getCurrentNumberOfEnemies()==getNumberOfEnemies()){
			allScanned = true;
		}
		
		//System.out.println(getCurrentNumberOfEnemies()+" vs us "+(getCurrentNumberOfTeamMates() + 1) + " teammates: " + getTeammates().length + " others"+getOthers());
		// 1vs1
		if (getCurrentNumberOfEnemies()==1 && getOthers()==1){
			radar=wideLock;
		}
		// Teams in superiority
		else if (getTeammates()!=null && getCurrentNumberOfEnemies()<=getCurrentNumberOfTeamMates() + 1) {
			radar=superWideLock;
		}
		// Teams in inferiority or Melee
		else if (getTeammates()!=null || allScanned){
			radar = smartSpinningRadar;
		}
		else {
			radar = spinningRadar;
		}
		
		// One leader and all droids
		if (getLeader()!=null && getCurrentNumberDroids()==getCurrentNumberOfEnemies()-1){
			selectEnemy = atackTheLeader;
		} else {
			selectEnemy = diamondMelee;
		}

		movement = diamondMovement;
		
		if (enemy!=null && getCurrentRoundScannedEnemies() > 0) {
			targeting = guessFactor;
			gun = maximum;
		} else {
			targeting = quietGun;
			gun = ceaseFire;
		}
	}
}
