/* Copyright 1999-2004 Carnegie Mellon University. Portions Copyright 2004 Sun Microsystems, Inc. Portions Copyright 2004 Mitsubishi Electric Research
 * Laboratories. All Rights Reserved. Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL WARRANTIES. */

package de.piegames.voicepi.stt;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.LineUnavailableException;
import de.piegames.voicepi.audio.Audio;

/**
 * InputStream adapter
 */
public class Microphone2 {

	// private final TargetDataLine line;
	private final InputStream inputStream;

	public Microphone2(Audio in) {
		try {
			// line = AudioSystem.getTargetDataLine(format);
			// line.open(format);
			inputStream = in.normalListening();
		} catch (LineUnavailableException e) {
			throw new IllegalStateException(e);
		}
		// // inputStream = AudioSystem.getAudioInputStream(new AudioFormat(16000, 16, 1, true, false), new AudioInputStream(new InterruptibleInputStream(new
		// // AudioInputStream(line)), format, format.getFrameSize()));
		// inputStream = AudioSystem.getAudioInputStream(new AudioFormat(16000, 16, 1, true, false), new AudioInputStream(line));
	}

	public void startRecording() {
		// line.start();
	}

	public void stopRecording() {
		// line.stop();
	}

	public void close() {
		try {
			inputStream.close();
			// line.close();
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	public InputStream getStream() {
		return inputStream;
	}
}
