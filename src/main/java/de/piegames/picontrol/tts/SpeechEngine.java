package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import application.AudioPlayer;

public abstract class SpeechEngine {

	protected AudioPlayer	tts	= new AudioPlayer();
	protected Log			LOG	= LogFactory.getLog(getClass());

	public SpeechEngine() {
	}

	public abstract AudioInputStream generateAudio(String text);

	public void speak(String text) {
		tts.setAudio(generateAudio(text));
		tts.start();
	}

	public void speakAndWait(String text) {
		speak(text);
		try {
			tts.join();
		} catch (InterruptedException e) {
			LOG.info("Could not wait for the message to finish speaking", e);
		}
	}

	public void stopPlayer() {
		tts.cancel();
	}

	public AudioPlayer getPlayer() {
		return tts;
	}

	protected static void foo() {
	}
}