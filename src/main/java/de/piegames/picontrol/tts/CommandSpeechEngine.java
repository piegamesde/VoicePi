package de.piegames.picontrol.tts;

import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.action.RunCommand;

public class CommandSpeechEngine extends SpeechEngine {

	protected RunCommand	command;
	protected boolean		noAudio;

	public CommandSpeechEngine(PiControl control, JsonObject config) {
		super(control, config);
		command = new RunCommand(config.get("command"));
		if (config.has("no-audio"))
			noAudio = config.getAsJsonPrimitive("no-audio").getAsBoolean();
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		try {
			Process process = command.execute();
			if (noAudio) {
				process.waitFor();
				return null;
			} else
				return AudioSystem.getAudioInputStream(process.getInputStream());
		} catch (IOException | UnsupportedAudioFileException | InterruptedException e) {
			log.warn("Could not speak text: " + text, e);
		}
		return null;
	}
}
