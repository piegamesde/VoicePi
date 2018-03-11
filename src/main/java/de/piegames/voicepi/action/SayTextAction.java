package de.piegames.voicepi.action;

import java.io.IOException;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class SayTextAction extends Action {

	protected String text;

	public SayTextAction(JsonObject data) {
		super(ActionType.SAY_TEXT, data);
		text = data.getAsJsonPrimitive("text").getAsString();
	}

	@Override
	public void execute(VoicePi control) throws IOException, InterruptedException {
		control.getTTS().speakAndWait(text);
	}
}
