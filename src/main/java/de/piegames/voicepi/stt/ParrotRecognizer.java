package de.piegames.voicepi.stt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.audio.Audio;

public class ParrotRecognizer extends SpeechRecognizer {

	public ParrotRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(commandsSpoken, commands);
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
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		if (!isRunning())
			return;
	}

	@Override
	public boolean transcriptionSupported() {
		return true;
	}

	@Override
	public List<String> transcribe() {
		try {
			System.out.println("Start-------------------------------------------");
			AudioInputStream in = control.getAudio().listenCommand(Audio.FORMAT);
			if (in == null) {
				System.out.println("NOPE!");
				return Collections.emptyList();
			}
			byte[] fileData = Audio.readAllBytes(in);
			control.getAudio().play(
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