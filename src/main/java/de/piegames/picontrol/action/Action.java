package de.piegames.picontrol.action;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

public abstract class Action {

	protected final Log log = LogFactory.getLog(getClass());

	public static enum ActionType {
		RUN_COMMAND, SAY_TEXT, PLAY_SOUND, NONE;

		public static ActionType forName(String jsonName) {
			String name = jsonName.toUpperCase().replace('-', '_');
			return valueOf(name);
		}
	}

	protected final ActionType type;

	public Action(ActionType type, JsonObject data) {
		this.type = type;
	}

	public abstract void execute(PiControl control) throws IOException, InterruptedException;

	public final void execute(PiControl control, Log log, String name) {
		try {
			execute(control);
		} catch (IOException | InterruptedException e) {
			log.warn("Could not execute Action " + name, e);
		}
	}

	@Deprecated
	public static Action fromJson(JsonObject json) {
		switch (ActionType.forName(json.getAsJsonPrimitive("action").getAsString())) {
			case RUN_COMMAND:
				return new RunCommandAction(json);
			case SAY_TEXT:
				return new SayTextAction(json);
			case PLAY_SOUND:
				return new PlaySoundAction(json);
			case NONE:
			default:
				return DO_NOTHING;
		}
	}

	public static final Action DO_NOTHING = new DoNothingAction();

	/** This is meant to be used only by Gson for deserialization methods */
	public static class DoNothingAction extends Action {

		private DoNothingAction() {
			super(ActionType.NONE, null);
		}

		@Override
		public void execute(PiControl control) throws IOException, InterruptedException {
		}
	}
}