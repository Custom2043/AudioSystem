package audio;

import static audio.AudioSystem.getSource;

import java.util.Iterator;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import util.CustomTimer;
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
    		Logger.warning("System isn't started");
    		return t;
    	}

    	if (t instanceof SourceCommand && ((SourceCommand)t).source == null)
    	{
    		Logger.warning("Unexistent source "+((SourceCommand)t).sourceId);
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

	static abstract class SourceCommand extends Command
	{
		final int sourceId;
		final Source source;
		SourceCommand(int sid)
		{
			this.sourceId = sid;
			this.source = getSource(this.sourceId);
		}
	}
	static abstract class GetSetCommand extends SourceCommand
	{
		final boolean get;
		GetSetCommand(boolean g, int sourceId)
		{
			super(sourceId);
			this.get = g;
		}
	}

	static class CommandCleanUp extends Command
	{
		CommandCleanUp(){}
		@Override
		void handle()
		{
			for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
				CommandDeleteSource.deleteSource(iter.next(), false);
		}
	}

	static class CommandNewSource extends Command
	{
		int sourceId;
		final InputStreamSource streamSource;
		final boolean loop;
		final Class<? extends Codec> codec;
		final int bufferSize, bufferNumber;
		final float volume;

		CommandNewSource(InputStreamSource streamSource, float volume, boolean loop, int bS, int bN, Class<? extends Codec> c)
		{
			this.streamSource = streamSource;
			this.volume = volume;
			this.loop = loop;
			this.bufferSize = bS;
			this.bufferNumber = bN; // Source
			this.codec = c;
		}

		@Override
		void handle()
		{
			this.sourceId = AL10.alGenSources();
			int error;
			if ((error = AL10.alGetError()) != AL10.AL_NO_ERROR)
			{
				Logger.warning("Couldn't create the source, error "+error);
				AudioSystem.setError(error);
				this.sourceId = -1;
				return;
			}

			Source source = null;
			if (this.bufferNumber != 0)
				source = new StreamingSource(this.sourceId, this.streamSource, this.volume, this.loop, this.bufferSize, this.bufferNumber, this.codec);
			else
				source = new SoundSource(this.sourceId, this.streamSource, this.volume, this.loop, this.bufferSize, this.codec);

			AudioSystem.addSource(source);
		}
	}
	static class CommandDeleteSource extends SourceCommand
	{
		CommandDeleteSource(int sid) {super(sid);}
		@Override
		void handle()
		{
			deleteSource(this.source, true);
		}
		static void deleteSource(Source source, boolean remove)
		{
			if (CommandPlaying.playing(source))
				CommandPlay.setPlaying(source, false);

			deleteBuffersFromSource(source);

			AL10.alDeleteSources(source.getSourceID()); //NE SUPPRIMER PAS LES BUFFERS !

			if (remove) // Can be call by a loop so we may not want to modify the iterator
				for (Iterator<Source> iter = AudioSystem.getSourcesIterator();iter.hasNext();)
					if (iter.next() == source)
						iter.remove();
		}
		static void deleteBuffersFromSource(Source source)
		{
			if (source.isLoading())
				CommandThread.getThread().removeLoadingForSource(source);

			int numberOfBuffers = AL10.alGetSourcei(source.getSourceID(), AL10.AL_BUFFERS_QUEUED);

			for (int i=0;i<numberOfBuffers;i++)
				AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source.getSourceID()));
		}
	}

	static class CommandPlay extends SourceCommand
	{
		CommandPlay(int sid) {super(sid);}
		@Override
		void handle()
		{
			setPlaying(this.source, true);
		}
		static void setPlaying(Source source, boolean state)
		{
			if (state)
			{
				Logger.debug("Play source "+source.getSourceID());
				if (!source.isLoading())
					AL10.alSourcePlay(source.getSourceID());
			}
			else
			{
				Logger.debug("Pause source "+source.getSourceID());
				AL10.alSourcePause(source.getSourceID());
			}
			source.setShouldPlay(state);
		}
	}
	static class CommandStop extends SourceCommand
	{
		CommandStop(int sid) {super(sid);}
		@Override
		void handle()
		{
			CommandPlay.setPlaying(this.source, false);
		}
	}
	static class CommandPause extends SourceCommand
	{
		CommandPause(int sid) {super(sid);}
		@Override
		void handle()
		{
			CommandPlay.setPlaying(this.source, !this.source.shouldPlay());
		}
	}
	static class CommandPlaying extends SourceCommand
	{
		boolean playing;
		CommandPlaying(int sid) {super(sid);}
		@Override
		void handle()
		{
			this.playing = playing(this.source);
		}
		static boolean playing(Source source)
		{
			return AL10.alGetSourcei(source.getSourceID(), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
		}
	}
	static class CommandLoading extends SourceCommand
	{
		boolean loading;
		CommandLoading(int sid) {super(sid);}
		@Override
		void handle()
		{
			this.loading = this.source.isLoading();
		}
	}

	static class CommandFloat extends GetSetCommand
	{
		float value;int dataType;
		CommandFloat(int sourceId, int data, float v){super(SET, sourceId);this.value = v;this.dataType = data;}
		CommandFloat(int sourceId, int data){super(GET, sourceId);this.dataType = data;}
		@Override
		void handle()
		{
			if (this.get)
				this.value = AL10.alGetSourcef(this.source.getSourceID(), this.dataType);
			else
				AL10.alSourcef(this.source.getSourceID(), this.dataType, this.value);
		}
	}

	static class CommandLoop extends GetSetCommand
	{
		boolean value; int number;
		CommandLoop(int sourceId, boolean v){super(SET, sourceId);this.value = v;}
		CommandLoop(int sourceId){super(GET, sourceId);}
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
	/*static class CommandVector extends GetSetCommand
	{
		Vector3f vec; int dataType;
		CommandVector(int sourceId, int data, Vector3f p){super(SET, sourceId);this.vec = p; this.dataType = data;}
		CommandVector(int sourceId, int data){super(GET, sourceId);this.dataType = data;}
		@Override
		void handle()
		{
			if (this.get)
			{
				FloatBuffer buf = CommandThread.createFloatBuffer(3);
				AL10.alGetSourcef(this.source.getSourceID(), this.dataType, buf);
				this.vec = new Vector3f(buf.get(), buf.get(), buf.get());
			}
			else
				AL10.alSource3f(this.source.getSourceID(), this.dataType, this.vec.x, this.vec.y, this.vec.z);
		}
	}*/
	/*static class CommandAttenuationMod extends Command
	{
		int atte; boolean act;
		CommandAttenuationMod(){this.act = GET;}
		CommandAttenuationMod(int a){this.act = SET;this.atte = a;}
		@Override
		void handle()
		{
			if (this.act == GET)
				this.atte = AL10.alGetInteger(AL10.AL_DISTANCE_MODEL);
			else
			{
				if (this.atte == AudioSystem.NO_ATTE)
					AL10.alDistanceModel(AL10.AL_NONE);
				if (this.atte == AudioSystem.LINEAR_ATTE)
					AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
				if (this.atte == AudioSystem.LINEAR_ATTE_CLAMPED)
					AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE_CLAMPED);
				if (this.atte == AudioSystem.EXPO_ATTE)
					AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE);
				if (this.atte == AudioSystem.EXPO_ATTE_CLAMPED)
					AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE_CLAMPED);
				if (this.atte == AudioSystem.INVERSE_ATTE)
					AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE);
				if (this.atte == AudioSystem.INVERSE_ATTE_CLAMPED)
					AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);
			}
		}
	}*/
	static class CommandGetBuffer extends SourceCommand
	{
		AudioBuffer[] buf;

		CommandGetBuffer(int sid)
		{
			super(sid);
		}

		@Override
		void handle()
		{
			this.buf = this.source.getBufferData();
		}
    }
    static class CommandGetDatas extends SourceCommand
    {
    	int channels, samplerate, sampleSize, totalSize;
    	CommandGetDatas(int sid){super(sid);}
    	@Override
		void handle()
    	{
    		//Codec can change, so we need tu put this of the thread
    		this.channels = this.source.getCurrentCodec().getChannelsNumber();
    		this.samplerate = this.source.getCurrentCodec().getSamplerate();
    		this.sampleSize = this.source.getCurrentCodec().getBitsPerSample();
    		this.totalSize = this.source.getCurrentCodec().getTotalSize();
    	}
    }
	static class CommandPosition extends GetSetCommand
	{
		int value;
		CommandPosition(int sourceId) {super(GET, sourceId);}
		CommandPosition(int sourceId, int v) {super(SET, sourceId);this.value=v;}
		@Override
		void handle()
		{
			if (this.get)
				this.value = AL10.alGetSourcei(this.source.getSourceID(), AL11.AL_BYTE_OFFSET);
			else if (!this.source.streaming())
				AL10.alSourcei(this.source.getSourceID(), AL11.AL_BYTE_OFFSET, this.value);
			else
			{
				if (!this.source.canLoop())
				{
					AudioSystem.setError(AudioSystem.ERROR_CANT_READ_STREAM);
					return;
				}
				CommandThread.getThread().addLoading(this.source, this.value);
			}
		}
	}
	static class CommandVolumeFading
	{
		private final CustomTimer timer;
		final int sourceId, milli;
		final float baseVolume, endVolume;
		CommandVolumeFading(int sid, float bV, float eV, int milli)
		{
			this.sourceId = sid;
			this.baseVolume = bV; this.endVolume = eV; this.milli = milli;
			this.timer = new CustomTimer();
		}
		int getMilli()
		{
			return (int)this.timer.getDifference();
		}
	}


}
