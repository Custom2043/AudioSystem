package audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Iterator;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.util.vector.Vector3f;

import util.InputStreamSource;
import util.Logger;

abstract class Command
{
	private static boolean GET = true,
						   SET = false;
	private boolean ended = false;

	Command(){}

	synchronized void setEnded() {this.ended = true;this.notify();}
	synchronized boolean isEnded() {return this.ended;}

	synchronized void setWaiting()
    {
    	while (!this.isEnded())
	    	try{this.wait();}
			catch (InterruptedException e){e.printStackTrace();}
    }

    static synchronized <Type extends Command> Type executeInThread(Type t)
    {
    	if (!AudioSystem.isInitialized())
    		AudioSystem.init();

    	if (!AudioSystem.isInitialized() || !CommandThread.getThread().shouldContinu())
    	{
    		Logger.error("System isn't started");
    		return t;
    	}

    	if (t instanceof SourceCommand && ((SourceCommand)t).source == null)
    	{
    		Logger.warning("Unexistent source "+((SourceCommand)t).source.getOpenALSourceID());
    		return t;
    	}

        CommandThread.getThread().addCommand(t);
        t.setWaiting();

        int error;
        if ((error = AL10.alGetError()) != 0)
        {
        	Logger.error("Error in command "+t.getClass().getSimpleName()+", OpenAL error : "+error);
        	AudioSystem.setError(error);
        }

        return t;
    }

	abstract void handle();

	static abstract class SourceCommand<T extends Source> extends Command
	{
		final T source;
		SourceCommand(T source)
		{
			this.source = source;
		}
	}
	static abstract class GetSetCommand<T extends Source> extends SourceCommand<T>
	{
		final boolean get;
		GetSetCommand(boolean g, T source)
		{
			super(source);
			this.get = g;
		}
	}

	static class CommandCleanUp extends Command
	{
		CommandCleanUp(){}
		@Override
		void handle()
		{
			CommandThread.getThread().quit();
			CommandThread.getThread().stopAllLoadings();
			for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
				CommandDeleteSource.deleteSource(iter.next());
		}
	}

	static class CommandNewSource extends Command
	{
		Source source = null;
		final InputStreamSource streamSource;
		final Class<? extends Codec> codec;
		final int bufferSize, bufferNumber;

		CommandNewSource()
		{
			this.streamSource = null;
			this.codec = null;
			this.bufferSize = 0;
			this.bufferNumber = 0;
		}

		CommandNewSource(InputStreamSource streamSource, int bS, int bN, Class<? extends Codec> c)
		{
			this.streamSource = streamSource;
			this.bufferSize = bS;
			this.bufferNumber = bN; // Source
			this.codec = c;
		}

		@Override
		void handle()
		{
			int sourceId = AL10.alGenSources();
			int error;
			if ((error = AL10.alGetError()) != AL10.AL_NO_ERROR)
			{
				Logger.warning("Couldn't create the source, error "+error);
				AudioSystem.setError(error);
				return;
			}

			if (this.streamSource == null)
				this.source = new ManualSource(sourceId);
			else if (this.bufferNumber != 0)
				this.source = new StreamingSource(sourceId, this.streamSource, this.bufferSize, this.bufferNumber, this.codec);
			else
				this.source = new SoundSource(sourceId, this.streamSource, this.bufferSize, this.codec);

			AudioSystem.addSource(this.source);
		}
	}
	static class CommandDeleteSource extends SourceCommand
	{
		CommandDeleteSource(Source source) {super(source);}
		@Override
		void handle()
		{
			deleteSource(this.source);

			for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
				if (iter.next() == this.source)
					iter.remove();
		}
		static void deleteSource(Source source)
		{
			AL10.alSourceStop(source.getOpenALSourceID());

			deleteBuffersFromSource(source);

			AL10.alDeleteSources(source.getOpenALSourceID()); //NE SUPPRIMER PAS LES BUFFERS !
		}
		static void deleteBuffersFromSource(Source source)
		{
			if (source.isAutomatic() && ((AutomaticSource)source).isSourceLoading())
				CommandThread.getThread().removeLoadingForSource((AutomaticSource)source);

			int sourceType = AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_SOURCE_TYPE);
			
