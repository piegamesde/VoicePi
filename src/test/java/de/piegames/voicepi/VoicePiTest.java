package de.piegames.voicepi;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import de.piegames.voicepi.stt.QueueRecognizer;
import de.piegames.voicepi.tts.QueueEngine;

public class VoicePiTest {

	protected VoicePi			control;
	protected QueueRecognizer	stt;
	protected QueueEngine		tts;
	protected Thread			t;
	protected final long		timeout	= 2;

	@Before
	public void setup() throws URISyntaxException {
		Configuration config = new Configuration(Paths.get(getClass().getResource("/testconfig.json").toURI()));
		control = new VoicePi(config);
		control.reload();
		stt = (QueueRecognizer) control.getSTT();
		tts = (QueueEngine) control.getTTS();

		t = new Thread(control);
		t.start();
	}

	/**
	 * This test will load a simple configuration and say the activation command and the test command in different context states and will test if the module
	 * triggers all actions correctly.
	 */
	@Test
	public void testTest() throws IOException, InterruptedException {
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
	}

	/** This test will load the same configuration, but use a more advanced ActionModule with different state. */
	@Test
	public void multistateTest() throws IOException, InterruptedException {
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
	}

	@After
	public void finish() throws InterruptedException {
		stt.commandSpoken("ACTIVATE");
		assertEquals("Yes, sir", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		stt.commandSpoken("EXIT");
		assertEquals("OK", tts.spoken.poll(timeout, TimeUnit.SECONDS));
		assertEquals("Stopping VoicePi", tts.spoken.poll(timeout, TimeUnit.SECONDS));

		t.join();
	}
}