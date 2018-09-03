package audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioBuffer
{
	private final byte[] audioData;
	private final int until;
	private final Codec writtenBy;
	private int openALBufferID = -1;

	public AudioBuffer(byte[] datas, int until, Codec codec)
	{
		this.audioData = datas;
		this.until = until;
		this.writtenBy = codec;
	}

	public ByteBuffer toByteBuffer()
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(this.until);
		buf.order(ByteOrder.nativeOrder());
		buf.put(this.audioData, 0, this.until);
		buf.flip();

		return buf;
	}

	public byte[] getAudioDatas()
	{
		return this.audioData;
	}

	public int getLimit()
	{
		return this.until;
	}

	public Codec getCodec()
	{
		return this.writtenBy;
	}

	public void setOpenALBufferID(int openALBufferID)
	{
		this.openALBufferID = openALBufferID;
	}

	public int getOpenALBufferID()
	{
		return this.openALBufferID;
	}
}
