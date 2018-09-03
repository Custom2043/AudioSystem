package test;

import audio.AudioSystem;
import audio.CodecMP3;
import audio.StreamingSource;
import util.FileInputStreamSource;
import util.Logger;

public class AudioSystemTester {

	public static StreamingSource source;
	public static void main(String[] args) throws InterruptedException
	{
		AudioSystem.init();

		AudioSystem.setDefaultCodec(CodecMP3.class);

		Logger.setLoggerProperties(true, true, true, true);

		AudioSystem.setDefaultBuffers(3, 16384, -1);

		source = AudioSystem.newStreamingSource(new FileInputStreamSource("Imprinting.mp3"));

		source.play();

		Thread.sleep(3000);
		
		//while (source.loading() || source.playing())
			//Thread.sleep(20);

		AudioSystem.quit();
	}
}
