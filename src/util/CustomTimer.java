package util;

public class CustomTimer
{
	private long oldTime, difference;
	private boolean paused;
	private int factor = 1;
	
	/**
	 * Creates a Timer using {@link java.lang.System#nanoTime()} and starts it
	 */
	public CustomTimer()
	{
		this.start();
	}
	
	public CustomTimer useNanoSecondsValue()
	{
		this.factor = 1;
		return this;
	}
	
	public CustomTimer useMicroSecondsValue()
	{
		this.factor = 1000;
		return this;
	}
	
	public CustomTimer useMilliSecondsValue()
	{
		this.factor = 1000000;
		return this;
	}
	
	public CustomTimer useSecondsValue()
	{
		this.factor = 1000000000;
		return this;
	}
	
	/**
	 * @return the current difference in nanoseconds
	 */
	public long getDifference()
	{
		if (this.paused)
			return this.difference;
		long currentTime = System.nanoTime();
		this.difference += currentTime - this.oldTime;
		this.oldTime = currentTime;
		return this.difference / this.factor;
	}
	
	/**
	 * Starts the timer
	 */
	public void start()
	{
		this.oldTime = System.nanoTime();
		this.difference = 0;
		this.paused = false;
	}

	/**
	 * Starts the timer with a specific value
	 */
	public void setValue (long value)
	{
		this.oldTime = System.nanoTime();
		this.difference = value * this.factor;
		this.paused = false;
	}
	
	/**
	 * Pauses or resumes the timer
	 */
	public void pause()
	{
		this.difference = this.getDifference();
		this.paused = !this.paused;
	}
	
	public boolean isPaused()
	{
		return this.paused;
	}
}

