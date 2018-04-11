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
import de.piegames.voicepi.state.ContextState;
import javafx.beans.value.ChangeListener;

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
		if (config == null || config.isJsonNull() || !config.has("active-on"))
			;
		else if (config.get("active-on").isJsonPrimitive())
			activate.add(config.getAsJsonPrimitive("active-on").getAsString());
		else
			for (JsonElement f : config.getAsJsonArray("active-on"))
				activate.add(f.getAsString());
		if (activate.isEmpty())
			activate.add("*:*");
		control.getStateMachine().current.addListener((ChangeListener<ContextState>) (observable, oldValue, newValue) -> onStateChanged(newValue));
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
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Stops the listening process. If {@code #run()} does not quit when interrupted, this method should be overwritten to stop the thread in less friendly
	 * ways.
	 */
	public void stopRecognition() {
		thread.interrupt();
		thread = null;
	}

	protected boolean isRunning() {
		return thread != null;
	}

	public void onStateChanged(ContextState current) {

	}

	protected boolean isStateEnabled() {
		ContextState current = control.getCurrentState();
		return activate.stream().anyMatch(s -> current.matches(s));
	}

	protected void commandSpoken(String command) {
		commandSpoken(Arrays.asList(command));
	}

	protected void commandSpoken(Collection<String> command) {
		log.debug("Command spoken [" + String.join(", ", command) + "]" + (isStateEnabled() ? "" : " Will be ignored."));
		if (isStateEnabled())
			commandsSpoken.offer(command);
	}

	/**
	 * This is called to tell the recognizer that it should start recording and listening for commands actively instead of just waiting passively. This method
	 * will only be called while listening and should return immediately. Listening should stop after a command has been spoken or after {@code timeout}
	 * seconds.<br/>
	 * This does not have to be implemented if the module is always listening for commands anyway.
	 */
	public void activeListening(int timeout) {

	}

	public void passiveListening() {

	}

	/**
	 * This will stop the recognizer from listening. The recognizer will not "hear" anything until {@code #undeafenRecognition()} is called. This is to prevent
	 * recording the output of the speech synthesis as command again. This should have no effect to those recognizers who don't rely on the microphone.
	 */
	public void deafenRecognition(boolean deaf) {
		log.debug((deaf ? "Pausing " : "Resuming ") + getClass().getSimpleName());
		this.deaf = deaf;
	}

	/** Unloads and releases all resources. The object won't be used after this method has been called. */
	public void unload() {
		commandsSpoken = null;
	}
}