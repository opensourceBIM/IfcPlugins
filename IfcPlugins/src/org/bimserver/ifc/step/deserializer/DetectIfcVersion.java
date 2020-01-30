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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.bimserver.BimserverDatabaseException;
import org.bimserver.emf.MetaDataException;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;
import org.bimserver.utils.FakeClosingInputStream;

import com.google.common.base.Charsets;

public class DetectIfcVersion {
	private int lineNumber;
	private IfcHeader ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
	
	public static void main(String[] args) {
		Path path = Paths.get("C:\\Bulk\\Single\\beam-standard-case.ifc");
		byte[] head = new byte[4096];
		try {
			IOUtils.readFully(Files.newInputStream(path), head);
			System.out.println(new DetectIfcVersion().detectVersion(head, false));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		}
	}
	
	public String detectVersion(byte[] head, boolean usesZip) throws DeserializeException, IOException {
		if (usesZip) {
			ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(head));
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			if (nextEntry == null) {
				throw new DeserializeException(DeserializerErrorCode.IFCZIP_CONTAINS_NO_IFC_FILES, "Zip files must contain exactly one IFC-file, this zip-file looks empty");
			}
			if (nextEntry.getName().toUpperCase().endsWith(".IFC")) {
				FakeClosingInputStream fakeClosingInputStream = new FakeClosingInputStream(zipInputStream);
				read(fakeClosingInputStream);
			}
		} else {
			read(new ByteArrayInputStream(head));
		}
		if (ifcHeader.getIfcSchemaVersion() == null) {
			throw new DeserializeException(DeserializerErrorCode.NO_IFC_SCHEMA_VERSION_FOUND, "No IFC schema found");
		}
		return ifcHeader.getIfcSchemaVersion();
	}
	
	private long read(InputStream inputStream) throws DeserializeException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
		lineNumber = 0;
		try {
			String line = reader.readLine();
			if (line == null) {
				throw new DeserializeException(DeserializerErrorCode.UNEXPECTED_END_OF_STREAM_WHILE_READING_FIRST_LINE, 0, "Unexpected end of stream reading first line");
			}
			while (line != null) {
				try {
					while (!processLine(line.trim())) {
						String readLine = reader.readLine();
						if (readLine == null) {
							break;
						}
						line += readLine;
						lineNumber++;
					}
				} catch (Exception e) {
					if (e instanceof DeserializeException) {
						throw (DeserializeException)e;
					} else {
						throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, " (" + e.getMessage() + ") " + line, e);
					}
				}

				if (ifcHeader.getIfcSchemaVersion() != null) {
					return lineNumber;
				}
				line = reader.readLine();
				lineNumber++;
			}
			return lineNumber;
		} catch (IOException e) {
			throw new DeserializeException(DeserializerErrorCode.IO_EXCEPTION, lineNumber, e);
		}
	}
	
	public enum Mode {
		HEADER, DATA, FOOTER, DONE
	}
	
	private boolean processLine(String line) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		if (line.length() > 0) {
			if (line.endsWith(";")) {
				processHeader(line);
				return true;
			} else {
				return false;
			}
		}
		if (line.equals("DATA;")) {
			return true;
		}
		return true;
	}
	
	private boolean processHeader(String line) throws DeserializeException {
		if (line.startsWith("/*")) {
			if (line.contains("*/")) {
				line = line.substring(line.indexOf("*/") + 2);
			}
		}
		if (line.startsWith("FILE_SCHEMA")) {
			String fileschema = line.substring("FILE_SCHEMA".length()).trim();
			new IfcHeaderParser().parseFileSchema(fileschema.substring(1, fileschema.length() - 2), ifcHeader, lineNumber);
			return true;
		} else if (line.startsWith("ENDSEC;")) {
			return true;
		}
		return false;
	}
}
