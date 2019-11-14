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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;

public class IfcHeaderParser {

	public IfcHeader parseFileName(String line) throws DeserializeException, ParseException {
		IfcHeader ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
		parseFileName(line, ifcHeader);
		return ifcHeader;
	}
	
	public IfcHeader parseDescription(String line) throws DeserializeException, ParseException {
		IfcHeader ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
		parseDescription(line, ifcHeader);
		return ifcHeader;
	}
	
	public void parseDescription(String line, IfcHeader ifcHeader) throws DeserializeException {
		line = line.replace("\r\n", "");

		StepParser stepParser = new StepParser(line);
		StepParser startList = stepParser.startList();
		while (startList.hasMoreListItems()) {
			ifcHeader.getDescription().add(startList.readNextString());
		}
		ifcHeader.setImplementationLevel(stepParser.readNextString());
	}
	
	public void parseFileName(String line, IfcHeader ifcHeader) throws DeserializeException {
		line = line.replace("\r\n", "");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");

		StepParser stepParser = new StepParser(line);
		ifcHeader.setFilename(stepParser.readNextString());
		try {
			ifcHeader.setTimeStamp(dateFormatter.parse(stepParser.readNextString()));
		} catch (ParseException e) {
			throw new DeserializeException(DeserializerErrorCode.INVALID_DATETIME_LITERAL, "Datetime parse error", e);
		}
		StepParser startList = stepParser.startList();
		while (startList.hasMoreListItems()) {
			ifcHeader.getAuthor().add(startList.readNextString());
		}
		startList = stepParser.startList();
		while (startList.hasMoreListItems()) {
			ifcHeader.getOrganization().add(startList.readNextString());
		}
		ifcHeader.setPreProcessorVersion(stepParser.readNextString());
		ifcHeader.setOriginatingSystem(stepParser.readNextString());
		ifcHeader.setAuthorization(stepParser.readNextString());
	}

	public void parseFileSchema(String line, IfcHeader ifcHeader) throws DeserializeException {
		line = line.replace("\r\n", "");
		StepParser stepParser = new StepParser(line);
		ifcHeader.setIfcSchemaVersion(stepParser.readNextString());
	}
}