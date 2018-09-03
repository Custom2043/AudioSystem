package audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.util.vector.Vector3f;

import util.Logger;
import util.UpdateList;

abstract class Source<T extends Source<T>>
{
	private final UpdateList<SourceCallBack> callBacks = new UpdateList<>();
	private final int sourceId;
	private boolean shouldBePlaying = false;
	private float volume = 1.0f;
	Source(int sourceID)
	{
		this.sourceId = sourceID;

		Logger.debug("Create source "+this.sourceId);
	}

	boolean shouldBePlaying()
	{
		return this.shouldBePlaying;
	}
	void setShouldBePlaying(boolean play)
	{
		this.shouldBePlaying = play;
	}
	public int getOpenALSourceID()
	{
		return this.sourceId;
	}

	public boolean isAutomatic()
	{
		return this instanceof AutomaticSource;
	}

	abstract void pushBuffer(AudioBuffer buffer);
	/**
	 * Returns the buffers currently loaded by the source.<br>
	 * If the source is a sound source, the buffer[0] contains all datas<br>
	 * @return The data read by the source
	 */
	public AudioBuffer[] getBufferData()
    {
        return Command.executeInThread(new Command.CommandGetBuffer(this)).buf;
    }
	abstract AudioBuffer[] getSourceBuffers();

	float getSourceVolume()
	{
		return this.volume;
	}
	void setSourceVolume(float sourceVolume)
	{
		this.volume = sourceVolume;
	}

	/**
	 * @return the volume of the source, between 0 and 1
	 * This value is a relative volume, and thus is not affected by masterVolume
	 */
	public float getVolume()
	{
		return Command.executeInThread(new Command.CommandVolume(this)).value;
	}
	/**
	 * Sets the volume of the source
	 * The volume set is relative to the masterVolume
	 * So the actual volume set is masterVolume * volume
	 * Error {@link #ERROR_INVALID_VALUE} if volume isn't between 0 and 1
	 */
	public T setVolume(float volume)
	{
		if (volume < 0 || volume > 1)
		{
			AudioSystem.setError(AudioSystem.ERROR_INVALID_VALUE);
			return (T)this;
		}

		Command.executeInThread(new Command.CommandVolume(this, volume));
		return (T)this;
	}
	/**
	 * @return the pitch of the source, between 0.5 and 2
	 */
	public float getPitch()
	{
		return Command.executeInThread(new Command.CommandFloat(this, AL10.AL_PITCH)).value;
	}
	/**
	 * Sets the pitch of the source
	 * ERROR {@link #ERROR_INVALID_VALUE} if pitch isn't between 0.5 and 2
	 */
	public T setPitch(float pitch)
	{
		if (pitch < 0.5 || pitch > 2)
		{
			AudioSystem.setError(AudioSystem.ERROR_INVALID_VALUE);
			return (T)this;
		}

		Command.executeInThread(new Command.CommandFloat(this, AL10.AL_PITCH, pitch));
		return (T)this;
	}

	public float getRollOffFactor()
	{
		return Command.executeInThread(new Command.CommandFloat(this, AL10.AL_ROLLOFF_FACTOR)).value;
	}
	public T setRollOffFactor(float rollOffFactor)
	{
		Command.executeInThread(new Command.CommandFloat(this, AL10.AL_ROLLOFF_FACTOR, rollOffFactor));
		return (T)this;
	}

	public Vector3f getPosition()
	{
		return Command.executeInThread(new Command.CommandVector(this, AL10.AL_POSITION)).value;
	}
	public T setPosition(Vector3f position)
	{
		Command.executeInThread(new Command.CommandVector(this, AL10.AL_POSITION, position));
		return (T)this;
	}

	public Vector3f getVelocity()
	{
		return Command.executeInThread(new Command.CommandVector(this, AL10.AL_VELOCITY)).value;
	}
	public T setVelocity(Vector3f velocity)
	{
		Command.executeInThread(new Command.CommandVector(this, AL10.AL_VELOCITY, velocity));
		return (T)this;
	}

	public void addCallBack(SourceCallBack callBack)
	{
		this.callBacks.add(callBack);
		callBack.setSource(this);
	}

	public float getSourceReferenceDistance()
	{
		return Command.executeInThread(new Command.CommandFloat(this, AL10.AL_REFERENCE_DISTANCE)).value;
	}
	public T setSourceReferenceDistance(float referenceDistance)
	{
		Command.executeInThread(new Command.CommandFloat(this, AL10.AL_REFERENCE_DISTANCE, referenceDistance));
		return (T)this;
	}

	public float getSourceMaxDistance()
	{
		return Command.executeInThread(new Command.CommandFloat(this, AL10.AL_MAX_DISTANCE)).value;
	}
	public T setSourceMaxDistance(float maxDistance)
	{
		Command.executeInThread(new Command.CommandFloat(this, AL10.AL_MAX_DISTANCE, maxDistance));
		return (T)this;

	}

	public UpdateList<SourceCallBack> getCallBack()
	{
		return this.callBacks;
	}
	
	public boolean isRelativeToListener()
	{
		return Command.executeInThread(new Command.CommandRelative(this)).value;
	}
	public T setRelativeToListener(boolean relative)
	{
		Command.executeInThread(new Command.CommandRelative(this, relative));
		return (T)this;
	}

	/**
	 * Plays the source
	 * If the sound isn't loaded yet, it will be played as soon as possible
	 * No effect if the source is already {@link #playing(int)}
	 */
	public void play()
	{
		Command.executeInThread(new Command.CommandPlay(this));
	}
	/**
	 * Pauses the source
	 * If the source is {@link #playing(int)}, call {@link #stop(int)}
	 * else, call {@link #play(int)}
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public void pause()
	{
		Command.executeInThread(new Command.CommandPause(this));
	}
	/**
	 * Stops the source
	 * No effect if the source isn't {@link #playing(int)}
	 * Loadings are still active when the source is stopped
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public void stop()
	{
		Command.executeInThread(new Command.CommandStop(this));
	}
	/**
	 * @return true if the source is currently playing, false if the source is loading or stopped
	 */
	public boolean playing()
	{
		return Command.executeInThread(new Command.CommandPlaying(this)).playing;
	}

	public void delete()
	{
		Command.executeInThread(new Command.CommandDeleteSource(this));
	}
	
	/**
	 * @return the current offset of the source in samples
	 * The offset is reset if the source loops
	 */
	public int getOffset()
	{
		return Command.executeInThread(new Command.CommandOffset(this)).value;
	}
	
    /**
	 * Sets the offset of the source in samples
	 * This method needs to start a new loading and read all datas
	 * up to the offset set. During this loading, datas to continue
	 * reading from the current point will still be loaded
	 * Once a buffer from the wanted position is loaded,
	 * all other datas are discarded and the source restarts playing if it was
	 * Error : {@link #ERROR_INVALID_VALUE} if value isn't between 0 and the max number of sample available in the stream
	 * Error : {@link #ERROR_CANT_READ_STREAM} can be thrown while loading
	 */
	public T setOffset(int value)
	{
		Command.executeInThread(new Command.CommandOffset(this, value));
		return (T)this;
	}
}

