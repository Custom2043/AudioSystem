package test;

import audio.AudioSystem;
import audio.CodecMP3;
import util.FileInputStreamSource;
import util.Logger;

public class AudioSystemTester {

	public static int sourceId;
	public static void main(String[] args) throws InterruptedException
	{
		AudioSystem.init();

		AudioSystem.setDefaultCodec(CodecMP3.class);

		Logger.setLoggerProperties(true, true, true, true);

		AudioSystem.setDefaultBuffers(3, 16384, -1);

		sourceId = AudioSystem.newStreamingSource(new FileInputStreamSource("welcome.mp3"), 1, false);

		AudioSystem.play(sourceId);

		while (AudioSystem.loading(sourceId) || AudioSystem.playing(sourceId))
			Thread.sleep(20);

		AudioSystem.quit();
	}
}
