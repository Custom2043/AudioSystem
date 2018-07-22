package util;

import java.io.InputStream;

public class SingleInputStreamSource implements InputStreamSource
{
	private boolean used = false;
	private InputStream stream;
	public SingleInputStreamSource(InputStream is)
	{
		this.stream = is;
	}

	@Override
	public boolean canStreamBeRetrieved()
	{
		return !this.used;
	}

	@Override
	public InputStream getStreamBack()
	{
		if (this.used)
			return null;

		this.used = true;
		return this.stream;
	}

}
