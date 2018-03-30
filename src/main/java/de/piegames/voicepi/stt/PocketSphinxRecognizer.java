package de.piegames.voicepi.stt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import edu.cmu.sphinx.api.Configuration;

public class PocketSphinxRecognizer extends SphinxBaseRecognizer {

	protected Process			process;
	protected BufferedReader	stdOut;
	protected Writer			stdIn;
	protected Thread			thread2;
	// protected LiveSpeechRecognizer stt;

	public PocketSphinxRecognizer(VoicePi control, JsonObject config) {
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

		log.debug("Dictionary path: " + dicPath.toAbsolutePath());
		log.debug("Language model path: " + lmPath.toAbsolutePath());

		process = new ProcessBuilder("python3", "./scripts/pocketsphinx.py", "--lm", lmPath.toAbsolutePath().toString(), "--dic",
				dicPath.toAbsolutePath().toString()).start();
		stdIn = new OutputStreamWriter(process.getOutputStream()); // Do not buffer here
		stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}

	@Override
	public void run() {
		try {
			for (String str = stdOut.readLine(); str != null; str = stdOut.readLine()) {
				log.debug("PocketSphinx says: " + str);
				if (str.startsWith("Final:"))
					commandSpoken(str.substring(6));
			}
		} catch (IOException e) {
			log.warn("Exception while listening", e);
		}
		log.debug("Not listening anymore");
	}

	@Override
	public void startRecognition() {
		log.debug("Starting SphinxRecognizer");
		thread = new Thread(this);
		thread.start();
		thread2 = new Thread(() -> {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					break;
				}
				synchronized (stdIn) {
					try {
						stdIn.write("Keep alive");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread2.start();
	}

	@Override
	public void stopRecognition() {
		log.debug("Stopping SphinxRecognizer");
		thread2.interrupt();
		thread2 = null;
		synchronized (stdIn) {
			try {
				stdIn.write("Quit");
			} catch (IOException e) {
			}
		}
		process.destroy();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			process.destroyForcibly(); // Just to make sure
		}
		thread.interrupt();
		thread = null;
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		synchronized (stdIn) {
			try {
				stdIn.write(deaf ? "Play" : "Pause");
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void unload() {
		// stt = null;
	}
}