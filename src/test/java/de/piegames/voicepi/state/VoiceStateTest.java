package de.piegames.voicepi.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public class VoiceStateTest {

	/** Creates a simple graph for testing that contains only one command */
	private MutableValueGraph<ContextState, CommandSet> createModule1(ContextState root) {
		MutableValueGraph<ContextState, CommandSet> graph = ValueGraphBuilder.directed().build();
		ContextState end = new ContextState("module1", "end");
		graph.putEdgeValue(root, end, new CommandSet(null, new HashSet<>(Arrays.asList("TEST"))));
		return graph;
	}

	/** Creates a graph for testing with one in-between state and a few useless commands. */
	private MutableValueGraph<ContextState, CommandSet> createModule2(ContextState root) {
		MutableValueGraph<ContextState, CommandSet> graph = ValueGraphBuilder.directed().build();
		ContextState between = new ContextState("module2", "between");
		ContextState end2 = new ContextState("module2", "end");
		graph.putEdgeValue(root, between, new CommandSet(null, new HashSet<>(Arrays.asList("HI", "HELLO"))));
		graph.putEdgeValue(between, end2, new CommandSet(null, new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER"))));
		return graph;
	}

	/**
	 * This test creates a new VoiceState, adds some modules and speaks some commands and tests if the state machine is reacting correctly. This test does not
	 * test edge cases with malformed module graphs like state and command conflicts.
	 */
	@Test
	public void testGraph() {
		VoiceState uut = new VoiceState();
		ContextState root = uut.getRoot();

		uut.addModuleGraph(createModule1(root));

		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST")), uut.getAvailableCommands());
		assertEquals(Collections.emptySet(), uut.getAvailableCommands(new ContextState("module2", "end")));
		assertNull(uut.commandSpoken("NOT REGISTERED COMMAND"));
		assertEquals(root, uut.getCurrentState());
		uut.commandSpoken("TEST");
		assertEquals("voicepi", uut.getCurrentState().module);
		assertEquals(root, uut.getCurrentState());

		MutableValueGraph<ContextState, CommandSet> graph = createModule2(root);
		uut.addModuleGraph(graph);
		ContextState between = new ContextState("module2", "between");

		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO", "BYE", "SEE YOU LATER")), uut.getAllCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals(new HashSet<>(Arrays.asList("BYE", "SEE YOU LATER")), uut.getAvailableCommands(between));
		assertNull(uut.commandSpoken("SEE YOU LATER"));
		uut.commandSpoken("HI");
		assertEquals("module2", uut.getCurrentState().module);
		assertEquals(between, uut.getCurrentState());
		uut.commandSpoken("TEST");
		assertEquals("module2:between", uut.getCurrentState().toString());
		uut.commandSpoken("BYE");
		assertEquals("voicepi", uut.getCurrentState().module);
		assertEquals(root, uut.getCurrentState());
	}

	@Test
	public void testActivation() {
		VoiceState uut = new VoiceState();
		ContextState root = uut.getRoot();
		uut.addModuleGraph(createModule1(root));
		uut.addModuleGraph(createModule2(root));
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands(root));
		uut.setActivationCommands(new HashSet<>(Arrays.asList("kevin", "horst", "idiot")));
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands(root));
		ContextState start = uut.getStart();
		assertEquals("voicepi:listening", start.toString());

		assertEquals(start, uut.getCurrentState());
		uut.commandSpoken("TEST");
		assertEquals(start, uut.getCurrentState());
		assertEquals(new HashSet<>(Arrays.asList("kevin", "horst", "idiot")), uut.getAvailableCommands());
		uut.commandSpoken("kevin");
		assertEquals("voicepi", uut.getCurrentState().module);
		assertEquals(root, uut.getCurrentState());
		assertEquals(new HashSet<>(Arrays.asList("TEST", "HI", "HELLO")), uut.getAvailableCommands());
		uut.commandSpoken("HELLO");
		assertEquals("module2", uut.getCurrentState().module);
		assertEquals("module2:between", uut.getCurrentState().toString());
		uut.commandSpoken("idiot");
		assertEquals("module2:between", uut.getCurrentState().toString());
		uut.commandSpoken("BYE");
		assertEquals(start, uut.getCurrentState());

		uut.setActivationCommands(null);
		assertEquals(root, uut.getStart());
	}
}