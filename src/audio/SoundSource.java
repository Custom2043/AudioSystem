package audio;

import org.lwjgl.openal.AL10;

import util.InputStreamSource;

class SoundSource extends Source
{
	private AudioBuffer audioBuffer = null;
	SoundSource(int sourceID, InputStreamSource streamSource, float volume, boolean loop,
			int bufferSize, Class<? extends Codec> codec)
	{
		super(sourceID, streamSource, volume, loop, bufferSize, codec);
	}

	@Override
	void setLooping(boolean loop)
	{
		AL10.alSourcei(this.getSourceID(), AL10.AL_LOOPING, loop ? 1 : 0);
	}

	@Override
	boolean isLooping()
	{
		return AL10.alGetSourcei(this.getSourceID(), AL10.AL_LOOPING) == 1;
	}

	@Override
	boolean canLoop()
	{
		return true;
	}

	@Override
	void pushBuffer(AudioBuffer buffer)
	{
		this.audioBuffer = buffer;
	}

	@Override
	AudioBuffer[] getBufferData()
	{
		return new AudioBuffer[]{this.audioBuffer};
	}
}
