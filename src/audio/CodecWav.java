package audio;

import java.io.IOException;
import java.io.InputStream;

import util.Logger;

public class CodecWav extends Codec
{
	private boolean over = false;
	private int bitsPerChannel, channels, fileSize, samplerate;
	public CodecWav(InputStream is)
	{
		super(is);
		try
		{
			this.stream.skip(22);
			this.channels = this.getValue(2);
			this.samplerate = this.getValue(4);
			this.stream.skip(6);
			this.bitsPerChannel = this.getValue(2);
			this.stream.skip(4);
			this.fileSize = this.getValue(4);
		} catch(IOException e){Logger.error(e);}

		Logger.debug("samplerate : " + this.samplerate);
		Logger.debug("channels : " + this.channels);
		Logger.debug("audioFormat : " + this.getALFormat());
	}

	@Override
	public AudioBuffer readChunk(int length) {
		try
		{
			byte[] b = new byte[length * this.getSampleSize()];
			int read = this.stream.read(b);
			if (this.stream.available() <= 0)
				this.over = true;
			return new AudioBuffer(b, read, this);
		}
		catch (IOException e) {Logger.error(e);}
		return null;
	}
	@Override
	public void quit(){}

	int getValue(int numberLength) throws IOException
	{
		byte[] b = new byte[numberLength];
		this.stream.read(b);
		int value = 0;
		for (int i=0;i<b.length;i++)
		{
			int bb = b[i];
			if (bb < 0)
				bb = 256 + bb;
			value += bb << (8*i);
		}
		return value;
	}

	@Override
	public AudioBuffer readAll()
	{
		return this.readChunk(this.fileSize);
	}

	@Override
	public boolean isStreamOver() {
		return this.over;
	}

	@Override
	public int getChannelsNumber()
	{
		return this.channels;
	}

	@Override
	public int getSamplerate()
	{
		return this.samplerate;
	}

	@Override
	public int getBitsPerSample()
	{
		return this.bitsPerChannel;
	}

	@Override
	public int getTotalSize()
	{
		return this.fileSize;
	}
}
