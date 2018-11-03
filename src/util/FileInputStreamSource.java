package util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileInputStreamSource implements InputStreamSource
{
	private File file;
	public FileInputStreamSource(String s)
	{
		this(new File(s));
	}
	public FileInputStreamSource(File f)
	{
		this.file = f;
	}
	@Override
	public boolean canStreamBeRetrieved()
	{
		return this.file.exists();
	}

	@Override
	public InputStream getStreamBack()
	{
		try
		{
			return new BufferedInputStream(new FileInputStream(this.file));
		}
		catch(Exception e){Logger.error("Can't create stream : "+this.file.toString());Logger.error(e);}
		return null;
	}
}
