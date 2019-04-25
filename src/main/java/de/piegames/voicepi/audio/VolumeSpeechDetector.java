package de.piegames.voicepi.audio;

import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class VolumeSpeechDetector implements Consumer<Float> {

	static enum State {
		QUIET, TOO_SHORT, TIMEOUT, SPEAKING, STARTED_SPEAKING, PAUSED_SPEAKING;
	}

	protected int								minTime, timeoutTime, pauseTime, maxCommandTime;
	// protected State state;
	private final ReadOnlyObjectWrapper<State>	writableState	= new ReadOnlyObjectWrapper<>();
	public final ReadOnlyObjectProperty<State>	state			= writableState.getReadOnlyProperty();
	protected long								timer;
	protected float								average;

	public VolumeSpeechDetector(float average, int minTime, int timeoutTime, int pauseTime, int maxCommandTime) {
		this.minTime = minTime;
		this.timeoutTime = timeoutTime;
		this.pauseTime = pauseTime;
		this.maxCommandTime = maxCommandTime;
		writableState.set(State.QUIET);
		this.average = average;
		timer = System.currentTimeMillis();
	}

	public void onSample(float rms) {
		long currentTime = System.currentTimeMillis();
		switch (writableState.get()) {
			case QUIET:
				if (rms > average * 2) {
					System.out.println("Speaking");
					writableState.set(State.STARTED_SPEAKING);
					timer = currentTime;
				} else if (currentTime - timer > timeoutTime) {
					System.out.println("Timeout");
					writableState.set(State.TIMEOUT);
				}
				break;
			case TIMEOUT:
			case TOO_SHORT:
				break;
			case STARTED_SPEAKING:
				if (currentTime - timer < minTime) {
					if (rms < average * 0.8f) {
						writableState.set(State.TOO_SHORT);
						System.out.println("Command too short: " + ((currentTime - timer) / 1000f) + ", expecting " + minTime / 1000f);
					}
				} else {
					System.out.println("Now we're talking");
					writableState.set(State.SPEAKING);
				}
				break;
			case SPEAKING:
				if (rms < average * 0.8f) {
					writableState.set(State.PAUSED_SPEAKING);
					timer = currentTime;
					System.out.println("Stopped speaking");
				}
				if (currentTime - timer > maxCommandTime) {
					writableState.set(State.QUIET);
					System.out.println("Max command length reached");
				}
				break;
			case PAUSED_SPEAKING:
				if (currentTime - timer > pauseTime) {
					writableState.set(State.QUIET);
					System.out.println("Silence##############################################");
					break;
				}
				if (rms > average * 1.5f) {
					System.out.println("Continued speaking. Silence before: " + ((currentTime - timer) / 1000f) + "s");
					writableState.set(State.SPEAKING);
					timer = currentTime;
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

	public boolean aborted() {
		return state.get() == State.TIMEOUT || state.get() == State.TOO_SHORT;
	}

	@Override
	public void accept(Float t) {
		onSample(t);
	}
}