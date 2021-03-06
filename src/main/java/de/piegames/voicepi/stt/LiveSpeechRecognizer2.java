/* Copyright 2013 Carnegie Mellon University. Portions Copyright 2004 Sun Microsystems, Inc. Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved. Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL WARRANTIES. */

package de.piegames.voicepi.stt;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

/**
 * High-level class for live speech recognition.
 */
public class LiveSpeechRecognizer2 extends AbstractSpeechRecognizer {

	private final Microphone2 microphone;

	/**
	 * Constructs new live recognition object.
	 *
	 * @param configuration common configuration
	 * @throws IOException if model IO went wrong
	 */
	public LiveSpeechRecognizer2(Configuration configuration, AudioFormat format) throws IOException {
		super(configuration);
		// microphone = speechSourceProvider.getMicrophone();
		microphone = new Microphone2(format);
		context.getInstance(StreamDataSource.class)
				.setInputStream(microphone.getStream());
	}

	/**
	 * Starts recognition process.
	 *
	 * @param clear clear cached microphone data
	 * @see LiveSpeechRecognizer2#stopRecognition()
	 */
	public void startRecognition(boolean clear) {
		recognizer.allocate();
		microphone.startRecording();
	}

	/**
	 * Stops recognition process.
	 *
	 * Recognition process is paused until the next call to startRecognition.
	 *
	 * @see LiveSpeechRecognizer2#startRecognition(boolean)
	 */
	public void stopRecognition() {
		microphone.stopRecording();
		microphone.close();
		recognizer.deallocate();
	}
}
