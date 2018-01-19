package application;

public class Main {

	/**
	 * The main method from which our application is starting
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		TextToSpeech tts = new TextToSpeech();

		// Setting the Current Voice
		// tts.setVoice("cmu-rms-hsmm");

		tts.speak("You are at: 1 9 2 point 1 6 8 point 1 0 point 1", 0.1f, false, true);
		tts.speak("Today we will learn how to add different languages and voices on Mary T T S!", 0.5f, false, true);
	}
}
