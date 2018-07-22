package audio;

import java.util.LinkedList;

import util.InputStreamSource;

class StreamingSource extends Source
{
	private boolean looping;
	private int bufferNumber;
	private final LinkedList<AudioBuffer> bufferDatas = new LinkedList<>();
	private int bufferProcessed;

	StreamingSource(int sourceID, InputStreamSource streamSource, float volume, boolean loop,
			int bufferSize, int bufferNumber, Class<? extends Codec> codec)
	{
		super(sourceID, streamSource, volume, loop, bufferSize, codec);
		this.bufferNumber = bufferNumber;
	}

	@Override
	void setLooping(boolean l)
	{
		this.looping = l;
	}

	@Override
	boolean isLooping()
	{
		return this.looping;
	}

	void removeBufferData()
	{
		this.bufferDatas.removeFirst();
	}

	int getBufferNumber()
	{
		return this.bufferNumber;
	}

	@Override
	boolean canLoop()
	{
		return this.getSource().canStreamBeRetrieved();
	}

	@Override
	void pushBuffer(AudioBuffer buffer)
	{
		this.bufferDatas.addLast(buffer);
		this.bufferProcessed ++;
	}

	@Override
	AudioBuffer[] getBufferData()
	{
		return this.bufferDatas.toArray(new AudioBuffer[this.bufferDatas.size()]);
	}

	int getProcessedBuffer()
	{
		return this.bufferProcessed;
	}
}
