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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IdEObjectImpl;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.MetaDataException;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.BasicIfcModel;
import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.ObjectAlreadyExistsException;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;
import org.bimserver.plugins.deserializers.EmfDeserializer;
import org.bimserver.shared.ListWaitingObject;
import org.bimserver.shared.SingleWaitingObject;
import org.bimserver.shared.WaitingList;
import org.bimserver.utils.FakeClosingInputStream;
import org.bimserver.utils.StringUtils;
import org.eclipse.emf.common.util.AbstractEList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EEnumImpl;

import com.google.common.base.Charsets;

import nl.tue.buildingsmart.schema.Attribute;
import nl.tue.buildingsmart.schema.EntityDefinition;
import nl.tue.buildingsmart.schema.ExplicitAttribute;

/**
 * @author Ruben de Laat
 *	Replaced by the streaming deserializers
 */
@Deprecated
public abstract class IfcStepDeserializer extends EmfDeserializer {
	/*
	 * The following hacks are present
	 * 
	 * - For every feature of type double there is an extra feature (name
	 * appended with "AsString") of type String to keep the original String
	 * version this is also done for aggregate features - WrappedValues for all
	 * for derived primitive types and enums that are used in a "select"
	 */

	private static final int AVERAGE_LINE_LENGTH = 58;
	private static final String WRAPPED_VALUE = "wrappedValue";
	private final WaitingList<Long> waitingList = new WaitingList<>();
	private Mode mode = Mode.HEADER;
	private IfcModelInterface model;
	private int lineNumber;
	private Schema schema;

	public enum Mode {
		HEADER, DATA, FOOTER, DONE
	}

	public IfcStepDeserializer(Schema schema) {
		this.schema = schema;
	}

	public IfcModelInterface read(InputStream in, String filename, long fileSize, ByteProgressReporter byteProgressReporter) throws DeserializeException {
		mode = Mode.HEADER;
		if (filename != null && (filename.toUpperCase().endsWith(".ZIP") || filename.toUpperCase().endsWith(".IFCZIP"))) {
			ZipInputStream zipInputStream = new ZipInputStream(in);
			ZipEntry nextEntry;
			try {
				nextEntry = zipInputStream.getNextEntry();
				if (nextEntry == null) {
					throw new DeserializeException(DeserializerErrorCode.IFCZIP_CONTAINS_NO_IFC_FILES, "Zip files must contain exactly one IFC-file, this zip-file looks empty");
				}
				if (nextEntry.getName().toUpperCase().endsWith(".IFC")) {
					IfcModelInterface model = null;
					FakeClosingInputStream fakeClosingInputStream = new FakeClosingInputStream(zipInputStream);
					model = read(fakeClosingInputStream, fileSize, byteProgressReporter);
					if (model.size() == 0) {
						throw new DeserializeException(DeserializerErrorCode.IFCZIP_CONTAINS_EMPTY_IFC_MODEL, "Uploaded file does not seem to be a correct IFC file");
					}
					if (zipInputStream.getNextEntry() != null) {
						zipInputStream.close();
						throw new DeserializeException(DeserializerErrorCode.IFCZIP_FILE_CONTAINS_TOO_MANY_FILES, "Zip files may only contain one IFC-file, this zip-file contains more files");
					} else {
						zipInputStream.close();
						return model;
					}
				} else {
					throw new DeserializeException(DeserializerErrorCode.IFCZIP_MUST_CONTAIN_EXACTLY_ONE_IFC_FILE, "Zip files must contain exactly one IFC-file, this zip-file seems to have one or more non-IFC files");
				}
			} catch (IOException e) {
				throw new DeserializeException(e);
			}
		} else {
			return read(in, fileSize, byteProgressReporter);
		}
	}

