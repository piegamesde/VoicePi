package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

@SuppressWarnings("deprecation")
public class SphinxRecognizer extends SphinxBaseRecognizer {

	protected LiveSpeechRecognizer stt;

	public SphinxRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(commandsSpoken, commands);
		// Configure stt
		Configuration sphinxConfig = new Configuration();
		sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		sphinxConfig.setDictionaryPath(dicPath.toAbsolutePath().toUri().toURL().toString());
		sphinxConfig.setLanguageModelPath(lmPath.toAbsolutePath().toUri().toURL().toString());

		stt = new LiveSpeechRecognizer(sphinxConfig);
	}

	@Override
	public void run() {
		SpeechResult result;
		while (!Thread.currentThread().isInterrupted()) {
			log.debug("Listening");
			if ((result = stt.getResult()) != null) {
				log.info("You said: " + result.getHypothesis());
				Collection<String> best = result.getNbest(Integer.MAX_VALUE);
				// TODO actually sort them by quality
				if (!deaf && isStateEnabled())
					commandsSpoken.offer(best);
			}
		}
		log.debug("Not listening anymore");
	}

	@Override
	public void startRecognition() {
		log.debug("Starting SphinxRecognizer");
		stt.startRecognition(true);
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void stopRecognition() {
		log.debug("Stopping SphinxRecognizer");
		thread.interrupt();
		Thread.yield();
		// Sorry, no other possibility here. Sphinx does not provide anything to stop it while recognizing.
		thread.stop();
		Thread.yield();
		try {
			stt.stopRecognition();
		} catch (IllegalStateException e) {
			log.error("Could not stop voice recognition", e);
		}
		thread = null;
	}

	@Override
	public void unload() {
		stt = null;
	}
}