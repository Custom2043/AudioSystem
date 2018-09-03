package audio;

import static org.lwjgl.openal.ALC10.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.openal.*;

import util.InputStreamSource;

public class AudioSystem
{
	/**
	 * Used to describe errors
	 * @see #getError()
	 */
	public static int ERROR_INVALID_VALUE = -1,
					  ERROR_CANT_READ_STREAM = -2,
					  ERROR_SYSTEM_STOPPED = -3;

	public static int MODEL_NONE = AL10.AL_NONE,
					  MODEL_INVERSE_DISTANCE = AL10.AL_INVERSE_DISTANCE,
					  MODEL_INVERSE_DISTANCE_CLAMPED = AL10.AL_INVERSE_DISTANCE_CLAMPED,
					  MODEL_LINEAR_DISTANCE = AL11.AL_LINEAR_DISTANCE,
					  MODEL_LINEAR_DISTANCE_CLAMPED = AL11.AL_LINEAR_DISTANCE_CLAMPED,
					  MODEL_EXPONENT_DISTANCE = AL11.AL_EXPONENT_DISTANCE,
					  MODEL_EXPONENT_DISTANCE_CLAMPED = AL11.AL_EXPONENT_DISTANCE_CLAMPED;

	static float masterVolume = 1;

	private static LinkedList<Source> sources = new LinkedList<>();

	private static Class<? extends Codec> defaultCodec = CodecWav.class;

	private static int defaultStreamingBufferSize = 262144,
					   defaultSoundBufferSize = 2097152,
					   defaultNumberOfStreamingBuffers = 3;

	private static int lastError;

	static int distanceModel = MODEL_NONE;

	private static boolean isInitialized;

	static void addSource(Source s)
    {
        sources.add(s);
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
	 * Creates a new streaming source that has to be feed manually
	 * Error {@link #ERROR_INVALID_VALUE} if volume isn't between 0 and 1
	 * @return the source created
	 */
	public static ManualSource newManualSource()
	{
		return (ManualSource) Command.executeInThread(new Command.CommandNewSource()).source;
	}

	/**
	 * @return newStreamingSource(streamSource, volume, looping, 0, 0, null)
	 */
	public static StreamingSource newStreamingSource(InputStreamSource streamSource)
	{
		return newStreamingSource(streamSource, 0, 0, null);
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
	public static StreamingSource newStreamingSource(InputStreamSource streamSource, int bufferSize, int bufferNumber, Class<? extends Codec> codec)
	{
		if (bufferSize < 0 || bufferNumber < 0)
		{
			setError(ERROR_INVALID_VALUE);
			return null;
		}

		return (StreamingSource) Command.executeInThread(new Command.CommandNewSource(streamSource,
				bufferSize == 0 ? defaultStreamingBufferSize : bufferSize,
				bufferNumber == 0 ? defaultNumberOfStreamingBuffers : bufferNumber,
				codec == null ? defaultCodec : codec)).source;
	}

	/**
	 * @return newStreamingSource(streamSource, volume, looping, 0, 0, null)
	 */
	public static SoundSource newSoundSource(InputStreamSource streamSource)
	{
		return newSoundSource(streamSource, 0, null);
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
	public static SoundSource newSoundSource(InputStreamSource streamSource, int bufferSize, Class<? extends Codec> codec)
	{
		return (SoundSource) Command.executeInThread(new Command.CommandNewSource(streamSource,
				bufferSize == 0 ? defaultSoundBufferSize : bufferSize, 0,
				codec == null ? defaultCodec : codec)).source;
	}

	/**
	 * @return an array containing the ids of all sources declared and not deleted
	 */
	public static List<Source> getSources()
	{
		return Collections.unmodifiableList(sources);
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

		Command.executeInThread(new Command.CommandMasterVolume(volume));
	}
	/**
	 * @return the master volume, between 0 and 1
	 */
	public static float getMasterVolume()
	{
		return Command.executeInThread(new Command.CommandMasterVolume()).value;
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

	public static void setAttenationMod(int atteMod)
	{
		Command.executeInThread(new Command.CommandAttenuationMod(atteMod));
	}
	public static int getAttenationMod()
	{
		return Command.executeInThread(new Command.CommandAttenuationMod()).value;
	}
}
