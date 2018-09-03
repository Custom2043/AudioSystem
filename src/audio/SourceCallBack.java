package audio;

import util.CustomTimer;

public abstract class SourceCallBack
{
	private Source source;
	private CustomTimer timer = new CustomTimer();
	public SourceCallBack() {}

	void setSource(Source source)
	{
		this.source = source;
	}
	public Source getSource()
	{
		return this.source;
	}
	public CustomTimer getTimer()
	{
		return this.timer;
	}

	public abstract void callback();

	public abstract void bufferLoaded(AudioBuffer loaded);

	public abstract void startsLoading(int skipping);

	public abstract void bufferProcessed(AudioBuffer buffer);

	public abstract void looped();
}
