package org.bimserver.ifc.step.deserializer;

import java.util.ArrayList;
import java.util.List;

import org.bimserver.plugins.deserializers.DeserializeException;

public class ParserPlan {
	private final List<Pass> passes = new ArrayList<>();
	
	public ParserPlan(Pass... passes) {
		for (Pass pass : passes) {
			this.passes.add(pass);
		}
	}
	
	public void add(Pass pass) {
		passes.add(pass);
	}
	
	public String process(int lineNumber, String input) throws DeserializeException {
		for (Pass pass : passes) {
			input = pass.process(lineNumber, input);
		}
		return input;
	}
}
