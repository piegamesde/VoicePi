package de.piegames.voicepi.tts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
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
			playSound(ais);
			return true;
		} catch (IOException | LineUnavailableException e) {
			log.warn("Could not speak text: ", e);
			return false;
		}
	}

	public static void playSound(AudioInputStream ais) throws LineUnavailableException, IOException {
		AudioFormat audioFormat = ais.getFormat();
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
		line.open(audioFormat);
		line.start();

		int count = 0;
		byte[] data = new byte[65532];
		while ((count = ais.read(data)) != -1)
			line.write(data, 0, count);

		line.drain();
		line.close();
	}
}