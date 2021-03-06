package de.piegames.voicepi.action;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.tts.SpeechEngine;

public class PlaySoundAction extends Action {

	protected String soundfile;

	public PlaySoundAction(JsonObject data) {
		super(ActionType.PLAY_SOUND, data);
		soundfile = data.getAsJsonPrimitive("soundfile").getAsString();
	}

	@Override
	public void execute(VoicePi control) throws IOException, InterruptedException {
		try {
			log.debug("Playing " + soundfile);
			SpeechEngine.playSound(AudioSystem.getAudioInputStream(new File(soundfile)));
		} catch (LineUnavailableException | UnsupportedAudioFileException e) {
			log.warn("Could not play sound " + soundfile, e);
		}
	}
}