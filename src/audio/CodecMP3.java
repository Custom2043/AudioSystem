package audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import javazoom.mp3spi.DecodedMpegAudioInputStream;
import util.Logger;

public class CodecMP3 extends Codec
{
	private boolean endOfStream = false;
	private DecodedMpegAudioInputStream myAudioInputStream = null;
	private int channels, sampleSize, samplerate;

	public CodecMP3(InputStream is)
	{
		super(is);
		this.myAudioInputStream = DecodedMpegAudioInputStream.toFileFormat(new BufferedInputStream(is));
		this.channels = this.myAudioInputStream.getHeader().mode() < 3 ? 2 : 1;
		this.sampleSize = 16;
		this.samplerate = this.myAudioInputStream.getHeader().frequency();
	}

	@Override
	public AudioBuffer readChunk(int length)
	{
		int bytesRead = 0, cnt = 0;

		byte[] streamBuffer = new byte[length * this.getSampleSize()];

		try
		{
			while (!this.endOfStream && bytesRead < streamBuffer.length)
			{
				this.myAudioInputStream.execute();
				if ((cnt = this.myAudioInputStream.read(streamBuffer, bytesRead, streamBuffer.length - bytesRead)) <= 0)
				{
					this.endOfStream = true;
					break;
				}
				bytesRead += cnt;
			}
		}
		catch (Exception ioe)
		{
			this.endOfStream = true;
			return null;
		}

		if (bytesRead <= 0)
		{
			this.endOfStream = true;
			return null;
		}
		return new AudioBuffer(streamBuffer, bytesRead, this);
	}

	@Override
	public AudioBuffer readAll()
	{
		LinkedList<AudioBuffer> bufs = new LinkedList<>();

		while (!this.isStreamOver())
			bufs.add(this.readChunk(16384));

		int sum = 0;
		for (AudioBuffer buf : bufs)
			sum += buf.getLimit();

		byte[] b = new byte[sum];

		int offset=0;
		for (AudioBuffer audioBuf : bufs)
		{
			System.arraycopy(audioBuf.getAudioDatas(), 0, b, offset, audioBuf.getLimit());
			offset += audioBuf.getLimit();
		}

		Logger.debug("Read : "+b.length);

		return new AudioBuffer(b, b.length, this);
	}

	@Override
	public boolean isStreamOver()
	{
		return this.endOfStream;
	}

	@Override
	public void quit()
	{
		try
		{
			this.myAudioInputStream.close();
		}
		catch (Exception e){}
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
		return this.sampleSize;
	}

	@Override
	public int getTotalSize()
	{
		return -1;
	}
}