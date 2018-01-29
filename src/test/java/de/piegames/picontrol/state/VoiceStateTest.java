package de.piegames.picontrol.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public class VoiceStateTest {

	/**
	 * This test creates a new VoiceState, adds some modules and speaks some commands and tests if the state machine is reacting correctly. This test does not
	 * test edge cases with malformed module graphs like state and command conflicts.
	 */
	@Test
	public void testGraph() {
		VoiceState<String> uut = new VoiceState<>();
		ContextState<String> root = uut.getRoot();
		MutableValueGraph<ContextState<String>, Set<String>> graph = ValueGraphBuilder.directed().build();
		ContextState<String> end = new ContextState<String>("module1", "end");
		graph.putEdgeValue(root, end, new HashSet<>(Arrays.asList("TEST")));

		uut.addModuleGraph(graph);

		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAvailableCommands());
		assertEquals(Collections.emptySet(), uut.getAvailableCommands(new ContextState<>("module2", "end")));
		assertNull(uut.commandSpoken("NOT REGISTERED COMMAND"));
		assertEquals("module1", uut.commandSpoken("TEST"));
		assertEquals(root, uut.getCurrentState());

		graph = ValueGraphBuilder.directed().build();
		ContextState<String> between = new ContextState<String>("module2", "between");
		ContextState<String> end2 = new ContextState<String>("module2", "end");
		graph.putEdgeValue(root, between, new HashSet<>(Arrays.asList("HI", "HELLO")));
		graph.putEdgeValue(between, end2, new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER")));

		uut.addModuleGraph(graph);

		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO", "BYE", "SEE YOU LATER")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals(new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER")), uut.getAvailableCommands(between));
		assertNull(uut.commandSpoken("SEE YOU LATER"));
		assertEquals("module2", uut.commandSpoken("HI"));
		assertEquals(between, uut.getCurrentState());
		assertNull(uut.commandSpoken("TEST"));
		assertEquals("module2", uut.commandSpoken("BYE"));
		assertEquals(root, uut.getCurrentState());
	}
}