package de.piegames.voicepi.tts;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class MultiSpeechEngine extends SpeechEngine {

	protected List<SpeechEngine>	outputs	= new ArrayList<>();
	protected boolean				onlyOne;

	public MultiSpeechEngine(VoicePi control, JsonObject config) {
		super(control, config);
		onlyOne = config.getAsJsonPrimitive("only-one-active").getAsBoolean();
		for (JsonElement e : config.getAsJsonArray("engines")) {
			JsonObject ttsConfig = e.getAsJsonObject();
			try {
				outputs.add((SpeechEngine) Class.forName(ttsConfig.getAsJsonPrimitive("class-name").getAsString())
						.getConstructor(VoicePi.class, JsonObject.class)
						.newInstance(control, ttsConfig));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
				log.warn("Could not instantiate speech engine " + ttsConfig.getAsJsonPrimitive("class-name").getAsString(), e1);
			}
		}
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		if (onlyOne)
			outputs.stream().anyMatch(o -> o.speakAndWait(text));
		else
			outputs.stream().forEach(o -> o.speakAndWait(text));
		return null;
	}
}
