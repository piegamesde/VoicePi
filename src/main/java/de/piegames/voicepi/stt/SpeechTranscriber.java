package de.piegames.voicepi.stt;

import java.util.List;

public interface SpeechTranscriber {

	public List<String> transcribe(int timeout);
}
