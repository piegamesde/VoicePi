package de.piegames.voicepi.tts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.audio.Audio;

/**
 * A speech synthesizing engine will take in text, synthesize audio data saying that text and play it ("text to speech", <i>TTS</i>). A {@code SpeechEngine}
 * will take in text an do something with it with the goal to let the user know that text. It is the primary interface of the application to its user. This will
 * normally involve playing TTS audio data but doesn't have to. Implementations of this class that <i>do</i> generate audio are encouraged to use {@link Audio}
 * for playing that audio instead of a custom (or external) implementation but are not required to do so.
 */
public abstract class SpeechEngine {

	protected Log			log			= LogFactory.getLog(getClass());
	protected JsonObject	config;
	protected VoicePi		control;
	protected List<String>	activate	= new ArrayList<>();

	public SpeechEngine(VoicePi control, JsonObject config) {
		this.config = config;
		this.control = control;
		// TODO move this into MultiSpeechEngine
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

	/**
	 * This method will handle the given text to be spoken and return the respective audio data as an {@link AudioInputStream}. If the implementation does not
	 * generate audio, it should return {@code null} instead. If the implementation does generate audio, but plays it using an external system, it should return
	 * {@code null} too. Note that when playing audio externally this method is expected to wait for it to finish playing.
	 */
	public abstract AudioInputStream generateAudio(String text);

	/**
	 * This method will handle the given text according to the implementation and wait for it to be processed. In the average case, this will result in STT
	 * audio being generated, playing it and waiting for it to finish playing. <br/>
	 * Subclasses should not override this method unless absolutely necessary and try to put all functionality into {@link #generateAudio(String)} instead.
	 */
	public boolean speakAndWait(String text) {
		if (!activate.stream().anyMatch(l -> control.getCurrentState().matches(l)))
			return false;
		log.info("Saying: '" + text + "'");
		AudioInputStream ais = generateAudio(text);
		if (ais == null)
			return false;
		try {
			control.getAudio().play(ais);
			return true;
		} catch (IOException | LineUnavailableException | InterruptedException e) {
			log.warn("Could not speak text: ", e);
			return false;
		}
	}
}