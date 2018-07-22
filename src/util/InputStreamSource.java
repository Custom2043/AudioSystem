package util;

import java.io.InputStream;

public interface InputStreamSource
{
	public boolean canStreamBeRetrieved();
	public InputStream getStreamBack();
}
