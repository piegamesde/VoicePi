package de.piegames.voicepi.audio;

public class VolumeSpeechDetector {

	static enum State {
		QUIET, STARTING_SPEAKING, SPEAKING, PAUSED_SPEAKING;
	}

	protected int	startTime, pauseTime;
	protected State	state;
	protected long	timer;
	protected float	average;

	public VolumeSpeechDetector(int startTime, int pauseTime) {
		this.startTime = startTime;
		this.pauseTime = pauseTime;
		state = State.QUIET;
		average = 1;
	}

	public void onSample(float rms) {
		if (state == State.QUIET)
			average = average * 0.95f + rms * 0.05f;
		else
			average = average * 0.999f + rms * 0.001f;

		switch (state) {
			case QUIET:
				if (rms > average * 2f) {
					System.out.println("Started speaking");
					state = State.STARTING_SPEAKING;
					timer = System.currentTimeMillis();
				}
				break;
			case STARTING_SPEAKING:
				if (rms > average) {
					if (System.currentTimeMillis() - timer > startTime) {
						state = State.SPEAKING;
						System.out.println("Speaking");
					}
				} else {
					System.out.println("Abort command");
					state = State.QUIET;
				}
				break;
			case SPEAKING:
				if (rms < average * 0.8f) {
					state = State.PAUSED_SPEAKING;
					timer = System.currentTimeMillis();
					System.out.println("Stopped speaking");
				}
				break;
			case PAUSED_SPEAKING:
				if (rms < average) {
					// System.out.println("Not speaking for " + timer + " frames.");
					// timer++;
				}
				if (System.currentTimeMillis() - timer > pauseTime) {
					state = State.QUIET;
					System.out.println("Silence##############################################");
					break;
				}
				if (rms > average * 1.5f) {
					System.out.println("Continued speaking. Silence before: " + timer);
					state = State.SPEAKING;
					timer = System.currentTimeMillis();
				}
				break;
		}
	}

	public void startSpeaking() {
		state = State.SPEAKING;
	}

	public boolean isSpeaking() {
		return state != State.QUIET;
	}
}