package util;

public class CustomTimer
{
	public long ActualTime;
	public long AncienTime;
	public long difference;
	public CustomTimer()
	{
		this.set0();
	}
	/**
	 * Obtenir difference depuis set0() en milliseconde ; Ne reset PAS !
	 */
	public long getDifference()
	{
		this.AncienTime = this.ActualTime;
		this.ActualTime = System.currentTimeMillis();
		this.difference += this.ActualTime - this.AncienTime;
		return this.difference;
	}
	/**
	 * Obtenir le nombre de ticks de *compteur* millisecondes (compteur / difference)
	 * Remet diff�rence � difference % *compteur*
	 */
	public long getNumberOfTicks(long duration)
	{
		return this.getDifference() / duration;
	}
	public void resetUnderATick(long duration)
	{
		this.difference = this.getDifference()%duration;
	}
	public long getDifferenceDivideBy(long compteur)
	{
		long dif = this.getDifference();
		dif /=compteur;
		return dif;
	}
	/**
	 * Positionne le 0 chrono
	 */
	public void set0()
	{
		this.ActualTime = System.currentTimeMillis();
		this.difference = 0;
	}
	/**
	 * Obtenir difference depuis set0() en Seconde ; Ne reset PAS !
	 */
	public float getDifInSec()
	{
		return (this.getDifference())/1000F;
	}
	/**
	 * Assigner une difference en milliseconde ; Reset
	 */
	public void setValue (long value)
	{
		this.ActualTime = System.currentTimeMillis();
		this.difference = value;
	}
}

