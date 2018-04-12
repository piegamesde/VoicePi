package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public abstract class SpeechRecognizer implements Runnable {

	protected final Log							log			= LogFactory.getLog(getClass());
	protected JsonObject						config;
	protected BlockingQueue<Collection<String>>	commandsSpoken;
	protected Thread							thread;
	protected List<String>						activate	= new ArrayList<>();
	protected VoicePi							control;
	protected volatile boolean					deaf;

	public SpeechRecognizer(VoicePi control, JsonObject config) {
		this.config = config;
		this.control = control;
		// TODO move activate into MultiEngine (without changing the way the user configures it)
		if (config == null || config.isJsonNull() || !config.has("active-on"))
			;
		else if (config.get("active-on").isJsonPrimitive())
			activate.add(config.getAsJsonPrimitive("active-on").getAsString());
		else
			for (JsonElement f : config.getAsJsonArray("active-on"))
				activate.add(f.getAsString());
		if (activate.isEmpty())
			activate.add("*:*");
	}

	public abstract void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException;

	/**
	 * Called in a background thread. This method will continuously listen for any spoken commands and add them to {@code #commandsSpoken}.
	 */
	@Override
	public abstract void run();

	// public abstract Collection<String> nextCommand() throws Exception;

	/** Starts the listening process in a background thread. The process might be started and stopped multiple times. */
	public void startRecognition() {
		log.debug("Starting " + getClass().getSimpleName());
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Stops the listening process. If {@code #run()} does not quit when interrupted, this method should be overwritten to stop the thread in less friendly
	 * ways.
	 */
	public void stopRecognition() {
		log.debug("Stopping " + getClass().getSimpleName());
		thread.interrupt();
		try {
			thread.join(10000);
		} catch (InterruptedException e) {
			log.warn("Could not make sure that the recognizer thread has finished", e);
		}
		thread = null;
	}

	protected boolean isRunning() {
		return thread != null;
	}

	// protected boolean isStateEnabled() {
	// ContextState current = control.getCurrentState();
	// return activate.stream().anyMatch(s -> current.matches(s));
	// }

	protected void commandSpoken(String command) {
		commandSpoken(Arrays.asList(command));
	}

	protected void commandSpoken(Collection<String> command) {
		log.debug("Command spoken [" + String.join(", ", command) + "]");
		// log.debug("Command spoken [" + String.join(", ", command) + "]" + (isStateEnabled() ? "" : " Will be ignored."));
		// if (isStateEnabled())
		commandsSpoken.offer(command);
	}

	/**
	 * This will stop the recognizer from listening. The recognizer will not "hear" anything until {@code #undeafenRecognition()} is called. This is to prevent
	 * recording the output of the speech synthesis as command again. This should have no effect to those recognizers who don't rely on the microphone.
	 */
	public void deafenRecognition(boolean deaf) {
		log.debug((deaf ? "Pausing " : "Resuming ") + getClass().getSimpleName());
		this.deaf = deaf;
	}

	public abstract boolean transcriptionSupported();

	public List<String> transcribe() {
		throw new UnsupportedOperationException();
	}

	/** Unloads and releases all resources. The object won't be used after this method has been called. */
	public void unload() {
		commandsSpoken = null;
	}
}