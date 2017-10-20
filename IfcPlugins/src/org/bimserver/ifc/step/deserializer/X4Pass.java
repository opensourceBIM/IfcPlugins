package org.bimserver.ifc.step.deserializer;

/******************************************************************************
 * Copyright (C) 2009-2017  BIMserver.org
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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bimserver.plugins.deserializers.DeserializeException;

public class X4Pass extends Pass {
	public String process(int lineNumber, String result) throws DeserializeException {
		while (result.contains("\\X4\\")) {
			int index = result.indexOf("\\X4\\");
			int indexOfEnd = result.indexOf("\\X0\\", index);
			if (indexOfEnd == -1) {
				throw new DeserializeException(lineNumber, "\\X4\\ not closed with \\X0\\");
			}
			if ((indexOfEnd - (index + 4)) % 8 != 0) {
				throw new DeserializeException(lineNumber, "Number of hex chars in \\X4\\ definition not divisible by 8");
			}
			try {
				ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(result.substring(index + 4, indexOfEnd).toCharArray()));
				CharBuffer decode = Charset.forName("UTF-32").decode(buffer);
				result = result.substring(0, index) + decode.toString() + result.substring(indexOfEnd + 4);
			} catch (DecoderException e) {
				throw new DeserializeException(lineNumber, e);
			} catch (UnsupportedCharsetException e) {
				throw new DeserializeException(lineNumber, "UTF-32 is not supported on your system", e);
			}
		}
		return result;
	}
}
