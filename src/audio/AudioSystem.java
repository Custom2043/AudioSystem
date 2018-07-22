package audio;

import static org.lwjgl.openal.ALC10.*;

import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import util.InputStreamSource;
import util.Logger;

public class AudioSystem
{
	/**
	 * Used to describe errors
	 * @see #getError()
	 */
	public static int ERROR_UNEXISTENT_SOURCE = -1,
					  ERROR_INVALID_VALUE = -2,
					  ERROR_CANT_READ_STREAM = -3,
					  ERROR_SYSTEM_STOPPED = -4;

	private static float masterVolume = 1;

	private static ArrayList<Source> sources= new ArrayList<>();

	private static Class<? extends Codec> defaultCodec = CodecWav.class;

	private static int defaultStreamingBufferSize = 262144,
					   defaultSoundBufferSize = 2097152,
					   defaultNumberOfStreamingBuffers = 3;

	private static int lastError;

	private static boolean isInitialized;

	static void addSource(Source s)
    {
        sources.add(s);
    }
	static Source getSource(int sourceId)
	{
		for (Source s : sources)
			if (s.getSourceID() == sourceId)
				return s;
		Logger.warning("Unexistent source "+sourceId);
		setError(ERROR_SYSTEM_STOPPED);
		return null;
	}

	static Iterator<Source> getSourcesIterator()
	{
		return sources.iterator();
	}

	private static long context, device;
	/**
	 * Initialize the system
	 * Called if necessary by any other function that a command
	 * Error : {@link #ERROR_SYSTEM_STOPPED} if the system couldn't start
	 * @return true if the systam has been initialized or was initialized
	 */
	public static boolean init()
	{
		if (isInitialized)
			return true;

		String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
		  				  device = alcOpenDevice(defaultDeviceName);

		int[] attributes = {0};
		context = alcCreateContext(device, attributes);
		alcMakeContextCurrent(context);

		ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
		AL.createCapabilities(alcCapabilities);

		AL10.alListener3f(AL10.AL_POSITION, 0,0,0);
		AL10.alListener3f(AL10.AL_VELOCITY, 0,0,0);

		if (AL10.alGetError() == AL10.AL_NO_ERROR)
		{
			// Successfully started
			CommandThread.getThread().start();
			isInitialized = true;
			return true;
		}
		else
		{
			// Error : system couldn't start
			AudioSystem.setError(AudioSystem.ERROR_SYSTEM_STOPPED);
			alcDestroyContext(context);
			alcCloseDevice(device);
			return false;
		}
	}
	/**
	 * Clean up the system and release resources
	 * No other function should be called after this one
	 */
	public static void quit()
	{
		Command.executeInThread(new Command.CommandCleanUp());
		CommandThread.getThread().quit();
		alcDestroyContext(context);
		alcCloseDevice(device);
	}
	/**
	 * @return true if the system is initialized
	 */
	public static boolean isInitialized()
	{
		return isInitialized;
	}

	/**
	 * @return newStreamingSource(streamSource, volume, looping, 0, 0, null)
	 */
	public static int newStreamingSource(InputStreamSource streamSource, float volume, boolean looping)
	{
		return newStreamingSource(streamSource, volume, looping, 0, 0, null);
	}

	/**
	 * Creates a new source and starts loading the stream
	 * If bufferSize is 0, uses {@link #defaultStreamingBufferSize}
	 * If bufferNumber is 0, uses {@link #defaultNumberOfStreamingBuffers}
	 * If codec is null, uses {@link #defaultCodec}
	 * Error {@link #ERROR_INVALID_VALUE} if bufferSize or bufferNumber are negative or volume isn't between 0 and 1
	 * Error {@link #ERROR_CANT_READ_STREAM} can be thrown while loading, but the source will be created
	 * @return the ID of the source created or -1 if an error occured
	 */
	public static int newStreamingSource(InputStreamSource streamSource, float volume, boolean looping, int bufferSize, int bufferNumber, Class<? extends Codec> codec)
	{
		if (volume < 0 || volume > 1 || bufferSize < 0 || bufferNumber < 0)
		{
			setError(ERROR_INVALID_VALUE);
			return -1;
		}

		return Command.executeInThread(new Command.CommandNewSource(streamSource, volume, looping,
				bufferSize == 0 ? defaultStreamingBufferSize : bufferSize,
				bufferNumber == 0 ? defaultNumberOfStreamingBuffers : bufferNumber,
				codec == null ? defaultCodec : codec)).sourceId;
	}

