package audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;

import org.lwjgl.openal.AL10;

import audio.Command.CommandVolumeFading;
import util.Logger;
import util.UpdateList;

class CommandThread extends Thread
{
	private boolean continu;
	public UpdateList<Command> list = new UpdateList<>();
	private static CommandThread instance = new CommandThread();
	public UpdateList<CommandVolumeFading> fadings = new UpdateList<>();
	private UpdateList<LoadingThread> loadings = new UpdateList<>();

	public static CommandThread getThread()
	{
		return instance;
	}
	private CommandThread()
	{
		this.continu = true;
	}
	@Override
	public void run()
	{
		while (this.continu)
		{
			for (Iterator<Source> iter = AudioSystem.getSourcesIterator() ; iter.hasNext() ; )
			{
				Source source = iter.next();
				if (source.streaming())
				{
					int processed = AL10.alGetSourcei(source.getSourceID(), AL10.AL_BUFFERS_PROCESSED);
					if (processed > 0)
					{
						if (source.getCurrentCodec().isStreamOver())
							if (source.isLooping())
								source.addLoop();
							else
								source.setShouldPlay(false);

						if (source.shouldPlay() && !source.isLoading())
							this.addLoading(source, 0);

						for (int i=0;i<processed;i++)
						{
							AL10.alSourceUnqueueBuffers(source.getSourceID());
							source.getStreamingSource().removeBufferData();
						}
					}
				}
			}

			for (Iterator<Command> iter = this.list.getList().iterator();iter.hasNext();)
			{
				Command c = iter.next();
				c.handle();
				c.setEnded();
				iter.remove();
			}

			for (Iterator<LoadingThread> iter = this.loadings.getList().iterator();iter.hasNext();)
			{
				LoadingThread th = iter.next();
				if (th.isLoadingOver())
				{
					Logger.debug("Loaded a buffer for source "+th.source.getSourceID());
					Logger.debug("Buffer length "+th.getOutBuffer().toByteBuffer().limit());
					th.source.setLoading(false);
					AudioBuffer buf = th.getOutBuffer();

					if (buf.getLimit() == 0)
						continue;

					if (th.getToSkip() != 0)
					{ // We need to get rid of the current buffers
						AL10.alSourceStop(th.source.getSourceID());
						Command.CommandDeleteSource.deleteBuffersFromSource(th.source);
					}

					int bufferID = AL10.alGenBuffers();
					AL10.alBufferData(bufferID, buf.getCodec().getALFormat(), buf.toByteBuffer(), buf.getCodec().getSamplerate());
					AL10.alSourceQueueBuffers(th.source.getSourceID(), bufferID);
					th.source.pushBuffer(th.getOutBuffer());
					iter.remove();

					// If the source is still missing buffers, we start a new loading
					if (th.source.streaming())
						if (th.source.getStreamingSource().getBufferNumber() - th.source.getStreamingSource().getBufferData().length > 0)
							this.addLoading(th.source, 0);

					if (th.source.shouldPlay() && AL10.alGetSourcei(th.source.getSourceID(), AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING)
						AL10.alSourcePlay(th.source.getSourceID());

				}
			}

			for (Iterator<CommandVolumeFading> iter = this.fadings.getList().iterator();iter.hasNext();)
			{
				CommandVolumeFading fading = iter.next();
				float v = fading.baseVolume + (fading.endVolume - fading.baseVolume) * (fading.getMilli() / (float)fading.milli);
				new Command.CommandFloat(fading.sourceId, AL10.AL_GAIN, v).handle();
				if (fading.getMilli() >= fading.milli)
					iter.remove();
			}
			if (this.continu && this.list.getList().size() == 0)
				try {Thread.sleep(1);}
				catch (InterruptedException e) {}

		}
	}
	static IntBuffer createIntBuffer(int size)
	{
		ByteBuffer temp = ByteBuffer.allocateDirect(4 * size);
		temp.order(ByteOrder.nativeOrder());
		return temp.asIntBuffer();
	}
	static FloatBuffer createFloatBuffer(int size)
	{
		ByteBuffer temp = ByteBuffer.allocateDirect(4 * size);
		temp.order(ByteOrder.nativeOrder());
		return temp.asFloatBuffer();
	}
	void quit()
	{
		this.continu = false;
		try {this.join();}
		catch (InterruptedException e) {Logger.error(e);}
	}
	void addCommand(Command c)
	{
		this.list.add(c);
		this.interrupt();
	}
	void addLoading(Source source, int toSkip)
	{
		if (!source.isLoading())
		{
			Logger.debug("Start loading source "+source.getSourceID());
			source.setLoading(true);
			this.loadings.add(new LoadingThread(source, toSkip));
			this.interrupt();
		}
	}
	void removeLoadingForSource(Source s)
	{
		Logger.debug("Stop loading source "+s.getSourceID());
		for (Iterator<LoadingThread> iter = this.loadings.getList().iterator();iter.hasNext();)
			if (iter.next().source == s)
				iter.remove(); // Doesn't stop it, but make sure we don't with the output buffer
	}
	boolean shouldContinu()
	{
		return this.continu;
	}
}