			if (sourceType == AL11.AL_STREAMING)
			{
				int numberOfBuffers = AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_BUFFERS_QUEUED);
				for (int i=0;i<numberOfBuffers;i++)
				{
					int bufferID = AL10.alSourceUnqueueBuffers(source.getOpenALSourceID());
					if (getBufferUse(bufferID) == 1)
						AL10.alDeleteBuffers(bufferID);
				}
			}
			else if (sourceType == AL11.AL_STATIC)
			{
				int bufferID = AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_BUFFER);
				AL10.alSourcei(source.getOpenALSourceID(), AL10.AL_BUFFER, 0);
				if (getBufferUse(bufferID) == 1)
					AL10.alDeleteBuffers(bufferID);
			}
			
		}
		static int getBufferUse(int bufferID)
		{
			int use = 0;
			for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
				for (AudioBuffer a : iter.next().getSourceBuffers())
					if (a.getOpenALBufferID() == bufferID)
						use ++;
			return use;
		}
	}

	static class CommandPlay extends SourceCommand
	{
		CommandPlay(Source source) {super(source);}
		@Override
		void handle()
		{
			setPlaying(this.source, true);
		}
		static void setPlaying(Source source, boolean state)
		{
			if (state)
			{
				Logger.debug("Play source "+source.getOpenALSourceID());
				if (!source.isAutomatic() || !((AutomaticSource)source).isSourceLoading())
					AL10.alSourcePlay(source.getOpenALSourceID());
			}
			else
			{
				Logger.debug("Pause source "+source.getOpenALSourceID());
				AL10.alSourcePause(source.getOpenALSourceID());
			}
			source.setShouldBePlaying(state);
		}
	}
	static class CommandStop extends SourceCommand
	{
		CommandStop(Source source) {super(source);}
		@Override
		void handle()
		{
			CommandPlay.setPlaying(this.source, false);
		}
	}
	static class CommandPause extends SourceCommand
	{
		CommandPause(Source source) {super(source);}
		@Override
		void handle()
		{
			CommandPlay.setPlaying(this.source, !this.source.shouldBePlaying());
		}
	}
	static class CommandPlaying extends SourceCommand
	{
		boolean playing;
		CommandPlaying(Source source) {super(source);}
		@Override
		void handle()
		{
			this.playing = playing(this.source);
		}
		static boolean playing(Source source)
		{
			return AL10.alGetSourcei(source.getOpenALSourceID(), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
		}
	}
	static class CommandLoading extends SourceCommand<AutomaticSource>
	{
		boolean loading;
		CommandLoading(AutomaticSource source) {super(source);}
		@Override
		void handle()
		{
			this.loading = this.source.isSourceLoading();
		}
	}

	static class CommandVolume extends GetSetCommand
	{
		float value;
		CommandVolume(Source source, float v){super(SET, source);this.value = v;}
		CommandVolume(Source source){super(GET, source);}
		@Override
		void handle()
		{
			if (this.get)
				this.value = this.source.getSourceVolume();
			else
			{
				this.source.setSourceVolume(this.value);
				AL10.alSourcef(this.source.getOpenALSourceID(), AL10.AL_GAIN, this.value * AudioSystem.masterVolume);
			}
		}
	}

	static class CommandFloat extends GetSetCommand
	{
		float value;int dataType;
		CommandFloat(Source source, int data, float v){super(SET, source);this.value = v;this.dataType = data;}
		CommandFloat(Source source, int data){super(GET, source);this.dataType = data;}
		@Override
		void handle()
		{
			if (this.get)
				this.value = AL10.alGetSourcef(this.source.getOpenALSourceID(), this.dataType);
			else
				AL10.alSourcef(this.source.getOpenALSourceID(), this.dataType, this.value);
		}
	}

	static class CommandVector extends GetSetCommand
	{
		Vector3f value; int dataType;
		CommandVector(Source source, int data, Vector3f p){super(SET, source);this.value = p; this.dataType = data;}
		CommandVector(Source source, int data){super(GET, source);this.dataType = data;}
		@Override
		void handle()
		{
			if (this.get)
			{
				ByteBuffer temp = ByteBuffer.allocateDirect(12);
				temp.order(ByteOrder.nativeOrder());
				FloatBuffer buf = temp.asFloatBuffer();
				AL10.alGetSourcef(this.source.getOpenALSourceID(), this.dataType, buf);
				this.value = new Vector3f(buf.get(), buf.get(), buf.get());
			}
			else
				AL10.alSource3f(this.source.getOpenALSourceID(), this.dataType, this.value.x, this.value.y, this.value.z);
		}
	}

	static class CommandLoop extends GetSetCommand<AutomaticSource>
	{
		boolean value; int number;
		CommandLoop(AutomaticSource source, boolean v){super(SET, source);this.value = v;}
		CommandLoop(AutomaticSource source){super(GET, source);}
		@Override
		void handle()
		{
			if (this.get)
			{
				this.value = this.source.isLooping();
				this.number = this.source.getLoopNumber();
			}
			else
				this.source.setLooping(this.value);
		}
	}

	static class CommandRelative extends GetSetCommand
	{
		boolean value; int number;
		CommandRelative(Source source, boolean v){super(SET, source);this.value = v;}
		CommandRelative(Source source){super(GET, source);}
		@Override
		void handle()
		{
			if (this.get)
				AL10.alSourcei(this.source.getOpenALSourceID(), AL10.AL_SOURCE_RELATIVE, this.value ? AL10.AL_TRUE : AL10.AL_FALSE);
			else
				this.value = AL10.alGetSourcei(this.source.getOpenALSourceID(), AL10.AL_SOURCE_RELATIVE) == AL10.AL_TRUE;
		}
	}

	static class CommandMasterVolume extends Command
	{
		float value;
		boolean get;
		public CommandMasterVolume(float value) {this.get = false; this.value = value;}
		public CommandMasterVolume() {this.get = true;}

		@Override
		void handle()
		{
			if (this.get)
				this.value = AudioSystem.masterVolume;
			else
			{
				if (this.value < 0 || this.value > 1)
				{
					AudioSystem.setError(AudioSystem.ERROR_INVALID_VALUE);
					return;
				}

				AudioSystem.masterVolume = this.value;
				Source s;
				for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
					AL10.alSourcef((s = iter.next()).getOpenALSourceID(), AL10.AL_GAIN, this.value * s.getSourceVolume());
			}
		}
	}

	static class CommandGetBuffer extends SourceCommand
	{
		AudioBuffer[] buf;

		CommandGetBuffer(Source source)
		{
			super(source);
		}

		@Override
		void handle()
		{
			this.buf = ((AutomaticSource)this.source).getSourceBuffers();
		}
    }
    static class CommandGetDatas extends SourceCommand<AutomaticSource>
    {
    	int channels, samplerate, sampleSize, totalSize;
    	CommandGetDatas(AutomaticSource source){super(source);}
    	@Override
		void handle()
    	{
    		//Codec can change, so we need to put this on the thread
    		this.channels = this.source.getCurrentCodec().getChannelsNumber();
    		this.samplerate = this.source.getCurrentCodec().getSamplerate();
    		this.sampleSize = this.source.getCurrentCodec().getBitsPerSample();
    		this.totalSize = this.source.getCurrentCodec().getTotalSize();
    	}
    }
    static class CommandAttenuationMod extends Command
	{
		int value;
		boolean get;
		public CommandAttenuationMod(int value) {this.get = false; this.value = value;}
		public CommandAttenuationMod() {this.get = true;}

		@Override
		void handle()
		{
			if (this.get)
				this.value = AudioSystem.distanceModel;
			else
				AL10.alDistanceModel(AudioSystem.distanceModel = this.value);
		}
	}

    static class CommandOffset extends GetSetCommand
	{
		int value;
		CommandOffset(Source source) {super(GET, source);}
		CommandOffset(Source source, int v) {super(SET, source);this.value=v;}
		@Override
		void handle()
		{
			if (this.get)
				this.value = AL10.alGetSourcei(this.source.getOpenALSourceID(), AL11.AL_SAMPLE_OFFSET);
			else if (!(this.source instanceof StreamingSource))
				AL10.alSourcei(this.source.getOpenALSourceID(), AL11.AL_SAMPLE_OFFSET, this.value);
			else
			{
				StreamingSource ss = (StreamingSource)this.source;
				if (this.value >= ss.getProcessedBuffer() * ss.getBufferSize() / ss.getCurrentCodec().getSampleSize() &&
						this.value < (ss.getProcessedBuffer() + ss.getSourceBuffers().length) * ss.getBufferSize() / ss.getCurrentCodec().getSampleSize())
				{
					int bufferPrior = this.value - ss.getProcessedBuffer() * ss.getBufferSize() / ss.getCurrentCodec().getSampleSize();
					bufferPrior = bufferPrior / (ss.getBufferSize() / ss.getCurrentCodec().getSampleSize());
					
					if (ss.shouldBePlaying() && !ss.isSourceLoading())
						CommandThread.getThread().addLoading(ss, 0);

					// We remove unused buffers
					for (int i=0;i<bufferPrior;i++)
					{
						AL10.alSourceUnqueueBuffers(ss.getOpenALSourceID());
						ss.removeBufferData();
					}
					
					AL10.alSourcei(this.source.getOpenALSourceID(), AL11.AL_SAMPLE_OFFSET, this.value - (ss.getProcessedBuffer() + bufferPrior) * ss.getBufferSize() / ss.getCurrentCodec().getSampleSize());
				}
				else
				{ // Need to reload the stream because the value is before the earliest buffer in memory
					CommandThread.getThread().removeLoadingForSource(ss);
					if (!((StreamingSource)this.source).canLoop())
					{
						AudioSystem.setError(AudioSystem.ERROR_CANT_READ_STREAM);
						return;
					}
					CommandThread.getThread().addLoading((StreamingSource)this.source, this.value);
				}
			}
		}
	}
}
