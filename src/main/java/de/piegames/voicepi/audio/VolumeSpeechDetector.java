package de.piegames.voicepi.audio;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class VolumeSpeechDetector {

	static enum State {
		QUIET, TOO_SHORT, TIMEOUT, SPEAKING, PAUSED_SPEAKING;
	}

	protected int								minTime, timeoutTime, pauseTime;
	// protected State state;
	private final ReadOnlyObjectWrapper<State>	writableState	= new ReadOnlyObjectWrapper<>();
	public final ReadOnlyObjectProperty<State>	state			= writableState.getReadOnlyProperty();
	protected long								timer;
	protected float								average;
	protected boolean							calibrating;

	public VolumeSpeechDetector(int minTime, int timeoutTime, int pauseTime) {
		this.minTime = minTime;
		this.timeoutTime = timeoutTime;
		this.pauseTime = pauseTime;
		writableState.set(State.QUIET);
		average = 1;
		calibrating = true;
		timer = System.currentTimeMillis();
	}

	public void onSample(float rms) {
		if (calibrating)
			average = average * 0.9f + rms * 0.1f;
		else
			switch (writableState.get()) {
				case QUIET:
					if (rms > average * 2f) {
						System.out.println("Started speaking");
						writableState.set(State.SPEAKING);
						timer = System.currentTimeMillis();
					} else if (System.currentTimeMillis() - timer > timeoutTime) {
						System.out.println("Timeout");
						writableState.set(State.TIMEOUT);
					}
					break;
				case TIMEOUT:
				case TOO_SHORT:
					break;
				case SPEAKING:
					if (rms < average * 0.8f) {
						if (System.currentTimeMillis() - timer < minTime) {
							writableState.set(State.TOO_SHORT);
							System.out.println("Too short command");
						} else {
							writableState.set(State.PAUSED_SPEAKING);
							timer = System.currentTimeMillis();
							System.out.println("Stopped speaking");
						}
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

	public void stopCalibrating() {
		calibrating = false;
	}

	public boolean aborted() {
		return state.get() == State.TIMEOUT || state.get() == State.TOO_SHORT;
	}
}