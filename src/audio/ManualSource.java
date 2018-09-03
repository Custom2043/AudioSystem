package audio;

import java.util.LinkedList;

import org.lwjgl.openal.AL10;

public class ManualSource extends Source<ManualSource>
{
	private final LinkedList<AudioBuffer> bufferDatas = new LinkedList<>();
	private int bufferProcessed;

	ManualSource(int sourceID)
	{
		super(sourceID);
	}

	AudioBuffer removeBufferData()
	{
		this.bufferProcessed ++;
		return this.bufferDatas.removeFirst();
	}

	@Override
	void pushBuffer(AudioBuffer buffer)
	{
		this.bufferDatas.addLast(buffer);
		if (this.shouldBePlaying())
			AL10.alSourcePlay(this.getOpenALSourceID());
	}

	@Override
	AudioBuffer[] getSourceBuffers()
	{
		return this.bufferDatas.toArray(new AudioBuffer[this.bufferDatas.size()]);
	}
	
	int getProcessedBuffer()
	{
		return this.bufferProcessed;
	}
}
