package de.piegames.voicepi.audio;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class VolumeSpeechDetector {

	static enum State {
		QUIET, SPEAKING, PAUSED_SPEAKING;
	}

	protected int								startTime, pauseTime;
	// protected State state;
	private final ReadOnlyObjectWrapper<State>	writableState	= new ReadOnlyObjectWrapper<>();
	public final ReadOnlyObjectProperty<State>	state			= writableState.getReadOnlyProperty();
	protected long								timer;
	protected float								average;
	protected boolean							volumeLock;

	public VolumeSpeechDetector(int startTime, int pauseTime) {
		this.startTime = startTime;
		this.pauseTime = pauseTime;
		writableState.set(State.QUIET);
		average = 1;
	}

	public void onSample(float rms) {
		if (!volumeLock)
			average = average * 0.9f + rms * 0.1f;

		switch (writableState.get()) {
			case QUIET:
				if (rms > average * 2f) {
					System.out.println("Started speaking");
					writableState.set(State.SPEAKING);
					timer = System.currentTimeMillis();
				}
				break;
			case SPEAKING:
				if (rms < average * 0.8f) {
					writableState.set(State.PAUSED_SPEAKING);
					timer = System.currentTimeMillis();
					System.out.println("Stopped speaking");
				}
				break;
			case PAUSED_SPEAKING:
				if (System.currentTimeMillis() - timer > pauseTime) {
					writableState.set(State.QUIET);
					System.out.println("Silence##############################################");
					break;
				}
				if (rms > average * 1.5f) {
					System.out.println("Continued speaking. Silence before: " + timer);
					writableState.set(State.SPEAKING);
					timer = System.currentTimeMillis();
				}
				break;
		}
	}

	public void startSpeaking() {
		writableState.set(State.SPEAKING);
	}

	public boolean isSpeaking() {
		return writableState.get() != State.QUIET;
	}

	public State getState() {
		return writableState.get();
	}

	public void lockVolume() {
		volumeLock = true;
	}
}