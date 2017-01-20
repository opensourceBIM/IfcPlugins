package org.bimserver.ifc.step.deserializer;

import org.bimserver.plugins.deserializers.DeserializeException;

public abstract class Pass {

	public abstract String process(int lineNumber, String result) throws DeserializeException;
}
