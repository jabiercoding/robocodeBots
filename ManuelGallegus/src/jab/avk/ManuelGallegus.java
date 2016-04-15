package jab.avk;

import jab.avk.gun.*;
import jab.avk.movement.*;
import jab.avk.radar.*;
import jab.avk.selectEnemy.*;
import jab.avk.special.*;
import jab.avk.targeting.*;

import java.awt.Color;



public class ManuelGallegus extends Module {

	protected void initialize() {
		this.setColors(Color.white, Color.blue.brighter(), Color.white);
	}

	// All the used parts
	SelectEnemy lastScanned = new LastScanned(this);
	Radar spinningRadar = new SpinningRadar(this);
	Radar wideLock = new WideLock(this);
	Gun ceaseFire = new CeaseFire(this);
	Gun maximum = new Maximum(this);
	Targeting guessFactor = new GuessFactor(this);
	Targeting quietGun = new QuietGun(this);
	Movement quiet = new Quiet(this);
	Movement basicSurfer = new BasicSurfer(this);
	Movement antiRamming = new AntiRamming(this);

	RamBotFlag ramBotFlag = new RamBotFlag(this);
	
	boolean battleStart = false;
	
	static boolean dataRestored = false;
	
	@SuppressWarnings("static-access")
	protected void selectBehavior() {

		if (!battleStart && getGunHeat()<=getGunCoolingRate()*5){
			battleStart = true;
		}
		
		activate(ramBotFlag);
		
		radar = wideLock;
		
		if (enemies.size() > 0 && enemy != null) {
			if (!dataRestored){
				((BasicSurfer)basicSurfer).restoreBrain();
				dataRestored=true;
			}
			selectEnemy = lastScanned;
			targeting = guessFactor;
			if (!battleStart || ramBotFlag.isRamBot) {
				movement = antiRamming;				
			} else {
				movement = basicSurfer;
			}

			gun = maximum;
		} else {
			// No Enemy
			selectEnemy = lastScanned;
			//radar = spinningRadar;
			gun = ceaseFire;
			targeting = quietGun;
			movement = quiet;
		}
	}
}
