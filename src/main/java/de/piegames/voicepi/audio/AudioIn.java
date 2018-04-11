package de.piegames.voicepi.audio;

import javax.sound.sampled.AudioInputStream;

public abstract class AudioIn {

	public AudioIn() {
	}

	public void startListening(int timeout) {
	}

	public void stopListening() {
	}

	public abstract AudioInputStream getAudio();

}