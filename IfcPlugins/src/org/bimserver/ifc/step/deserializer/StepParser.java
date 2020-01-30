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

import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;
import org.bimserver.utils.StringUtils;

public class StepParser {

	private String line;
	private int lastIndex = 0;
	
	public StepParser(String line) {
		this.line = line;
		if (line.startsWith("(") && line.endsWith(")")) {
			this.line = line.substring(1, line.length() - 1);
		}
		lastIndex = StringUtils.nextField(this.line, 0);
	}

	public String readNextString(long lineNumber) throws DeserializeException {
		int nextIndex = StringUtils.nextString(line, lastIndex);
		String val = null;
		try {
			val = line.substring(lastIndex, nextIndex - 1).trim();
		} catch (Exception e) {
			throw new DeserializeException(DeserializerErrorCode.EXPECTED_STRING, lineNumber, "Expected string");
		}
		lastIndex = StringUtils.nextField(this.line, nextIndex);
		
		if (val.equals("$")) {
			return null;
		}
		
		return IfcParserWriterUtils.readString(val, 0);
	}

	private void skipSpaces() {
		while (lastIndex < line.length() - 1 && line.charAt(lastIndex) == ' ') {
			lastIndex++;
		}
	}
	
	public StepParser startList() throws DeserializeException {
		skipSpaces();
		
		int nextIndex = StringUtils.nextString(line, lastIndex);
		String val = line.substring(lastIndex, nextIndex - 1).trim();
		lastIndex = StringUtils.nextField(this.line, nextIndex);
		return new StepParser(val);
	}

	public boolean hasMoreListItems() {
		skipSpaces();
		if (lastIndex >= line.length()) {
			// End reached
			return false;
		}
		String character = line.substring(lastIndex, lastIndex + 1);
		return !character.equals(")");
	}

	public void endList() throws DeserializeException {
		String character = line.substring(lastIndex, lastIndex + 1);
		if (character.equals(")")) {
			lastIndex++;
		} else {
			throw new DeserializeException(DeserializerErrorCode.EXPECTED_RIGHT_PARENTHESIS, "Expected ), got " + character);
		}
	}
}
