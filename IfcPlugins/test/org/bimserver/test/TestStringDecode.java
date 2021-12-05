package org.bimserver.test;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
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
            Assert.assertEquals("转角", IfcParserWriterUtils.readString("'\\X2\\8F6C89D2\\X0\\'", 0));
            Assert.assertEquals("/*", IfcParserWriterUtils.readString("'/*'", 0));
            Assert.assertEquals("Fläche", IfcParserWriterUtils.readString("'Fl\\X2\\00E4\\X0\\che'", 0));
            Assert.assertEquals("Länge", IfcParserWriterUtils.readString("'L\\X2\\00E4\\X0\\nge'", 0));
            
            // This string is taken from (and edited) ifc4_add2.ifc, pretty sure the original is invalid though
            Assert.assertEquals("相对于纵轴的旋转角。对全局坐标系中的垂直柱，该属性为相对于轴的角度。（若轮廓方向在轴上，则转角为0。）对全局坐标系中的非垂直柱，该属性为相对于Z轴的角度。（若轮廓方向在Z轴上，则转角为0。）\n" + 
            		"该属性所提供的形状信息是对内部形状描述和几何参数的补充。如果几何参数与该属性所提供的形状属性不符，应以几何参数为准。对CAD等几何编辑程序，该属性应为只写类型。A注：IFC2x4新添属性", IfcParserWriterUtils.readString("'\\X2\\76F85BF94E8E7EB58F74768465CB8F6C89D230025BF951685C40575068077CFB4E2D7684578276F467F1FF0C8BE55C5E60274E3A76F85BF94E8E\\X0\\\\X2\\8F74768489D25EA63002FF0882E58F6E5ED365B954115728\\X0\\\\X2\\8F744E0AFF0C52198F6C89D24E3A\\X0\\0\\X2\\3002FF095BF951685C40575068077CFB4E2D7684975E578276F467F1FF0C8BE55C5E60274E3A76F85BF94E8E\\X0\\Z\\X2\\8F74768489D25EA63002FF0882E58F6E5ED365B954115728\\X0\\Z\\X2\\8F744E0AFF0C52198F6C89D24E3A\\X0\\0\\X2\\3002FF09\\X0\\\\X\\0A\\X2\\8BE55C5E6027624063D04F9B76845F6272B64FE1606F662F5BF9518590E85F6272B663CF8FF0548C51E04F5553C2657076848865514530025982679C51E04F5553C265704E0E8BE55C5E6027624063D04F9B76845F6272B65C5E60274E0D7B26FF0C5E944EE551E04F5553C265704E3A51C630025BF9\\X0\\CAD\\X2\\7B4951E04F557F168F917A0B5E8FFF0C8BE55C5E60275E944E3A53EA51997C7B578B3002\\X0\\A\\X2\\6CE8FF1A\\X0\\IFC2x4\\X2\\65B06DFB5C5E6027\\X0\\'", 0));
            Assert.assertEquals("REALIZAČNÝ PROJEKT", IfcParserWriterUtils.readString("'REALIZA\\X2\\010C\\X0\\N\\X\\DD PROJEKT'", 0));  // this is correct
            Assert.assertEquals("REALIZAČNÝ PROJEKT", IfcParserWriterUtils.readString("'\\PB\\REALIZA\\S\\HN\\S\\] PROJEKT'", 0)); // This is ISO 8859-2
            // Assert.assertEquals("REALIZAČNÝ PROJEKT", IfcParserWriterUtils.readString("'REALIZA\\X\\C8N\\X\\DD PROJEKT'", 0)); // This is ISO 8859-2, but \\X\\ directive does not work this way
            Assert.assertEquals("ÄÜÖ", IfcParserWriterUtils.readString("'\\S\\D\\S\\\\\\S\\V'",0));
            Assert.assertEquals("0.00 \\X\\B0C", IfcParserWriterUtils.readString("'0.00 \\\\X\\\\B0C'", 0));
            String lorem = "'Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'";
            Assert.assertEquals( lorem.substring(1, lorem.length()-1) , IfcParserWriterUtils.readString(lorem, 0));
		} catch (DeserializeException e) {
			Assert.fail(e.getMessage());
		}
    }

    @Test(expected = DeserializeException.class)
    public void testWrongDanglingBackslash () throws DeserializeException {
        IfcParserWriterUtils.readString("'one \\ two'", 0);
    }

    @Test(expected = DeserializeException.class)
    public void testDirectiveXMissingBackslash () throws DeserializeException {
        IfcParserWriterUtils.readString("'one \\Xtwo'", 0);
    }

    @Test(expected = DeserializeException.class)
    public void testDirectiveXMissingEnd () throws DeserializeException {
        IfcParserWriterUtils.readString("'L\\X2\\00E4nge'", 0);
    }

    @Test
    public void testPerformance() throws DeserializeException {
        for (int i=0; i<10000; i++){
            testOrder();
        }
    }
}
