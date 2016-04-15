package jab.module;

public abstract class SelectEnemy extends Part{
	
	public Module bot;
	public SelectEnemy(Module bot){
		this.bot=bot;
	}
	public void select(){}
	
}
