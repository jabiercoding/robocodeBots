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

import robocode.Droid;

public class Bee extends Module implements Droid {

	protected void initialize() {
		setBodyColor(Color.BLACK);
		setGunColor(Color.YELLOW);
		setScanColor(Color.YELLOW);
		setBulletColor(Color.YELLOW);
	}

	// All the used parts
	Radar noRadar = new NoRadar(this);
	SelectEnemy diamondMelee = new DiamondMelee(this);
	SelectEnemy atackTheLeader = new AtackTheLeader(this);
	SelectEnemy assigned = new Assigned(this);
	Gun ceaseFire = new CeaseFire(this);
	Gun maximum = new Maximum(this);
	Gun fireWhenClose = new FireWhenEnemyClose(this);
	Gun fireWhenClosestClose = new FireWhenClosestClose(this);
	Targeting guessFactor = new GuessFactorMelee(this);
	Targeting linearTheClosest = new LinearTargetingTheClosest(this);
	Targeting linear = new LinearTargeting(this);
	Targeting quietGun = new QuietGun(this);
	Targeting blindFighterTargetingAndGun = new BlindFighterTargetingAndGun(this);
	Movement quiet = new Quiet(this);
	Movement linearAntiGravityRamming = new LinearAntiGravityRamming(this);
	Movement blindFighterMovement = new BlindFighterMovement(this);

	public boolean infoOfAllTeammates = false;

	protected void selectBehavior() {

		// Radar
		radar = noRadar;

		// Enemies: the leader and all droids
		if (getEnemiesLeader() != null && getCurrentNumberDroidEnemies() == getCurrentNumberOfEnemies() - 1) {
			selectEnemy = atackTheLeader;
		} else {
			selectEnemy = assigned;
		}

		movement = linearAntiGravityRamming;

		if (enemy != null && getCurrentRoundScannedEnemies() > 0) {
			if (enemy.distance < FireWhenEnemyClose.CONSIDER_CLOSE) {
				targeting = linear;
				gun = fireWhenClose;
			} else {
				targeting = linearTheClosest;
				gun = fireWhenClosestClose;
			}
		} else {
			targeting = quietGun;
			gun = ceaseFire;
		}

		if (!infoOfAllTeammates && getCurrentNumberOfTeamMates() == this.getTeammates().length) {
			infoOfAllTeammates = true;
		}
		// We are all droids
		// Credits go to BlindDroid - a robot by (Kwok-Cheung Li)
		if (infoOfAllTeammates && getTeamLeader() == null) {
			targeting = blindFighterTargetingAndGun;
			movement = blindFighterMovement;
			gun = ceaseFire;
		}
	}
}