	/**
	 * @return newStreamingSource(streamSource, volume, looping, 0, 0, null)
	 */
	public static int newSoundSource(InputStreamSource streamSource, float volume, boolean looping)
	{
		return newSoundSource(streamSource, volume, looping, 0, null);
	}

	/**
	 * Creates a new sound
	 * If bufferSize is 0, uses {@link #defaultStreamingBufferSize}
	 * If bufferSize is negative, reads the sound infinitely
	 * If codec is null, uses {@link #defaultCodec}
	 * Error {@link #ERROR_INVALID_VALUE} if volume isn't between 0 and 1
	 * Error {@link #ERROR_CANT_READ_STREAM} can be thrown while loading, but the source will be created
	 * @return the ID of the source created or -1 if an error occured
	 */
	public static int newSoundSource(InputStreamSource streamSource, float volume, boolean looping, int bufferSize, Class<? extends Codec> codec)
	{
		if (volume < 0 || volume > 1)
		{
			setError(ERROR_INVALID_VALUE);
			return -1;
		}

		return Command.executeInThread(new Command.CommandNewSource(streamSource, volume, looping,
				bufferSize == 0 ? defaultSoundBufferSize : bufferSize, 0,
				codec == null ? defaultCodec : codec)).sourceId;
	}

	/**
	 * @return an array containing the ids of all sources declared and not deleted
	 */
	public static int[] getSources()
	{
		int[] sourcesId = new int[sources.size()];
		int i = 0;
		for (Source s : sources)
			sourcesId[i++] = s.getSourceID();
		return sourcesId;
	}
	/**
	 * Deletes the source
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static void deleteSource(int sourceId)
	{
		Command.executeInThread(new Command.CommandDeleteSource(sourceId));
	}

	/**
	 * Plays the source
	 * If the sound isn't loaded yet, it will be played as soon as possible
	 * No effect if the source is already {@link #playing(int)}
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static void play(int sourceId)
	{
		Command.executeInThread(new Command.CommandPlay(sourceId));
	}
	/**
	 * Pauses the source
	 * If the source is {@link #playing(int)}, call {@link #stop(int)}
	 * If the source isn't {@link #playing(int)}, call {@link #play(int)}
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static void pause(int sourceId)
	{
		Command.executeInThread(new Command.CommandPause(sourceId));
	}
	/**
	 * Stops the source
	 * No effect if the source isn't {@link #playing(int)}
	 * Loadings are still active when the source is stopped
	 * Error {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static void stop(int sourceId)
	{
		Command.executeInThread(new Command.CommandStop(sourceId));
	}
	/**
	 * @return true if the source is currently playing, false if the source is loading or stopped
	 */
	public static boolean playing(int sourceId)
	{
		return Command.executeInThread(new Command.CommandPlaying(sourceId)).playing;
	}

	/**
	 * @return true if the source is currently loading, false if the source is loaded
	 */
	public static boolean loading(int sourceId)
	{
		return Command.executeInThread(new Command.CommandLoading(sourceId)).loading;
	}

