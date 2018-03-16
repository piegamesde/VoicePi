package de.piegames.voicepi.state;

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

	/** Creates a simple graph for testing that contains only one command */
	private MutableValueGraph<ContextState<String>, Set<String>> createModule1(ContextState<String> root) {
		MutableValueGraph<ContextState<String>, Set<String>> graph = ValueGraphBuilder.directed().build();
		ContextState<String> end = new ContextState<String>("module1", "end");
		graph.putEdgeValue(root, end, new HashSet<>(Arrays.asList("TEST")));
		return graph;
	}

	/** Creates a graph for testing with one in-between state and a few useless commands. */
	private MutableValueGraph<ContextState<String>, Set<String>> createModule2(ContextState<String> root) {
		MutableValueGraph<ContextState<String>, Set<String>> graph = ValueGraphBuilder.directed().build();
		ContextState<String> between = new ContextState<String>("module2", "between");
		ContextState<String> end2 = new ContextState<String>("module2", "end");
		graph.putEdgeValue(root, between, new HashSet<>(Arrays.asList("HI", "HELLO")));
		graph.putEdgeValue(between, end2, new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER")));
		return graph;
	}

	/**
	 * This test creates a new VoiceState, adds some modules and speaks some commands and tests if the state machine is reacting correctly. This test does not
	 * test edge cases with malformed module graphs like state and command conflicts.
	 */
	@Test
	public void testGraph() {
		VoiceState<String> uut = new VoiceState<>();
		ContextState<String> root = uut.getRoot();

		uut.addModuleGraph(createModule1(root));

		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAvailableCommands());
		assertEquals(Collections.emptySet(), uut.getAvailableCommands(new ContextState<>("module2", "end")));
		assertNull(uut.commandSpoken("NOT REGISTERED COMMAND"));
		assertEquals(root, uut.getCurrentState());
		assertEquals("module1", uut.commandSpoken("TEST").owner);
		assertEquals(root, uut.getCurrentState());

		MutableValueGraph<ContextState<String>, Set<String>> graph = createModule2(root);
		uut.addModuleGraph(graph);
		ContextState<String> between = new ContextState<>("module2", "between");

		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO", "BYE", "SEE YOU LATER")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals(new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER")), uut.getAvailableCommands(between));
		assertNull(uut.commandSpoken("SEE YOU LATER"));
		assertEquals("module2", uut.commandSpoken("HI").owner);
		assertEquals(between, uut.getCurrentState());
		assertNull(uut.commandSpoken("TEST"));
		assertEquals("module2", uut.commandSpoken("BYE").owner);
		assertEquals(root, uut.getCurrentState());
	}

	@Test
	public void testActivation() {
		VoiceState<String> uut = new VoiceState<>();
		ContextState<String> root = uut.getRoot();
		uut.addModuleGraph(createModule1(root));
		uut.addModuleGraph(createModule2(root));
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands(root));
		uut.setActivationCommands(new HashSet<>(Arrays.asList("kevin", "horst", "idiot")));
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands(root));
		ContextState<String> start = uut.getStart();
		assertEquals("listening", start.name);

		assertEquals(start, uut.getCurrentState());
		assertNull(uut.commandSpoken("TEST"));
		assertEquals(start, uut.getCurrentState());
		assertEquals(new HashSet<>(Arrays.asList("kevin", "horst", "idiot")), uut.getAvailableCommands());
		assertNull(uut.commandSpoken("kevin").owner);
		assertEquals(root, uut.getCurrentState());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals("module2", uut.commandSpoken("HELLO").owner);
		assertEquals(new ContextState<>("module2", "between"), uut.getCurrentState());
		uut.commandSpoken("idiot");
		assertEquals(new ContextState<>("module2", "between"), uut.getCurrentState());
		uut.commandSpoken("BYE");
		assertEquals(start, uut.getCurrentState());

		uut.setActivationCommands(null);
		assertEquals(root, uut.getStart());
	}
}