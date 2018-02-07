package de.piegames.picontrol.tts;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.piegames.picontrol.PiControl;

public abstract class SpeechEngine {

	protected Log		log	= LogFactory.getLog(getClass());
	protected PiControl	control;

	public SpeechEngine(PiControl control) {
		this.control = control;
	}

	public abstract AudioInputStream generateAudio(String text);

	public void speakAndWait(String text) {
		log.info("Saying: '" + text + "'");
		AudioInputStream ais = generateAudio(text);
		if (ais == null)
			return;
		try {
			playSound(ais);
		} catch (IOException | LineUnavailableException e) {
			log.warn("Could not speak text: ", e);
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