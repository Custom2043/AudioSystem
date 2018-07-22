package audio;

import java.io.InputStream;

import org.lwjgl.openal.AL10;

/**
 * Codecs must implements an constructor that takes an input stream
 */
public abstract class Codec
{
	public InputStream stream;
	public Codec(InputStream is)
	{
		this.stream = is;
	}
	/**
	 * Reads datas
	 * @param chunkSize the maximum samples read
	 * The number of byte read is chunkSize * {@link #getSampleSize()}
	 * Less datas can be read if the stream ends.
	 * The number of datas read is always a multiple of {@link #getSampleSize()}
	 * @return the datas
	 */
	abstract AudioBuffer readChunk(int chunkSize);
	/**
	 * Reads datas
	 * Datas are read until the stream is over
	 * The number of datas read is always a multiple of {@link #getSampleSize()}
	 * @return the datas
	 */
	abstract AudioBuffer readAll();
	/**
	 * @return true if the stream is out of datas
	 */
	abstract boolean isStreamOver();
	/**
	 * This method must close the stream
	 */
	abstract void quit();

	public abstract int getChannelsNumber();
	public abstract int getSamplerate();
	public abstract int getBitsPerSample();
    /**
     * @return the total size of all datas or -1 if we don't know
     */
	public abstract int getTotalSize();

	int getALFormat()
	{
		if (this.getChannelsNumber() == 1)
			return this.getBitsPerSample() == 8 ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16;

		return this.getBitsPerSample() == 8 ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16;
	}

	/**
	 * @return the size of a sample (in bytes)
	 */
	int getSampleSize()
	{
		return this.getChannelsNumber() * this.getBitsPerSample() / 8;
	}

	int byteSizeToSampleNumber(int byteSize)
	{
		return byteSize / this.getSampleSize();
	}

	int sampleNumberToByteSize(int sampleNumber)
	{
		return sampleNumber * this.getSampleSize();
	}
}
