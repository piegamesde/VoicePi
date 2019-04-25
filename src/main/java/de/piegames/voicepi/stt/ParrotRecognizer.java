package de.piegames.voicepi.stt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import com.google.gson.JsonObject;
import de.piegames.voicepi.audio.Audio;

public class ParrotRecognizer extends SpeechRecognizer {

	public ParrotRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			log.debug("Listening");
			transcribe();
		}
		log.debug("Not listening anymore");
	}

	@Override
	public boolean transcriptionSupported() {
		return true;
	}

	@Override
	public List<String> transcribe() {
		try {
			System.out.println("Start-------------------------------------------");
			AudioInputStream in = audio.listenCommand(Audio.FORMAT);
			if (in == null) {
				System.out.println("NOPE!");
				return Collections.emptyList();
			}
			byte[] fileData = Audio.readAllBytes(in);
			if (!deaf)
				audio.play(
						new AudioInputStream(new ByteArrayInputStream(fileData), Audio.FORMAT, AudioSystem.NOT_SPECIFIED));
		} catch (IOException e) {
			log.warn(e);
		}
		return Collections.emptyList();
	}

	@Override
	public void unload() {
	}
}