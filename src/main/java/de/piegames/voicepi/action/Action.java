package de.piegames.voicepi.action;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public abstract class Action {

	protected final Log log = LogFactory.getLog(getClass());

	public static enum ActionType {
		RUN_COMMAND(RunCommandAction.class), SAY_TEXT(SayTextAction.class), PLAY_SOUND(PlaySoundAction.class), NONE(DoNothingAction.class);

		private Class<? extends Action> clazz;

		private ActionType(Class<? extends Action> clazz) {
			this.clazz = Objects.requireNonNull(clazz);
		}

		public static ActionType forName(String jsonName) {
			String name = jsonName.toUpperCase().replace('-', '_');
			return valueOf(name);
		}

		public Class<? extends Action> getActionClass() {
			return clazz;
		}
	}

	protected final ActionType type;

	public Action(ActionType type, JsonObject data) {
		this.type = type;
	}

	public abstract void execute(VoicePi control) throws IOException, InterruptedException;

	public final void execute(VoicePi control, Log log, String name) {
		try {
			execute(control);
		} catch (IOException | InterruptedException | NullPointerException e) {
			log.warn("Could not execute Action " + name, e);
		}
	}

	public static Action fromJson(JsonObject data) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return ActionType.forName(data.getAsJsonPrimitive("action").getAsString())
				.getActionClass()
				.getConstructor(JsonObject.class)
				.newInstance(data);
	}

	public static final Action DO_NOTHING = new DoNothingAction();

	/** This is meant to be used only by Gson for deserialization methods */
	public static class DoNothingAction extends Action {

		private DoNothingAction() {
			super(ActionType.NONE, null);
		}

		/** @deprecated See {@link Action#DO_NOTHING} instead. */
		@Deprecated
		public DoNothingAction(JsonObject config) {
			super(ActionType.NONE, config);
		}

		@Override
		public void execute(VoicePi control) throws IOException, InterruptedException {
		}
	}
}