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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;

import com.google.common.base.Charsets;

public class X2Pass extends Pass {

	@Override
	public String process(long lineNumber, String result) throws DeserializeException {
		while (result.contains("\\X2\\")) {
			int index = result.indexOf("\\X2\\");
			int indexOfEnd = result.indexOf("\\X0\\", index);
			if (indexOfEnd == -1) {
				throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_X2_NOT_CLOSED_WITH_X0, lineNumber, "\\X2\\ not closed with \\X0\\");
			}
			if ((indexOfEnd - index) % 4 != 0) {
				throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_NUMBER_OF_HEX_CHARS_IN_X2_NOT_DIVISIBLE_BY_4, lineNumber, "Number of hex chars in \\X2\\ definition not divisible by 4");
			}
			try {
				ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(result.substring(index + 4, indexOfEnd).toCharArray()));
				CharBuffer decode = Charsets.UTF_16BE.decode(buffer);
				result = result.substring(0, index) + decode.toString() + result.substring(indexOfEnd + 4);
			} catch (DecoderException e) {
				throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_CHARACTER_DECODING_EXCEPTION, lineNumber, e);
			}
		}
		return result;
	}
}
