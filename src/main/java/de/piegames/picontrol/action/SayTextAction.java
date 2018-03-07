package de.piegames.picontrol.action;

import java.io.IOException;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

public class SayTextAction extends Action {

	protected String text;

	public SayTextAction(JsonObject data) {
		super(ActionType.SAY_TEXT, data);
		text = data.getAsJsonPrimitive("text").getAsString();
	}

	@Override
	public void execute(PiControl control) throws IOException, InterruptedException {
		control.getTTS().speakAndWait(text);
	}
}
