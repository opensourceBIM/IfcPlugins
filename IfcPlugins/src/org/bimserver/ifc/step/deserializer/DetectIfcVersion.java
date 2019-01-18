package org.bimserver.ifc.step.deserializer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
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
				throw new DeserializeException("Zip files must contain exactly one IFC-file, this zip-file looks empty");
			}
			if (nextEntry.getName().toUpperCase().endsWith(".IFC")) {
				FakeClosingInputStream fakeClosingInputStream = new FakeClosingInputStream(zipInputStream);
				read(fakeClosingInputStream);
			}
		} else {
			read(new ByteArrayInputStream(head));
		}
		if (ifcHeader.getIfcSchemaVersion() == null) {
			throw new DeserializeException("No IFC schema found");
		}
		return ifcHeader.getIfcSchemaVersion();
	}
	
	private long read(InputStream inputStream) throws DeserializeException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
		lineNumber = 0;
		try {
			String line = reader.readLine();
			if (line == null) {
				throw new DeserializeException(0, "Unexpected end of stream reading first line");
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
						throw new DeserializeException(lineNumber, " (" + e.getMessage() + ") " + line, e);
					}
				}

				if (ifcHeader.getIfcSchemaVersion() != null) {
					return lineNumber;
				}
				line = reader.readLine();
				lineNumber++;
			}
			return lineNumber;
		} catch (FileNotFoundException e) {
			throw new DeserializeException(lineNumber, e);
		} catch (IOException e) {
			throw new DeserializeException(lineNumber, e);
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
			new IfcHeaderParser().parseFileSchema(fileschema.substring(1, fileschema.length() - 2), ifcHeader);
			return true;
		} else if (line.startsWith("ENDSEC;")) {
			return true;
		}
		return false;
	}
}
