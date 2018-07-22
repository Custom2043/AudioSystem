package audio;

import java.io.InputStream;

import org.lwjgl.openal.AL10;

import util.InputStreamSource;
import util.Logger;

abstract class Source
{
	private boolean loading = false, shouldBePlaying = false;
	private final Class<? extends Codec> codecClass;
	private final int bufferSize;
	private final InputStreamSource streamSource;
	private final int sourceId;
	private int loopNumber = 0;
	private Codec currentCodec;
	Source(int sourceID, InputStreamSource streamSource, float volume, boolean loop, int bufferSize, Class<? extends Codec> codecClass)
	{
		this.sourceId = sourceID;
		this.streamSource = streamSource;
		this.setVolume(volume);
		this.setLooping(loop);
		this.bufferSize = bufferSize;
		this.codecClass = codecClass;
		this.setNewCodec();

		Logger.debug("Create source "+this.sourceId);

		CommandThread.getThread().addLoading(this, 0);

		/*AL10.alSourcef(this.sourceId, AL10.AL_PITCH, 1);
		AL10.alSource3f(this.sourceId, AL10.AL_POSITION, 0, 0, 0);
		AL10.alSourcef(this.sourceId, AL10.AL_MAX_DISTANCE, mD);
		AL10.alSourcef(this.sourceId, AL10.AL_REFERENCE_DISTANCE, rD);
		AL10.alSourcef(this.sourceId, AL10.AL_ROLLOFF_FACTOR, roll);*/
	}

	boolean isLoading()
	{
		return this.loading;
	}
	void setLoading(boolean load)
	{
		this.loading = load;
	}
	boolean shouldPlay()
	{
		return this.shouldBePlaying;
	}
	void setShouldPlay(boolean play)
	{
		this.shouldBePlaying = play;
	}

	int getSourceID()
	{
		return this.sourceId;
	}
	int getBufferSize()
	{
		return this.bufferSize;
	}
	Class<? extends Codec> getCodecClass()
	{
		return this.codecClass;
	}
	InputStreamSource getSource()
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

	boolean streaming()
	{
		return this instanceof StreamingSource;
	}
	StreamingSource getStreamingSource()
	{
		return (StreamingSource)this;
	}
	SoundSource getSoundSource()
	{
		return (SoundSource)this;
	}
	abstract void setLooping(boolean loop);
	abstract boolean isLooping();
	abstract boolean canLoop();
	abstract void pushBuffer(AudioBuffer buffer);
	abstract AudioBuffer[] getBufferData();
	int getLoopNumber()
	{
		return this.loopNumber;
	}
	void addLoop()
	{
		this.loopNumber ++;
	}
	void setVolume(float volume)
	{
		AL10.alSourcef(this.sourceId, AL10.AL_GAIN, AudioSystem.getMasterVolume() * volume);
	}
}
