package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;

public class SphinxRecognizer extends SphinxBaseRecognizer {

	protected SphinxSpeechRecognizer	stt;
	protected AudioFormat				format;

	public SphinxRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
		float sampleRate = config.has("sample-rate") ? config.getAsJsonPrimitive("sample-rate").getAsFloat() : 16000;
		int sampleSize = config.has("sample-size") ? config.getAsJsonPrimitive("sample-size").getAsInt() : 16;
		int channels = config.has("channels") ? config.getAsJsonPrimitive("channels").getAsInt() : 1;
		boolean signed = config.has("signed") ? config.getAsJsonArray("signed").getAsBoolean() : true;
		boolean bigEndian = config.has("big-endian") ? config.getAsJsonArray("big-endian").getAsBoolean() : false;
		format = new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(commandsSpoken, commands);
		// Configure stt
		Configuration sphinxConfig = new Configuration();
		sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		sphinxConfig.setDictionaryPath(dicPath.toAbsolutePath().toUri().toURL().toString());
		sphinxConfig.setLanguageModelPath(lmPath.toAbsolutePath().toUri().toURL().toString());

		stt = new SphinxSpeechRecognizer(sphinxConfig, control.getAudio());
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
				if (!deaf)
					commandsSpoken.offer(best);
			}
		}
		log.debug("Not listening anymore");
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		stt.setDeaf(deaf);
	}

	@Override
	public void startRecognition() {
		log.debug("Starting SphinxRecognizer");
		try {
			stt.startRecognition(true);
		} catch (LineUnavailableException | IOException e) {
			e.printStackTrace();
		}
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void stopRecognition() {
		log.debug("Stopping SphinxRecognizer");
		thread.interrupt();
		Thread.yield();
		try {
			stt.stopRecognition();
		} catch (IllegalStateException | IOException e) {
			log.error("Could not stop voice recognition", e);
		}
		try {
			thread.join(10000);
		} catch (InterruptedException e) {
			log.warn("Could not make sure that the recognizer thread has finished", e);
		}
		thread = null;
	}

	@Override
	public void unload() {
		stt = null;
	}

	@Override
	public boolean transcriptionSupported() {
		return false;// TODO
	}
}