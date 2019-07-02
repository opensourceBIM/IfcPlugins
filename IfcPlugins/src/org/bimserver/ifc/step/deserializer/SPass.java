package org.bimserver.ifc.step.deserializer;

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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import com.google.common.base.Charsets;

public class SPass extends Pass {
	public String process(long lineNumber, String result) {
		while (result.contains("\\S\\")) {
			int index = result.indexOf("\\S\\");
			char x = result.charAt(index + 3);
			ByteBuffer b = ByteBuffer.wrap(new byte[] { (byte) (x + 128) });
			CharBuffer decode = Charsets.ISO_8859_1.decode(b);
			result = result.substring(0, index) + decode.get() + result.substring(index + 4);
		}
		return result;
	}
}
