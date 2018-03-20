package de.piegames.voicepi;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import de.piegames.voicepi.stt.QueueRecognizer;
import de.piegames.voicepi.tts.QueueEngine;

public class VoicePiTest {

	/**
	 * This test will load a simple configuration and say the activation command and the test command in different context states and will test if the module
	 * triggers all actions correctly.
	 */
	@Test
	public void testTest() throws URISyntaxException, IOException, InterruptedException {
		Configuration config = new Configuration(Paths.get(getClass().getResource("/testconfig.json").toURI()));
		VoicePi control = new VoicePi(config);
		control.reload();
		QueueRecognizer stt = (QueueRecognizer) control.getSTT();
		QueueEngine tts = (QueueEngine) control.getTTS();

		Thread t = new Thread(control);
		t.start();
		long timeout = 2;

		assertEquals("Starting VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("TEST");
		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("TEST");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Hello world", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		Thread.sleep(5000);
		assertEquals("Too late", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("RELOAD");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Reloading", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		Thread.sleep(1000);
		stt = (QueueRecognizer) control.getSTT();
		tts = (QueueEngine) control.getTTS();

		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("EXIT");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Stopping VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		t.join(1000);
	}

	/** This test will load the same configuration, but use a more advanced ActionModule with different state. */
	@Test
	public void multistateTest() throws URISyntaxException, IOException, InterruptedException {
		Configuration config = new Configuration(Paths.get(getClass().getResource("/testconfig.json").toURI()));
		VoicePi control = new VoicePi(config);
		control.reload();
		QueueRecognizer stt = (QueueRecognizer) control.getSTT();
		QueueEngine tts = (QueueEngine) control.getTTS();

		Thread t = new Thread(control);
		t.start();
		long timeout = 2;

		assertEquals("Starting VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("ADVANCE");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Going to the next level", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("TEST");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Advanced shit, bro!", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("ADVANCE");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Going to the next level", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("TEST2");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Feel the progress?", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("ADVANCE");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Going to the next level", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		Thread.sleep(5000);
		assertEquals("Too late", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("EXIT");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Stopping VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		t.join(1000);
	}
}