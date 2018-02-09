package de.piegames.picontrol.action;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.tts.SpeechEngine;

public class PlaySoundAction extends Action {

	protected String soundfile;

	public PlaySoundAction(JsonObject data, PiControl control) {
		super(ActionType.PLAY_SOUND, data, control);
		soundfile = data.getAsJsonPrimitive("soundfile").getAsString();
	}

	@Override
	public void execute() throws IOException, InterruptedException {
		try {
			SpeechEngine.playSound(AudioSystem.getAudioInputStream(new File(soundfile)));
		} catch (LineUnavailableException | UnsupportedAudioFileException e) {
			log.warn("Could not play sound " + soundfile, e);
		}
	}
}