	/**
	 * Sets the volume of the source
	 * The volume set is relative to the masterVolume
	 * So the actual volume set is masterVolume * volume
	 * Error {@link #ERROR_INVALID_VALUE} if volume isn't between 0 and 1
	 */
	public static void setVolume(int sourceId, float volume)
	{
		if (volume < 0 || volume > 1)
		{
			setError(ERROR_INVALID_VALUE);
			return;
		}

		Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_GAIN, volume));
	}
	/**
	 * @return the volume of the source, between 0 and 1
	 * This value is a relative volume, and thus is not affected by masterVolume
	 */
	public static float getVolume(int sourceId)
	{
		return Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_GAIN)).value;
	}
	/**
	 * Sets the master volume
	 * All sources have their volume updated
	 * Error {@link #ERROR_INVALID_VALUE} if volume isn't between 0 and 1
	 */
	public static void setMasterVolume(float volume)
	{
		if (volume < 0 || volume > 1)
		{
			setError(ERROR_INVALID_VALUE);
			return;
		}

		AudioSystem.masterVolume = volume;
		for (Source s : AudioSystem.sources)
			s.setVolume(AL10.alGetSourcei(s.getSourceID(), AL10.AL_GAIN));
	}
	/**
	 * @return the master volume, between 0 and 1
	 */
	public static float getMasterVolume()
	{
		return AudioSystem.masterVolume;
	}
	/**
	 * Sets the pitch of the source
	 * ERROR {@link #ERROR_INVALID_VALUE} if pitch isn't between 0.5 and 2
	 * ERROR {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static void setPitch(int sourceId, float pitch)
	{
		if (pitch < 0.5 || pitch > 2)
		{
			setError(ERROR_INVALID_VALUE);
			return;
		}

		Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_PITCH, pitch));
	}
	/**
	 * @return the pitch of the source, between 0.5 and 2
	 */
	public static float getPitch(int sourceId)
	{
		return Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_PITCH)).value;
	}
	/**
	 * Sets the position of the source in samples
	 * This method needs to start a new loading and read all datas
	 * up to the position set. During this loading, datas to continue
	 * reading from the current point will still be loaded
	 * Once a buffer from the wanted position is loaded,
	 * all other datas are discarded and the source restarts playing if it was
	 * Error : {@link #ERROR_UNEXISTENT_SOURCE}
	 * Error : {@link #ERROR_INVALID_VALUE} if value isn't between 0 and the max number of sample available in the stream
	 * Error : {@link #ERROR_CANT_READ_STREAM} can be thrown while loading
	 */
	public static void setPosition(int sourceId, int value)
	{
		Command.executeInThread(new Command.CommandPosition(sourceId, value));
	}
	/**
	 * @return the current position of the source in samples
	 * The position is reset if the source is looped
	 * Error : {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static int getPosition(int sourceId)
	{
		return Command.executeInThread(new Command.CommandPosition(sourceId)).value;
	}

	public static void fade(int source, int time, float baseVolume, float endVolume)
	{
		CommandThread.getThread().fadings.add(new Command.CommandVolumeFading(source, baseVolume, endVolume, time));
	}
	/**
	 * Sets the source looping
	 * Everytime the source needs to loop, it will reload the datas if needed
	 * If the source is a sound or if the datas are short enough, it will just reuse the loaded datas
	 * Error : {@link #ERROR_UNEXISTENT_SOURCE}
	 * Error : {@link #ERROR_CANT_READ_STREAM} can be thrown while loading
	 */
	public static void setLooping(int sourceId, boolean looping)
	{
		Command.executeInThread(new Command.CommandLoop(sourceId, looping));
	}
	/**
	 * @return true if the source is looping
	 * Error : {@link #ERROR_UNEXISTENT_SOURCE}
	 */
	public static boolean isLooping(int sourceId)
	{
		return Command.executeInThread(new Command.CommandLoop(sourceId)).value;
	}
	 /**
     * Returns true if the source can loop
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
     * @param sourceID The id of the source
     * @return True if source is a sound source, or if it has been initialized with {@link #newStreamingSource(InputStreamSource, boolean, float, boolean, int, int, Class)}
     */
    public static boolean canLoop(int sourceID)
    {
    	Source s = getSource(sourceID);
    	if (s != null)
    		return s.canLoop();
    	return false;
    }

    /**
     * Sets default values of the buffers used when creating a source
     * If an argument is 0, the current value is kept
     * If soundBufferSize is negative, sounds will be read infinitely by default
     * The size of the buffers are in samples, not bytes
     * Error {@link #ERROR_INVALID_VALUE} if streamingBufferSize or streamingBufferNumber is negative
     * Other values are not affected if one triggers an error
     */
	public static void setDefaultBuffers(int streamingBufferNumber, int streamingBufferSize, int soundBufferSize)
	{
		if (streamingBufferNumber < 0 || streamingBufferSize < 0)
		{
			setError(ERROR_INVALID_VALUE);
			return;
		}

		defaultStreamingBufferSize = streamingBufferSize;
		defaultSoundBufferSize = soundBufferSize;
		defaultNumberOfStreamingBuffers = streamingBufferNumber;
	}
	/**
	 * @return the default size of buffers in samples for streaming sources
	 */
	public static int getDefaultStreamingBufferSize()
	{
		return defaultStreamingBufferSize;
	}
	/**
	 * @return the default size of buffers in samples for sound sources
	 */
	public static int getDefaultSoundBufferSize()
	{
		return defaultSoundBufferSize;
	}
	/**
	 * @return the default number of buffers for streaming sources
	 */
	public static int getDefaultNumberOfBuffers()
	{
		return defaultNumberOfStreamingBuffers;
	}
	/**
	 * Sets the default codec class used to write datas
	 * If c is null, the ancien value is kept
	 */
	public static void setDefaultCodec(Class<? extends Codec> c)
	{
		if (c != null)
			defaultCodec = c;
	}
	/**
	 * @return the default codec class used when a source is created
	 */
	public static Class<? extends Codec> getDefaultCodec()
	{
		return defaultCodec;
	}

	/**
	 * Returns the buffers currently loaded by the source.<br>
	 * If the source is a sound source, the buffer[0] contains all datas<br>
	 * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
	 * @param sourceID The id of the source
	 * @return The data read by the source
	 */
    public static AudioBuffer[] getSourceBuffers(int sourceID)
    {
        return Command.executeInThread(new Command.CommandGetBuffer(sourceID)).buf;
    }

    /**
     * Returns the samplerate of the source.<br>
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
	 * @param sourceID The id of the sound source
	 * @return The samplerate of the input datas of the sound source
	 */
    public static int getSampleRate(int sourceID)
    {
    	return Command.executeInThread(new Command.CommandGetDatas(sourceID)).samplerate;
    }

    /**
     * Returns the number of channel of the source.<br>
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
	 * @param sourceID The id of the sound source
	 * @return The number of channel of the input datas of the sound source
	 */
    public static int getNumberOfChannel(int sourceID)
    {
    	return Command.executeInThread(new Command.CommandGetDatas(sourceID)).channels;
    }

    /**
     * Returns the size of a sample of the source.<br>
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
	 * @param sourceID The id of the sound source
	 * @return The size of a sample of the input datas (8 or 16)
	 */
    public static int getSampleSize(int sourceID)
    {
    	return Command.executeInThread(new Command.CommandGetDatas(sourceID)).sampleSize;
    }

    /**
     * Returns the total size (in bytes) of this source's data
     * For sound source, this is equivalent to {@link #getBufferSize(int)}
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
     * @param sourceID The id of source
     * @return The the total size (in bytes) of the source's data
     */
    public static int getTotalSize(int sourceID)
    {
        return Command.executeInThread(new Command.CommandGetDatas(sourceID)).totalSize;
    }

    /**
     * Returns the size of the buffer(s) used by the source.
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
     * @param sourceID The id of source
     * @return The size of the buffer(s) used by the source
     */
    public static int getBufferSize(int sourceID)
    {
        Source s = getSource(sourceID);
        if (s != null)
        	return s.getBufferSize();
        return -1;
    }

    /**
     * Returns the number of buffer(s) used by the source.
     * Error ERROR_UNEXISTENT_SOURCE if the source doesn't exist<br>
     * @param sourceID The id of source
     * @return The number of the buffer(s) used by the source (1 for sound source)
     */
    public static int getBufferNumber(int sourceID)
    {
        Source s = getSource(sourceID);
        if (s == null)
        	return -1;
        if (s instanceof SoundSource)
            return 1;
        else
            return ((StreamingSource)s).getBufferNumber();
    }

    /**
     * @return how many time the source has looped
     * Resets each time {@link #setLooping(int, boolean)}(sourceId, false) is called
     * Error : {@link #ERROR_UNEXISTENT_SOURCE}
     */
    public static int getLoopNumber(int sourceId)
    {
    	return Command.executeInThread(new Command.CommandLoop(sourceId)).number;
    }

    /**
     * Converts a number of samples into a duration
     */
    public static long samplesToNanoSeconds(int sourceID, long samples)
	{
		Source s = getSource(sourceID);
		if (s == null)
			return -1;

		return samples * 1000000 / s.getCurrentCodec().getSamplerate();
	}
    /**
     * Converts a duration into a number of samples
     */
	public static long nanoSecondsToSamples(int sourceID, long nanoseconds)
	{
		Source s = getSource(sourceID);
		if (s == null)
			return -1;

		return nanoseconds * s.getCurrentCodec().getSamplerate() / 1000000;
	}

	static void setError(int errorID)
	{
		lastError = errorID;
	}
	/**
     * Returns the last error that occured.
     * Resets the error when used
     * @return The last error or 0 if no error occured since the last call of getError()
     */
    public static int getError()
    {
    	int i = lastError;
    	lastError = 0;
    	return i;
    }

    /*public static int getMaxSourcesAllowed()
    {

    }

    public static int getMaxSourcesPlayingAllowed()
    {

    }*/

	/* Unused : 3D Audio
	public static int getDistanceModel()
	{
		return Command.executeInThread(new Command.CommandAttenuationMod()).atte;
	}
	public static void setSourcePosition(int sourceId, Vector3f vec)
	{
		Command.executeInThread(new Command.CommandVector(sourceId, AL10.AL_POSITION, vec));
	}
	public static Vector3f getSourcePosition(int sourceId)
	{
		return Command.executeInThread(new Command.CommandVector(sourceId, AL10.AL_POSITION)).vec;
	}
	public static void setSourceVelocity(int sourceId, Vector3f vec)
	{
		Command.executeInThread(new Command.CommandVector(sourceId, AL10.AL_VELOCITY, vec));
	}
	public static void setAttenationMod(int atteMod)
	{
		Command.executeInThread(new Command.CommandAttenuationMod(atteMod));
	}
	public static int getAttenationMod()
	{
		return Command.executeInThread(new Command.CommandAttenuationMod()).atte;
	}
	public static Vector3f getSourceVelocity(int sourceId)
	{
		return Command.executeInThread(new Command.CommandVector(sourceId, AL10.AL_VELOCITY)).vec;
	}
	public static void setSourceRoll(int sourceId, float roll)
	{
		Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_ROLLOFF_FACTOR, roll));
	}
	public static float getSourceRoll(int sourceId)
	{
		return Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_ROLLOFF_FACTOR)).value;
	}
	public static void setSourceReferenceDistance(int sourceId, float referenceDistance)
	{
		Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_REFERENCE_DISTANCE, referenceDistance));
	}
	public static float getSourceReferenceDistance(int sourceId)
	{
		return Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_REFERENCE_DISTANCE)).value;
	}
	public static void setSourceMaxDistance(int sourceId, float maxDistance)
	{
		Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_MAX_DISTANCE, maxDistance));
	}
	public static float getSourceMaxDistance(int sourceId)
	{
		return Command.executeInThread(new Command.CommandFloat(sourceId, AL10.AL_MAX_DISTANCE)).value;
	}*/
}
