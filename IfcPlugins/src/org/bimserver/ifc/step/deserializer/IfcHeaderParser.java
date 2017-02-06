package org.bimserver.ifc.step.deserializer;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.deserializers.DeserializeException;

public class IfcHeaderParser {

	private void filterComments(Tokenizer tokenizer) throws TokenizeException {
		if (tokenizer.startsWith("/*")) {
			tokenizer.zoomIn("/*", "*/");
			tokenizer.readAll();
			tokenizer.zoomOut();
		}
	}

	public IfcHeader parseFileName(String line) throws TokenizeException, DeserializeException, ParseException {
		IfcHeader ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
		parseFileName(line, ifcHeader);
		return ifcHeader;
	}
	
	public IfcHeader parseDescription(String line) throws TokenizeException, DeserializeException, ParseException {
		IfcHeader ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
		parseDescription(line, ifcHeader);
		return ifcHeader;
	}
	
	public void parseDescription(String line, IfcHeader ifcHeader) throws TokenizeException, DeserializeException, ParseException {
		Tokenizer tokenizer = new Tokenizer(line.substring(line.indexOf("(")));
		tokenizer.zoomIn("(", ")");
		tokenizer.zoomIn("(", ")");
		filterComments(tokenizer);
		while (!tokenizer.isEmpty()) {
			ifcHeader.getDescription().add(tokenizer.readSingleQuoted());
			if (tokenizer.nextIsAComma()) {
				tokenizer.readComma();
			}
		}
		tokenizer.zoomOut();
		tokenizer.readComma();
		filterComments(tokenizer);
		ifcHeader.setImplementationLevel(tokenizer.readSingleQuoted());
		tokenizer.zoomOut();
		tokenizer.shouldBeFinished();
	}
	
	public void parseFileName(String line, IfcHeader ifcHeader) throws TokenizeException, DeserializeException, ParseException {
		Tokenizer tokenizer = new Tokenizer(line.substring(line.indexOf("(")));
		tokenizer.zoomIn("(", ")");
		filterComments(tokenizer);
		if (tokenizer.nextIsDollar()) {
			throw new DeserializeException("FILE_NAME.name is not an optional field, but $ used");
		}
		ifcHeader.setFilename(tokenizer.readSingleQuoted());
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
		tokenizer.readComma();
		filterComments(tokenizer);
		ifcHeader.setTimeStamp(dateFormatter.parse(tokenizer.readSingleQuoted()));
		tokenizer.readComma();
		filterComments(tokenizer);
		tokenizer.zoomIn("(", ")");
		while (!tokenizer.isEmpty()) {
			ifcHeader.getAuthor().add(tokenizer.readSingleQuoted());
			if (tokenizer.nextIsAComma()) {
				tokenizer.readComma();
			}
		}
		tokenizer.zoomOut();
		tokenizer.readComma();
		filterComments(tokenizer);
		tokenizer.zoomIn("(", ")");
		while (!tokenizer.isEmpty()) {
			ifcHeader.getOrganization().add(tokenizer.readSingleQuoted());
			if (tokenizer.nextIsAComma()) {
				tokenizer.readComma();
			}
		}
		tokenizer.zoomOut();
		tokenizer.readComma();
		filterComments(tokenizer);
		ifcHeader.setPreProcessorVersion(tokenizer.readSingleQuoted());
		tokenizer.readComma();
		filterComments(tokenizer);
		ifcHeader.setOriginatingSystem(tokenizer.readSingleQuoted());
		tokenizer.readComma();
		filterComments(tokenizer);
		if (tokenizer.nextIsDollar()) {
			tokenizer.readDollar();
		} else {
			ifcHeader.setAuthorization(tokenizer.readSingleQuoted());
		}
		tokenizer.zoomOut();
		tokenizer.shouldBeFinished();		
	}
}