package audio;

import java.io.IOException;
import java.io.InputStream;

import org.newdawn.slick.openal.OggInputStream;

import util.Logger;

public class CodecOgg extends Codec
{
	protected OggInputStream oggStream;
	public int channels, samplerate;
	public CodecOgg(InputStream is) throws IOException
	{
		super(is);
		this.oggStream = new OggInputStream(is);
		this.samplerate = this.oggStream.getRate();
		this.channels = this.oggStream.getChannels();
	}
	@Override
	public AudioBuffer readChunk(int length) {
		try
		{
			byte[] by = new byte[length * this.getSampleSize()];
			int red = this.oggStream.read(by);
			return new AudioBuffer(by, red, this);
		}
		catch (IOException e) {Logger.error(e);}
		return null;
	}

	@Override
	public void quit()
	{
		try
		{
			this.oggStream.close();
		} catch (IOException e) {Logger.error(e);}
	}
	@Override
	public AudioBuffer readAll() {
		return this.readChunk(this.oggStream.getLength());
	}
	@Override
	public boolean isStreamOver() {
		return this.oggStream.atEnd();
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
		return 16;
	}
	@Override
	public int getTotalSize()
	{
		return this.oggStream.getLength();
	}
}