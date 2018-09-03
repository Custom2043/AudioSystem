package audio;

import java.io.InputStream;

import util.InputStreamSource;
import util.Logger;

abstract class AutomaticSource<T extends AutomaticSource<T>> extends Source<T>
{
	private boolean loading = false;
	private final Class<? extends Codec> codecClass;
	private final int bufferSize;
	private final InputStreamSource streamSource;
	private int loopNumber = 0;
	private Codec currentCodec;

	AutomaticSource(int sourceID, InputStreamSource streamSource, int bufferSize, Class<? extends Codec> codecClass)
	{
		super(sourceID);
		this.streamSource = streamSource;
		this.bufferSize = bufferSize;
		this.codecClass = codecClass;
		this.setNewCodec();

		CommandThread.getThread().addLoading(this, 0);
	}

	boolean isSourceLoading()
	{
		return this.loading;
	}
	void setLoading(boolean load)
	{
		this.loading = load;
	}
	/**
     * @return The size of the buffer(s) used by the source
     */
	public int getBufferSize()
	{
		return this.bufferSize;
	}
	public Class<? extends Codec> getCodecClass()
	{
		return this.codecClass;
	}
	public InputStreamSource getSource()
	{
		return this.streamSource;
	}
	/**
	 * Should only be called if source.canStreamBeRetrieved() has been checked
	 */
	void setNewCodec()
	{
		if (this.currentCodec != null)
			this.currentCodec.quit();
		try
		{
			this.currentCodec = this.codecClass.getConstructor(InputStream.class).newInstance(this.streamSource.getStreamBack());
		}
		catch(Exception e)
		{
			Logger.warning(e);
			AudioSystem.setError(AudioSystem.ERROR_CANT_READ_STREAM);
			this.currentCodec = null;
		}
	}
	Codec getCurrentCodec()
	{
		return this.currentCodec;
	}

	/**
	 * Sets the source looping
	 * Everytime the source needs to loop, it will reload the datas if needed
	 * If the source is a sound or if the datas are short enough, it will just reuse the loaded datas
	 * Error : {@link #ERROR_CANT_READ_STREAM} can be thrown while loading
	 */
	public abstract void setLooping(boolean loop);
	/**
	 * @return true if the source is looping
	 * Error : {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public abstract boolean isLooping();
	 /**
     * Returns true if the source can loop
     * @return True if source is a sound source, or if the stream can still be retrieved
     */
	public abstract boolean canLoop();

	int getSourceLoopNumber()
	{
		return this.loopNumber;
	}
	void addLoop()
	{
		this.loopNumber ++;
	}

	/**
	 * @return true if the source is currently loading, false if the source is loaded
	 */
	public boolean loading()
	{
		return Command.executeInThread(new Command.CommandLoading(this)).loading;
	}

	 /**
     * Converts a number of samples into a duration
     */
    public long samplesToNanoSeconds(long samples)
	{
		return samples * 1000000 / this.getCurrentCodec().getSamplerate();
	}
    /**
     * Converts a duration into a number of samples
     */
	public long nanoSecondsToSamples(long nanoseconds)
	{
		return nanoseconds * this.getCurrentCodec().getSamplerate() / 1000000;
	}

    /**
     * Returns the samplerate of the source.<br>
	 * @return The samplerate of the input datas of the sound source
	 */
    public int getSampleRate()
    {
    	return Command.executeInThread(new Command.CommandGetDatas(this)).samplerate;
    }

    /**
     * Returns the number of channel of the source.<br>
	 * @return The number of channel of the input datas of the sound source
	 */
    public int getNumberOfChannel()
    {
    	return Command.executeInThread(new Command.CommandGetDatas(this)).channels;
    }

    /**
     * Returns the size of a sample of the source.<br>
	 * @return The size of a sample of the input datas (8 or 16)
	 */
    public int getSampleSize()
    {
    	return Command.executeInThread(new Command.CommandGetDatas(this)).sampleSize;
    }

    /**
     * Returns the total size (in bytes) of this source's data
     * For sound source, this is equivalent to {@link #getBufferSize(int)}
     * @return The the total size (in bytes) of the source's data
     */
    public int getTotalSize()
    {
        return Command.executeInThread(new Command.CommandGetDatas(this)).totalSize;
    }

    /**
     * @return how many time the source has looped
     * Resets each time {@link #setLooping(boolean)}(false) is called
     */
    public int getLoopNumber()
    {
    	return Command.executeInThread(new Command.CommandLoop(this)).number;
    }
}
