package de.piegames.picontrol.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonObject;

public abstract class SpeechRecognizer {

	protected final Log		log	= LogFactory.getLog(getClass());
	protected JsonObject	config;

	public SpeechRecognizer(JsonObject config) {
		// TODO make local config for stt
		this.config = config;
	}

	public abstract void load(Set<String> commands) throws IOException;

	/**
	 * This method finds out the next spoken command. It will block and wait until one is found.
	 *
	 * @throws Exception
	 */
	public abstract Collection<String> nextCommand() throws Exception;

	public void resumeRecognition() {
	}

	public void pauseRecognition() {
	}

	public void stopRecognition() {
		pauseRecognition();
	}
}
