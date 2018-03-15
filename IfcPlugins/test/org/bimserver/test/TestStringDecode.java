package org.bimserver.test;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import org.bimserver.ifc.step.deserializer.IfcParserWriterUtils;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.junit.Test;

import junit.framework.Assert;

public class TestStringDecode {
	@Test
	public void testOrder() {
		try {
			Assert.assertEquals("【S】铝合金-浅灰色（窗框）", IfcParserWriterUtils.readString("'\\X2\\3010\\X0\\S\\X2\\301194DD540891D1\\X0\\-\\X2\\6D4570708272FF087A976846FF09\\X0\\'", 0));
                        // Nordic character in UTF-16:
                        Assert.assertEquals("Vaffelrøre", IfcParserWriterUtils.readString("'Vaffelr\\X2\\00F8\\X0\\re'", 0));
                        // Nordic character in UTF-32:
                        Assert.assertEquals("Vaffelrøre", IfcParserWriterUtils.readString("'Vaffelr\\X4\\000000F8\\X0\\re'", 0));
                        
            Assert.assertEquals("Åäèíèöà", IfcParserWriterUtils.readString("'\\S\\E\\S\\d\\S\\h\\S\\m\\S\\h\\S\\v\\S\\`'", 0));
            Assert.assertEquals("Øòóê", IfcParserWriterUtils.readString("'\\S\\X\\S\\r\\S\\s\\S\\j'", 0));
		} catch (DeserializeException e) {
			Assert.fail(e.getMessage());
		}
	}
}
