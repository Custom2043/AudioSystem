package audio;

import java.util.Iterator;
import java.util.List;

import org.lwjgl.openal.AL10;

import util.CustomTimer;
import util.Logger;
import util.UpdateList;

class CommandThread extends Thread
{
	private boolean continu;
	public UpdateList<Command> commandList = new UpdateList<>();
	private static CommandThread instance = new CommandThread();
	private UpdateList<LoadingThread> loadings = new UpdateList<>();
	private CustomTimer timer = new CustomTimer();
	private boolean oneLoadingOver = false;
	private int refreshPeriod = 1000000;

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
			if (this.timer.getDifference() >= this.refreshPeriod)
			{
				for (Iterator<Source> iter = AudioSystem.getSourcesIterator() ; iter.hasNext() ; )
				{ // Update every playing source (load new buffer, discard read ones or starts loading)
					Source source = iter.next();
					this.updatePlayingSource(source);

					int error;
			        if ((error = AL10.alGetError()) != 0)
			        {
			        	Logger.error("Error during updating source "+source.getOpenALSourceID()+", OpenAL error : "+error);
			        	AudioSystem.setError(error);
			        }
				}

				for (Source s : AudioSystem.getSources())
				{ // Handle callbacks of all sources
					List<SourceCallBack> l = s.getCallBack().getList();
					for (SourceCallBack sc : l)
						try {sc.callback();}
						catch(Exception e) {Logger.error("Error during source "+s.getOpenALSourceID()+" callback : "+sc, e);}
				}

				this.timer.setValue(this.timer.getDifference() % this.refreshPeriod);
			}

			for (Iterator<LoadingThread> iter = this.loadings.getList().iterator();iter.hasNext();)
			{ // Handle finished loading
				LoadingThread th = iter.next();
				if (th.isLoadingOver())
				{
					this.handleLoadedBuffer(th);
					iter.remove();

					int error;
			        if ((error = AL10.alGetError()) != 0)
			        {
			        	Logger.error("Error during loading source "+th.source.getOpenALSourceID()+", OpenAL error : "+error);
			        	AudioSystem.setError(error);
			        }
				}
			}
			
			for (Iterator<Command> iter = this.commandList.getList().iterator();iter.hasNext() && this.continu;)
			{ // Handle commands (Error are handle by the command itself)
				Command c = iter.next();
				c.handle();
				c.setEnded();
				iter.remove();
			}

			if (this.continu && this.commandList.getList().size() == 0 && !this.atLeastOneloadingOver())
				try {
					Thread.sleep(Math.max(0, this.refreshPeriod - this.timer.getDifference()));
				} catch(InterruptedException e) {}
		}
	}
	void quit()
	{
		this.continu = false;
	}
	void addCommand(Command c)
	{
		this.commandList.add(c);
		this.interrupt();
	}
	synchronized boolean atLeastOneloadingOver()
	{
		boolean l = this.oneLoadingOver;
		this.oneLoadingOver = false;
		return l;
	}
	synchronized void notifyLoadingOver()
	{
		this.oneLoadingOver = true;
	}
	void addLoading(AutomaticSource source, int toSkip)
	{
		if (!source.isSourceLoading())
		{
			Logger.debug("Start loading source "+source.getOpenALSourceID());

			List<SourceCallBack> l = source.getCallBack().getList();
			for (SourceCallBack sc : l)
				sc.startsLoading(toSkip);

			source.setLoading(true);
			this.loadings.add(new LoadingThread(source, toSkip));
			this.interrupt();
		}
	}
	void removeLoadingForSource(AutomaticSource s)
	{
		Logger.debug("Stop loading source "+s.getOpenALSourceID());
		for (Iterator<LoadingThread> iter = this.loadings.getList().iterator();iter.hasNext();)
			if (iter.next().source == s)
				iter.remove(); // Doesn't stop it, but make sure we don't with the output buffer
	}
	void stopAllLoadings()
	{
		this.loadings = new UpdateList<>();
	}
	boolean shouldContinu()
	{
		return this.continu;
	}
	void updatePlayingSource(Source source)
	{
		if (source instanceof StreamingSource)
		{
			StreamingSource streamingSource = (StreamingSource)source;
			int processed = AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_BUFFERS_PROCESSED);
			if (processed > 0)
			{
				if (streamingSource.getCurrentCodec().isStreamOver())
					if (streamingSource.isLooping())
					{
						streamingSource.addLoop();
						for (SourceCallBack sc : streamingSource.getCallBack().getList())
							sc.looped();
					}
					else
						streamingSource.setShouldBePlaying(false);

				if (streamingSource.shouldBePlaying() && !streamingSource.isSourceLoading())
					this.addLoading(streamingSource, 0);

				for (int i=0;i<processed;i++)
				{
					AL10.alSourceUnqueueBuffers(streamingSource.getOpenALSourceID());
					AudioBuffer buffer = streamingSource.removeBufferData();
					for (SourceCallBack sc : streamingSource.getCallBack().getList())
						sc.bufferProcessed(buffer);
				}
			}
		}
		if (source instanceof ManualSource)
		{
			int processed = AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_BUFFERS_PROCESSED);
			for (int i=0;i<processed;i++)
			{
				AL10.alSourceUnqueueBuffers(source.getOpenALSourceID());
				AudioBuffer buffer = ((ManualSource)source).removeBufferData();
				for (SourceCallBack sc : ((ManualSource)source).getCallBack().getList())
					sc.bufferProcessed(buffer);
			}
		}
	}
	void handleLoadedBuffer(LoadingThread th)
	{
		Logger.debug("Loaded a "+th.getOutBuffer().toByteBuffer().limit()+" buffer for source "+th.source.getOpenALSourceID());
		th.source.setLoading(false);
		AudioBuffer buf = th.getOutBuffer();

		if (buf.getLimit() == 0)
			return;

		if (th.getToSkip() != 0)
		{ // We need to get rid of the current buffers
			AL10.alSourceStop(th.source.getOpenALSourceID());
			Command.CommandDeleteSource.deleteBuffersFromSource(th.source);
		}

		buf.setOpenALBufferID(AL10.alGenBuffers());
		AL10.alBufferData(buf.getOpenALBufferID(), buf.getCodec().getALFormat(), buf.toByteBuffer(), buf.getCodec().getSamplerate());
		
		if (th.source instanceof SoundSource)
			AL10.alSourcei(th.source.getOpenALSourceID(), AL10.AL_BUFFER, buf.getOpenALBufferID());
		else
			AL10.alSourceQueueBuffers(th.source.getOpenALSourceID(), buf.getOpenALBufferID());
		
		th.source.pushBuffer(buf);

		// If the source is still missing buffers, we start a new loading
		if (th.source instanceof StreamingSource)
			if (((StreamingSource)th.source).getBufferNumber() - ((StreamingSource)th.source).getSourceBuffers().length > 0)
				this.addLoading(th.source, 0);

		if (th.source.shouldBePlaying() && AL10.alGetSourcei(th.source.getOpenALSourceID(), AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING)
			AL10.alSourcePlay(th.source.getOpenALSourceID());

		List<SourceCallBack> l = th.source.getCallBack().getList();
		for (SourceCallBack sc : l)
			sc.bufferLoaded(buf);
	}
}
