package de.piegames.voicepi;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.google.gson.JsonObject;
import de.piegames.voicepi.audio.FileAudio;
import de.piegames.voicepi.stt.SphinxRecognizer;
import de.piegames.voicepi.tts.QueueEngine;

public class SphinxTest {

	protected VoicePi			control;
	protected SphinxRecognizer	stt;
	protected QueueEngine		tts;
	protected Thread			t;
	protected FileAudio			audio;
	protected final long		timeout	= 2;
	protected Configuration		config;

	/** All tests have to be in one method, because of the way FileAudio works */
	@Test
	public void allTests() throws URISyntaxException, InterruptedException {
		config = new Configuration(Paths.get(getClass().getResource("/testconfig.json").toURI()));
		config.setAudio(audio = new FileAudio(null, new File(getClass().getResource("/commands1.wav").toURI()), null));
		control = new VoicePi(config);
		config.setSTT(new SphinxRecognizer(control, new JsonObject()));
		control.reload();
		stt = (SphinxRecognizer) control.getSTT();
		tts = (QueueEngine) control.getTTS();

		t = new Thread(control);
		t.start();

		// ACTIVATE
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		// RELOAD
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Reloading", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		Thread.sleep(5000);
		stt = (SphinxRecognizer) control.getSTT();
		tts = (QueueEngine) control.getTTS();

		config.setAudio(audio = new FileAudio(null, new File(getClass().getResource("/commands2.wav").toURI()), null));
		control.reload();
		stt = (SphinxRecognizer) control.getSTT();
		tts = (QueueEngine) control.getTTS();

		// ACTIVATE
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		// EXIT
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Stopping VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		t.join();
	}
}