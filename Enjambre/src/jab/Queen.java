package jab;

import jab.gun.*;
import jab.module.Gun;
import jab.module.Module;
import jab.module.Movement;
import jab.module.Radar;
import jab.module.SelectEnemy;
import jab.module.Special;
import jab.module.Targeting;
import jab.movement.*;
import jab.radar.*;
import jab.selectEnemy.*;
import jab.special.AssignationManager;
import jab.targeting.*;

import java.awt.Color;

public class Queen extends Module {

	protected void initialize() {
		setBodyColor(Color.BLACK);
		setGunColor(Color.YELLOW);
		setRadarColor(Color.BLACK);
		setScanColor(Color.YELLOW);
		setBulletColor(Color.YELLOW);
	}

	// All the used parts
	SelectEnemy diamondMelee = new DiamondMelee(this);
	SelectEnemy atackTheLeader = new AtackTheLeader(this);
	Radar spinningRadar = new SpinningRadar(this);
	Radar smartSpinningRadar = new SmartSpinningRadar(this);
	Radar wideLock = new WideLock(this);
	Gun ceaseFire = new CeaseFire(this);
	Gun maximum = new Maximum(this);
	Targeting guessFactor = new GuessFactorMelee(this);
	Targeting linear = new LinearTargeting(this);
	Targeting headOn = new HeadOnTargeting(this);
	Targeting quietGun = new QuietGun(this);
	Movement quiet = new Quiet(this);
	Movement diamondMovement = new DiamondMovement(this);
	Special assignationManager = new AssignationManager(this);

	protected void selectBehavior() {
		
		// Radar
		if (isOneOnOneBattle()) {
			radar = wideLock;
		} else {
			if (!isAllScannedAtLeastOnce()) {
				radar = spinningRadar;
			} else {
				if (getCurrentNumberOfEnemies() == 1) {
					radar = wideLock;
				} else {
					radar = smartSpinningRadar;
				}
			}
		}

		// Enemies: One leader and all droids
		if (getEnemiesLeader() != null && getCurrentNumberDroidEnemies() == getCurrentNumberOfEnemies() - 1) {
			selectEnemy = atackTheLeader;
			deactivate(assignationManager);
		} else {
			selectEnemy = diamondMelee;
			activate(assignationManager);
		}

		movement = diamondMovement;

		if (enemy != null && getCurrentRoundScannedEnemies() > 0) {
			targeting = guessFactor;
			gun = maximum;
		} else {
			targeting = quietGun;
			gun = ceaseFire;
		}
	}
}
