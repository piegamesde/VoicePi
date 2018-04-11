package de.piegames.voicepi.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public final class AudioHelper {

	private AudioHelper() {
	}

	protected static AudioInputStream convert(AudioFormat srcf, AudioInputStream in) {
		AudioInputStream ais = null;
		if (AudioSystem.isConversionSupported(AudioFormat.Encoding.PCM_SIGNED,
				srcf)) {
			if (srcf.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
				ais = AudioSystem.getAudioInputStream(
						AudioFormat.Encoding.PCM_SIGNED,
						in);
			else
				ais = AudioSystem.getAudioInputStream(new AudioFormat(8000, 8, 1, true, false), in);
		} else
			throw new RuntimeException();

		return ais;
	}
}
