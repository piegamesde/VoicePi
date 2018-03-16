package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonObject;

public abstract class SpeechRecognizer implements Runnable {

	protected final Log								log	= LogFactory.getLog(getClass());
	protected JsonObject							config;
	// TODO make read-only
	public final BlockingQueue<Collection<String>>	commandsSpoken;
	protected Thread								thread;

	public SpeechRecognizer(JsonObject config) {
		this.config = config;
		commandsSpoken = new LinkedBlockingQueue<>();
	}

	public abstract void load(Set<String> commands) throws IOException;

	/**
	 * Called in a background thread. This method will continuously listen for any spoken commands and add them to {@code #commandsSpoken}.
	 */
	@Override
	public abstract void run();

	// public abstract Collection<String> nextCommand() throws Exception;

	/** Starts the listening process in a background thread. The process might be started and stopped multiple times. */
	public void startRecognition() {
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Stops the listening process. If {@code #run()} does not quit when interrupted, this method should be overwritten to stop the thread in less friendly
	 * ways.
	 */
	public void stopRecognition() {
		thread.interrupt();
	}

	/**
	 * This will stop the recognizer from listening. It is not necessary to stop the background thread if other possibilities are available, but it is still
	 * possible.
	 */
	public void pauseRecognition() {
		stopRecognition();
	}

	/** This will resume a paused recognition, returning it back to normal state. */
	public void resumeRecognition() {
		startRecognition();
	}

	/** Unloads and releases all resources. The object won't be used after this method being called. */
	public void unload() {

	}
}