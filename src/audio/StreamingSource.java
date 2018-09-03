package audio;

import java.util.LinkedList;

import util.InputStreamSource;

public class StreamingSource extends AutomaticSource<StreamingSource>
{
	private boolean looping;
	private int bufferNumber;
	private final LinkedList<AudioBuffer> bufferDatas = new LinkedList<>();
	private int bufferProcessed;

	StreamingSource(int sourceID, InputStreamSource streamSource,
			int bufferSize, int bufferNumber, Class<? extends Codec> codec)
	{
		super(sourceID, streamSource, bufferSize, codec);
		this.bufferNumber = bufferNumber;
	}

	@Override
	public void setLooping(boolean l)
	{
		this.looping = l;
	}

	@Override
	public boolean isLooping()
	{
		return this.looping;
	}

	AudioBuffer removeBufferData()
	{
		this.bufferProcessed ++;
		return this.bufferDatas.removeFirst();
	}

	/**
     * @return the number of buffer(s) used by the source.
     */
	public int getBufferNumber()
	{
		return this.bufferNumber;
	}

	@Override
	public boolean canLoop()
	{
		return this.getSource().canStreamBeRetrieved();
	}

	@Override
	void pushBuffer(AudioBuffer buffer)
	{
		this.bufferDatas.addLast(buffer);
	}

	@Override
	public AudioBuffer[] getSourceBuffers()
	{
		return this.bufferDatas.toArray(new AudioBuffer[this.bufferDatas.size()]);
	}

	int getProcessedBuffer()
	{
		return this.bufferProcessed;
	}
}