	private IfcModelInterface read(InputStream inputStream, long fileSize, ByteProgressReporter byteProgressReporter) throws DeserializeException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
		int initialCapacity = (int) (fileSize / AVERAGE_LINE_LENGTH);
		model = new BasicIfcModel(getPackageMetaData(), null, initialCapacity);
		long bytesRead = 0;
		lineNumber = 0;
		try {
			String line = reader.readLine();
			if (line == null) {
				throw new DeserializeException(DeserializerErrorCode.UNEXPECTED_END_OF_STREAM_WHILE_READING_FIRST_LINE, "Unexpected end of stream reading first line " + model);
			}
			MessageDigest md = MessageDigest.getInstance("MD5");
			while (line != null) {
				byte[] bytes = line.getBytes(Charsets.UTF_8);
				md.update(bytes, 0, bytes.length);
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
				bytesRead += bytes.length;
				if (byteProgressReporter != null) {
					byteProgressReporter.progress(bytesRead);
				}

				line = reader.readLine();
				lineNumber++;
			}
			model.getModelMetaData().setChecksum(md.digest());
			if (mode == Mode.HEADER) {
				throw new DeserializeException(DeserializerErrorCode.NO_VALID_IFC_HEADER_FOUND, lineNumber, "No valid IFC header found");
			}
		} catch (IOException e) {
			throw new DeserializeException(DeserializerErrorCode.IO_EXCEPTION, lineNumber, e);
		} catch (NoSuchAlgorithmException e) {
			throw new DeserializeException(DeserializerErrorCode.INTERNAL_BIMSERVER_ERROR, lineNumber, e);
		}
		return model;
	}

	public IfcModelInterface read(File sourceFile) throws DeserializeException {
		try {
			FileInputStream in = new FileInputStream(sourceFile);
			read(in, sourceFile.length(), null);
			in.close();
			model.getModelMetaData().setDate(new Date());
			model.getModelMetaData().setName(sourceFile.getName());
			return model;
		} catch (FileNotFoundException e) {
			throw new DeserializeException(DeserializerErrorCode.FILE_NOT_FOUND_EXCEPTION, lineNumber, e);
		} catch (IOException e) {
			throw new DeserializeException(DeserializerErrorCode.IO_EXCEPTION, lineNumber, e);
		}
	}

	public IfcModelInterface getModel() {
		return model;
	}

	private boolean processLine(String line) throws DeserializeException, MetaDataException {
		switch (mode) {
		case HEADER:
			if (line.length() > 0) {
				if (line.endsWith(";")) {
					processHeader(line);
				} else {
					return false;
				}
			}
			if (line.equals("DATA;")) {
				mode = Mode.DATA;
			}
			break;
		case DATA:
			if (line.equals("ENDSEC;")) {
				mode = Mode.FOOTER;
			} else {
				if (line.length() > 0 && line.charAt(0) == '#') {
					while (line.endsWith("*/")) {
						line = line.substring(0, line.lastIndexOf("/*")).trim();
					}
					if (line.endsWith(";")) {
						processRecord(line);
					} else {
						return false;
					}
				}
			}
			break;
		case FOOTER:
			if (line.equals("ENDSEC;")) {
				mode = Mode.DONE;
			}
			break;
		case DONE:
		}
		return true;
	}

	private void processHeader(String line) throws DeserializeException {
		IfcHeader ifcHeader = model.getModelMetaData().getIfcHeader();
		if (ifcHeader == null) {
			ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
			model.getModelMetaData().setIfcHeader(ifcHeader);
		}
		
		if (line.startsWith("FILE_DESCRIPTION")) {
			String filedescription = line.substring("FILE_DESCRIPTION".length()).trim();
			new IfcHeaderParser().parseDescription(filedescription.substring(1, filedescription.length() - 2), ifcHeader, lineNumber);
		} else if (line.startsWith("FILE_NAME")) {
			String filename = line.substring("FILE_NAME".length()).trim();
			new IfcHeaderParser().parseFileName(filename.substring(1, filename.length() - 2), ifcHeader, lineNumber);
		} else if (line.startsWith("FILE_SCHEMA")) {
			String fileschema = line.substring("FILE_SCHEMA".length()).trim();
			new IfcHeaderParser().parseFileSchema(fileschema.substring(1, fileschema.length() - 2), ifcHeader, lineNumber);

			String ifcSchemaVersion = ifcHeader.getIfcSchemaVersion();
			if (!ifcSchemaVersion.toLowerCase().equalsIgnoreCase(schema.getHeaderName().toLowerCase())) {
				throw new DeserializeException(DeserializerErrorCode.UNSUPPORTED_IFC_SCHEMA_VERSION, lineNumber, ifcSchemaVersion + " is not supported by this deserializer (" + schema.getHeaderName() + " is)");
			}
			ifcHeader.setIfcSchemaVersion(ifcSchemaVersion);
		} else if (line.startsWith("ENDSEC;")) {
			// Do nothing
		}
	}

	public void processRecord(String line) throws DeserializeException, MetaDataException {
		int equalSignLocation = line.indexOf("=");
		int lastIndexOfSemiColon = line.lastIndexOf(";");
		if (lastIndexOfSemiColon == -1) {
			throw new DeserializeException(DeserializerErrorCode.NO_SEMICOLON_FOUND_IN_RECORD, lineNumber, "No semicolon found in line");
		}
		int indexOfFirstParen = line.indexOf("(", equalSignLocation);
		if (indexOfFirstParen == -1) {
			throw new DeserializeException(DeserializerErrorCode.NO_LEFT_PARENTHESIS_FOUND_IN_RECORD, lineNumber, "No left parenthesis found in line");
		}
		int indexOfLastParen = line.lastIndexOf(")", lastIndexOfSemiColon);
		if (indexOfLastParen == -1) {
			throw new DeserializeException(DeserializerErrorCode.NO_RIGHT_PARENTHESIS_FOUND_IN_RECORD, lineNumber, "No right parenthesis found in line");
		}
		long recordNumber = Long.parseLong(line.substring(1, equalSignLocation).trim());
		String name = line.substring(equalSignLocation + 1, indexOfFirstParen).trim();
		EClass eClass = (EClass) getPackageMetaData().getEClassifierCaseInsensitive(name);
		if (eClass != null) {
			IdEObject object = (IdEObject) getPackageMetaData().create(eClass);
			try {
				model.add(recordNumber, object);
			} catch (ObjectAlreadyExistsException e) {
				throw new DeserializeException(DeserializerErrorCode.DUPLICATE_EXPRESS_ID, lineNumber, e);
			}
			((IdEObjectImpl) object).setExpressId(recordNumber);
			String realData = line.substring(indexOfFirstParen + 1, indexOfLastParen);
			int lastIndex = 0;
			EntityDefinition entityBN = getPackageMetaData().getSchemaDefinition().getEntityBN(name);
			if (entityBN == null) {
				throw new DeserializeException(DeserializerErrorCode.UNKNOWN_ENTITY, lineNumber, "Unknown entity " + name);
			}
			for (EStructuralFeature eStructuralFeature : eClass.getEAllStructuralFeatures()) {
				if (getPackageMetaData().useForSerialization(eClass, eStructuralFeature)) {
					if (getPackageMetaData().useForDatabaseStorage(eClass, eStructuralFeature)) {
						int nextIndex = StringUtils.nextString(realData, lastIndex);
						if (nextIndex <= lastIndex && eStructuralFeature == Ifc4Package.eINSTANCE.getIfcSurfaceStyleShading_Transparency()) {
							// IFC4 add1/add2 hack
							
							// 21-11-2018 Ruben de Laat: Disabled as it's an optional field, see https://github.com/opensourceBIM/BIMserver/issues/689#issuecomment-383588646
//							object.eSet(eStructuralFeature, 0D);
//							object.eSet(eStructuralFeature.getEContainingClass().getEStructuralFeature(eStructuralFeature.getName() + "AsString"), "0");
							continue;
						}
						String val = null;
						try {
							val = realData.substring(lastIndex, nextIndex - 1).trim();
						} catch (Exception e) {
							int expected = 0;
							for (Attribute attribute2 : entityBN.getAttributesCached(true)) {
								if (attribute2 instanceof ExplicitAttribute) {
									expected++;
								}
							}
							throw new DeserializeException(DeserializerErrorCode.NOT_ENOUGH_FIELDS_FOR_ENTITY, lineNumber, eClass.getName() + " expects " + expected + " fields, but less found");
						}
						lastIndex = nextIndex;
						char firstChar = val.charAt(0);
						if (firstChar == '$') {
							object.eUnset(eStructuralFeature);
							if (eStructuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
								EStructuralFeature doubleStringFeature = eClass.getEStructuralFeature(eStructuralFeature.getName() + "AsString");
								object.eUnset(doubleStringFeature);
							}
						} else if (firstChar == '#') {
							readReference(val, object, eStructuralFeature);
						} else if (firstChar == '.') {
							readEnum(val, object, eStructuralFeature);
						} else if (firstChar == '(') {
							readList(val, object, eStructuralFeature);
						} else if (firstChar == '*') {
						} else {
							if (!eStructuralFeature.isMany()) {
								object.eSet(eStructuralFeature, convert(eStructuralFeature, eStructuralFeature.getEType(), val));
								if (eStructuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
									EStructuralFeature doubleStringFeature = eClass.getEStructuralFeature(eStructuralFeature.getName() + "AsString");
									object.eSet(doubleStringFeature, val);
								}
							} else {
								// It's not a list in the file, but it is in the
								// schema??
							}
						}
					} else {
						int nextIndex = StringUtils.nextString(realData, lastIndex);
						lastIndex = nextIndex;
					}
				} else {
					if (getPackageMetaData().useForDatabaseStorage(eClass, eStructuralFeature)) {
						if (eStructuralFeature instanceof EReference && getPackageMetaData().isInverse((EReference) eStructuralFeature)) {
							object.eUnset(eStructuralFeature);
						} else {
							if (eStructuralFeature.getEAnnotation("asstring") == null) {
								object.eUnset(eStructuralFeature);
							}
						}
					}
				}
			}
			if (waitingList.containsKey(recordNumber)) {
				waitingList.updateNode(recordNumber, eClass, object);
			}
		} else {
			throw new DeserializeException(DeserializerErrorCode.UNKNOWN_ENTITY, lineNumber, name + " is not a known entity");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void readList(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException, MetaDataException {
		int index = 0;
		if (!structuralFeature.isMany()) {
			throw new DeserializeException(DeserializerErrorCode.UNEXPECTED_AGGREGATION, lineNumber, "Field " + structuralFeature.getName() + " of " + structuralFeature.getEContainingClass().getName() + " is no aggregation");
		}
		AbstractEList list = (AbstractEList) object.eGet(structuralFeature);
		AbstractEList doubleStringList = null;
		if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
			EStructuralFeature doubleStringFeature = structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString");
			if (doubleStringFeature == null) {
				throw new DeserializeException(DeserializerErrorCode.INTERNAL_BIMSERVER_ERROR, lineNumber, "Field not found: " + structuralFeature.getName() + "AsString");
			}
			doubleStringList = (AbstractEList) object.eGet(doubleStringFeature);
		}
		String realData = val.substring(1, val.length() - 1);
		int lastIndex = 0;
		while (lastIndex != realData.length() + 1) {
			int nextIndex = StringUtils.nextString(realData, lastIndex);
			String stringValue = realData.substring(lastIndex, nextIndex - 1).trim();
			lastIndex = nextIndex;
			if (stringValue.length() > 0) {
				if (stringValue.charAt(0) == '#') {
					long referenceId = Long.parseLong(stringValue.substring(1));
					if (model.contains(referenceId)) {
						EObject referencedObject = model.get(referenceId);
						if (referencedObject != null) {
							EClass referenceEClass = referencedObject.eClass();
							if (((EClass) structuralFeature.getEType()).isSuperTypeOf(referenceEClass)) {
								while (list.size() <= index) {
									list.addUnique(referencedObject);
								}
								list.setUnique(index, referencedObject);
							} else {
								throw new DeserializeException(DeserializerErrorCode.REFERENCED_OBJECT_CANNOT_BE_STORED_IN_THIS_FIELD, lineNumber, referenceEClass.getName() + " cannot be stored in " + structuralFeature.getName());
							}
						}
					} else {
						waitingList.add(referenceId, new ListWaitingObject(lineNumber, object, (EReference) structuralFeature, index));
					}
				} else if (stringValue.charAt(0) == '(') {
					// Two dimensional list
					IdEObject newObject = (IdEObject) getPackageMetaData().create((EClass) structuralFeature.getEType());
					readList(stringValue, newObject, newObject.eClass().getEStructuralFeature("List"));
					list.addUnique(newObject);
				} else {
					Object convert = convert(structuralFeature, structuralFeature.getEType(), stringValue);
					if (convert != null) {
						while (list.size() <= index) {
							if (doubleStringList != null) {
								doubleStringList.addUnique(stringValue);
							}
							list.addUnique(convert);
						}
						if (doubleStringList != null) {
							doubleStringList.setUnique(index, stringValue);
						}
						list.setUnique(index, convert);
					}
				}
			}
			index++;
		}
	}

	private Object convert(EStructuralFeature eStructuralFeature, EClassifier classifier, String value) throws DeserializeException, MetaDataException {
		if (classifier != null) {
			if (classifier instanceof EClassImpl) {
				if (null != ((EClassImpl) classifier).getEStructuralFeature(WRAPPED_VALUE)) {
					IdEObject newObject = (IdEObject) getPackageMetaData().create((EClass) classifier);
					Class<?> instanceClass = newObject.eClass().getEStructuralFeature(WRAPPED_VALUE).getEType().getInstanceClass();
					if (value.equals("")) {

					} else if (value.charAt(0) == '(') {
						readList(value, newObject, newObject.eClass().getEStructuralFeature(WRAPPED_VALUE));
					} else {
						if (instanceClass == Integer.class || instanceClass == int.class) {
							try {
								newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Integer.parseInt(value));
							} catch (NumberFormatException e) {
								throw new DeserializeException(DeserializerErrorCode.INVALID_INTEGER_LITERAL, lineNumber, value + " is not a valid integer(32) value");
							}
						} else if (instanceClass == Long.class || instanceClass == long.class) {
							try {
								newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Long.parseLong(value));
							} catch (NumberFormatException e) {
								throw new DeserializeException(DeserializerErrorCode.INVALID_LONG_LITERAL, lineNumber, value + " is not a valid integer(64) value");
							}
						} else if (instanceClass == Boolean.class || instanceClass == boolean.class) {
							newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), value.equals(".T."));
						} else if (instanceClass == Double.class || instanceClass == double.class) {
							try {
								newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Double.parseDouble(value));
							} catch (NumberFormatException e) {
								throw new DeserializeException(DeserializerErrorCode.INVALID_DOUBLE_LITERAL, lineNumber, value + " is not a valid floating point(32) number");
							}
							newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE + "AsString"), value);
						} else if (instanceClass == String.class) {
							newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), IfcParserWriterUtils.readString(value, lineNumber));
						} else if (instanceClass.getSimpleName().equals("Tristate")) {
							Object tristate = null;
							if (value.equals(".T.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance();
							} else if (value.equals(".F.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance();
							} else if (value.equals(".U.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance();
							}
							newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), tristate);
						} else {
							throw new DeserializeException(DeserializerErrorCode.UNIMPLEMENTED_BIMSERVER_FEATURE, lineNumber, instanceClass.getSimpleName() + " not implemented");
						}
					}
					return newObject;
				} else {
					return processInline(eStructuralFeature, classifier, value);
				}
			} else if (classifier instanceof EDataType) {
				return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, classifier.getInstanceClass(), value, lineNumber);
			}
		}
		return null;
	}

	private Object processInline(EStructuralFeature eStructuralFeature, EClassifier classifier, String value) throws DeserializeException, MetaDataException {
		if (value.indexOf("(") != -1) {
			String typeName = value.substring(0, value.indexOf("(")).trim();
			String v = value.substring(value.indexOf("(") + 1, value.length() - 1);
			EClassifier eClassifier = getPackageMetaData().getEClassifierCaseInsensitive(typeName);
			if (eClassifier instanceof EClass) {
				Object convert = convert(eStructuralFeature, eClassifier, v);
				try {
					model.add(-1, (IdEObject) convert);
				} catch (ObjectAlreadyExistsException e) {
					throw new DeserializeException(DeserializerErrorCode.DUPLICATE_EXPRESS_ID, lineNumber, e);
				}
				return convert;
			} else {
				throw new DeserializeException(DeserializerErrorCode.NON_EXISTING_ENTITY_TYPE_USED, lineNumber, typeName + " is not an existing IFC entity type");
			}
		} else {
			return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, classifier.getInstanceClass(), value, lineNumber);
		}
	}

	private void readEnum(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException, MetaDataException {
		if (val.equals(".T.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
			} else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
				EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
				EObject createIfcBoolean = getPackageMetaData().create(eClass);
				createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
				object.eSet(structuralFeature, createIfcBoolean);
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.eSet(structuralFeature, true);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				EObject createIfcBoolean = getPackageMetaData().create(eClass);
				createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
				object.eSet(structuralFeature, createIfcBoolean);
			}
		} else if (val.equals(".F.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
			} else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
				EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
				EObject createIfcBoolean = getPackageMetaData().create(eClass);
				createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
				object.eSet(structuralFeature, createIfcBoolean);
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.eSet(structuralFeature, false);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				EObject createIfcBoolean = getPackageMetaData().create(eClass);
				createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
				object.eSet(structuralFeature, createIfcBoolean);
			}
		} else if (val.equals(".U.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.eUnset(structuralFeature);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				EObject createIfcBoolean = getPackageMetaData().create(eClass);
				createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
				object.eSet(structuralFeature, createIfcBoolean);
			}
		} else {
			if (structuralFeature.getEType() instanceof EEnumImpl) {
				String realEnumValue = val.substring(1, val.length() - 1);
				EEnumLiteral enumValue = (((EEnumImpl) structuralFeature.getEType()).getEEnumLiteral(realEnumValue));
				if (enumValue == null) {
					// Another IFC4/add1/add2 hack
					if (structuralFeature.getEType() == Ifc4Package.eINSTANCE.getIfcExternalSpatialElementTypeEnum() && realEnumValue.equals("NOTDEFIEND")) {
						realEnumValue = "NOTDEFINED";
						enumValue = (((EEnumImpl) structuralFeature.getEType()).getEEnumLiteral(realEnumValue));
					}
				}
				if (enumValue == null) {
					throw new DeserializeException(DeserializerErrorCode.NON_EXISTING_ENUM_LITERAL_USED, lineNumber, "Enum type " + structuralFeature.getEType().getName() + " has no literal value '" + realEnumValue + "'");
				}
				object.eSet(structuralFeature, enumValue.getInstance());
			} else {
				throw new DeserializeException(DeserializerErrorCode.UNEXPECTED_ENUM, lineNumber, "Value " + val + " indicates enum type but " + structuralFeature.getEType().getName() + " expected");
			}
		}
	}

	private void readReference(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException {
		if (structuralFeature == Ifc4Package.eINSTANCE.getIfcIndexedColourMap_Opacity()) {
			// HACK for IFC4/add1/add2
			object.eSet(structuralFeature, 0D);
			object.eSet(structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString"), "0");
			return;
		}

		long referenceId;
		try {
			referenceId = Long.parseLong(val.substring(1));
		} catch (NumberFormatException e) {
			throw new DeserializeException(DeserializerErrorCode.INVALID_REFERENCE, lineNumber, "'" + val + "' is not a valid reference");
		}
		if (model.contains(referenceId)) {
			object.eSet(structuralFeature, model.get(referenceId));
		} else {
			waitingList.add(referenceId, new SingleWaitingObject(lineNumber, object, (EReference) structuralFeature));
		}
	}
}