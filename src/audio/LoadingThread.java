package audio;

/**
 * A thread that loads a buffer from a codec Instanciated each time a source
 * needs to prepare a buffer Used to allow CommandThread to react fast at short
 * commands Make sure to use only one LoadingThread by source at the same time
 */
class LoadingThread extends Thread
{
	AutomaticSource source;
	private AudioBuffer out = null;
	private int toSkip;

	/**
	 * Create a new LoadingThread and starts it
	 *
	 * @param codec
	 *            The codec used to read datas : It must not be used by another
	 *            LoadingThread
	 * @param bufferSize
	 *            The size of the buffer to read : 0 to all available
	 * @param pre
	 *            Datas already loaded to add to the buffer Only used by streaming
	 *            sounds
	 */
	LoadingThread(AutomaticSource source, int tS)
	{
		this.source = source;
		this.toSkip = tS;
		this.start();
	}

	/**
	 * Starts reading datas and filling the buffer
	 */
	@Override
	public void run()
	{
		try
		{
			AudioBuffer sb;
			if (this.source instanceof SoundSource)
			{
				if (this.source.getBufferSize() < 0)
					sb = this.source.getCurrentCodec().readAll();
				else
					sb = this.source.getCurrentCodec().readChunk(this.source.getBufferSize());
			}
			else if (this.toSkip != 0)
			{
				StreamingSource source = (StreamingSource)this.source;
				source.setNewCodec();
				source.getCurrentCodec().readChunk(this.toSkip);
				sb = source.getCurrentCodec().readChunk(source.getBufferSize());
			}
			else
			{
				StreamingSource source = (StreamingSource)this.source;
				if (source.getCurrentCodec().isStreamOver())
				{
					if (!source.isLooping())
					{
						this.setBuffer(new AudioBuffer(new byte[0], 0, source.getCurrentCodec()));
						return;
					}
					source.setNewCodec();
				}
				sb = source.getCurrentCodec().readChunk(source.getBufferSize());
			}
			this.setBuffer(sb);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Synchronized to be sure this doesn't interfere with {@link #getOutBuffer()}
	 * or {@link #isLoadingOver()}
	 *
	 * @param buffer
	 *            The loaded buffer
	 */
	private synchronized void setBuffer(AudioBuffer buffer)
	{
		this.out = buffer;
	}

	/**
	 * Synchronized to be sure this doesn't interfere with
	 * {@link #setBuffer(AudioBuffer)}
	 *
	 * @return The loaded buffer
	 */
	synchronized AudioBuffer getOutBuffer()
	{
		return this.out;
	}

	/**
	 * Synchronized to be sure this doesn't interfere with
	 * {@link #setBuffer(AudioBuffer)}
	 *
	 * @return True if the buffer is loaded
	 */
	synchronized boolean isLoadingOver()
	{
		return this.out != null;
	}

	synchronized int getToSkip()
	{
		return this.toSkip;
	}
}
