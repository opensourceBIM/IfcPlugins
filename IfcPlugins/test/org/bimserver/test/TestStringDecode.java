package org.bimserver.test;

import org.bimserver.ifc.step.deserializer.IfcParserWriterUtils;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.junit.Test;

import junit.framework.Assert;

public class TestStringDecode {
	@Test
	public void testOrder() {
		try {
			Assert.assertEquals("【S】铝合金-浅灰色（窗框）", IfcParserWriterUtils.readString("'\\X2\\3010\\X0\\S\\X2\\301194DD540891D1\\X0\\-\\X2\\6D4570708272FF087A976846FF09\\X0\\'", 0));
		} catch (DeserializeException e) {
			Assert.fail(e.getMessage());
		}
	}
}
