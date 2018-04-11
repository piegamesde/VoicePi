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

public abstract class SpeechEngine {

	protected Log			log			= LogFactory.getLog(getClass());
	protected JsonObject	config;
	protected VoicePi		control;
	protected List<String>	activate	= new ArrayList<>();

	public SpeechEngine(VoicePi control, JsonObject config) {
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
	}

	public abstract AudioInputStream generateAudio(String text);

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
		} catch (IOException | LineUnavailableException e) {
			log.warn("Could not speak text: ", e);
			return false;
		}
	}
}