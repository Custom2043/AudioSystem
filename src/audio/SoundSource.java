package audio;

import org.lwjgl.openal.AL10;

import util.InputStreamSource;

public class SoundSource extends AutomaticSource<SoundSource>
{
	private AudioBuffer audioBuffer = null;
	SoundSource(int sourceID, InputStreamSource streamSource,
			int bufferSize, Class<? extends Codec> codec)
	{
		super(sourceID, streamSource, bufferSize, codec);
	}

	@Override
	public void setLooping(boolean loop)
	{
		AL10.alSourcei(this.getOpenALSourceID(), AL10.AL_LOOPING, loop ? 1 : 0);
	}

	@Override
	public boolean isLooping()
	{
		return AL10.alGetSourcei(this.getOpenALSourceID(), AL10.AL_LOOPING) == 1;
	}

	@Override
	public boolean canLoop()
	{
		return true;
	}

	@Override
	void pushBuffer(AudioBuffer buffer)
	{
		this.audioBuffer = buffer;
	}

	@Override
	public AudioBuffer[] getSourceBuffers()
	{
		return new AudioBuffer[]{this.audioBuffer};
	}
}
