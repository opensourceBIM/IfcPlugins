package org.bimserver.ifc.step.deserializer;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bimserver.BimserverDatabaseException;
import org.bimserver.database.MetricCollector;
import org.bimserver.emf.MetaDataException;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DatabaseInterface;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.StreamingDeserializer;
import org.bimserver.shared.ByteBufferList;
import org.bimserver.shared.ByteBufferVirtualObject;
import org.bimserver.shared.ByteBufferWrappedVirtualObject;
import org.bimserver.shared.Guid;
import org.bimserver.shared.GuidCompressor;
import org.bimserver.shared.InvalidGuidException;
import org.bimserver.shared.ListCapableVirtualObject;
import org.bimserver.shared.ListWaitingVirtualObject;
import org.bimserver.shared.PrimitiveByteBufferList;
import org.bimserver.shared.QueryContext;
import org.bimserver.shared.SingleWaitingVirtualObject;
import org.bimserver.shared.VirtualObject;
import org.bimserver.shared.WaitingListVirtualObject;
import org.bimserver.shared.WrappedVirtualObject;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.bimserver.utils.FakeClosingInputStream;
import org.bimserver.utils.StringUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EEnumImpl;

import com.google.common.base.Charsets;

import nl.tue.buildingsmart.schema.Attribute;
import nl.tue.buildingsmart.schema.EntityDefinition;
import nl.tue.buildingsmart.schema.ExplicitAttribute;

public abstract class IfcStepStreamingDeserializer implements StreamingDeserializer {
	private ByteProgressReporter byteProgressReporter;
	private PackageMetaData packageMetaData;
	private static final String WRAPPED_VALUE = "wrappedValue";
	private WaitingListVirtualObject<Integer> waitingList;
	private Mode mode = Mode.HEADER;
	private int lineNumber;
	private Schema schema;
	
	// ExpressID -> ObjectID
	// TODO find more efficient implementation
	private Map<Integer, Long> mappedObjects;
	private QueryContext reusable;
	private IfcHeader ifcHeader;
	
	private static MetricCollector metricCollector = new MetricCollector();
	
	// Use String instead of EClass, compare takes 1.7%
	private final Map<String, AtomicInteger> summaryMap = new TreeMap<>();

	@Override
	public void init(PackageMetaData packageMetaData) {
		this.packageMetaData = packageMetaData;
		this.schema = packageMetaData.getSchema();
	}
	
	@Override
	public Map<EClass, Integer> getSummaryMap() {
		Map<EClass, Integer> newMap = new TreeMap<>(new Comparator<EClass>(){
			@Override
			public int compare(EClass o1, EClass o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (String key : this.summaryMap.keySet()) {
			EClass eClass = packageMetaData.getEClass(key);
			newMap.put(eClass, this.summaryMap.get(key).get());
		}
		return newMap;
	}
	
	@Override
	public void setProgressReporter(ByteProgressReporter byteProgressReporter) {
		this.byteProgressReporter = byteProgressReporter;
	}
	
	public enum Mode {
		HEADER, DATA, FOOTER, DONE
	}

	public PackageMetaData getPackageMetaData() {
		return packageMetaData;
	}
	
	@Override
	public long read(InputStream in, String filename, long fileSize, QueryContext reusable) throws DeserializeException {
		this.reusable = reusable;
		mappedObjects = new HashMap<>();
		waitingList = new WaitingListVirtualObject<Integer>();
		mode = Mode.HEADER;
		if (filename != null && (filename.toUpperCase().endsWith(".ZIP") || filename.toUpperCase().endsWith(".IFCZIP"))) {
			ZipInputStream zipInputStream = new ZipInputStream(in);
			try {
				ZipEntry nextEntry = zipInputStream.getNextEntry();
				if (nextEntry == null) {
					throw new DeserializeException("Zip files must contain exactly one IFC-file, this zip-file looks empty");
				}
				if (nextEntry.getName().toUpperCase().endsWith(".IFC")) {
					FakeClosingInputStream fakeClosingInputStream = new FakeClosingInputStream(zipInputStream);
					long size = read(fakeClosingInputStream, fileSize);
					if (size == 0) {
						throw new DeserializeException("Uploaded file does not seem to be a correct IFC file");
					}
					if (zipInputStream.getNextEntry() != null) {
						zipInputStream.close();
						throw new DeserializeException("Zip files may only contain one IFC-file, this zip-file contains more files");
					} else {
						zipInputStream.close();
					}
					return size;
				} else {
					throw new DeserializeException("Zip files must contain exactly one IFC-file, this zip-file seems to have one or more non-IFC files");
				}
			} catch (IOException e) {
				throw new DeserializeException(e);
			}
		} else {
			return read(in, fileSize);
		}
	}

	private long read(InputStream inputStream, long fileSize) throws DeserializeException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
		long bytesRead = 0;
		lineNumber = 0;
		try {
			String line = reader.readLine();
			if (line == null) {
				throw new DeserializeException(0, "Unexpected end of stream reading first line");
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
						throw new DeserializeException(lineNumber, " (" + e.getMessage() + ") " + line, e);
					}
				}
				bytesRead += bytes.length;
				if (byteProgressReporter != null) {
					byteProgressReporter.progress(bytesRead);
				}

				line = reader.readLine();
				lineNumber++;
			}
//			model.getModelMetaData().setChecksum(md.digest());
			if (mode == Mode.HEADER) {
				throw new DeserializeException(lineNumber, "No valid IFC header found");
			}
			return lineNumber;
		} catch (FileNotFoundException e) {
			throw new DeserializeException(lineNumber, e);
		} catch (IOException e) {
			throw new DeserializeException(lineNumber, e);
		} catch (NoSuchAlgorithmException e) {
			throw new DeserializeException(lineNumber, e);
		}
	}

	private boolean processLine(String line) throws DeserializeException, MetaDataException, BimserverDatabaseException {
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
				try {
					waitingList.dumpIfNotEmpty();
				} catch (BimServerClientException e) {
					e.printStackTrace();
				}
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

	public IfcHeader getIfcHeader() {
		return ifcHeader;
	}
	
	private void processHeader(String line) throws DeserializeException {
		try {
			if (ifcHeader == null) {
				ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
			}
			if (line.startsWith("FILE_DESCRIPTION")) {
				String filedescription = line.substring("FILE_DESCRIPTION".length()).trim();
				new IfcHeaderParser().parseDescription(filedescription.substring(1, filedescription.length() - 2), ifcHeader);
			} else if (line.startsWith("FILE_NAME")) {
				String filename = line.substring("FILE_NAME".length()).trim();
				new IfcHeaderParser().parseFileName(filename.substring(1, filename.length() - 2), ifcHeader);
			} else if (line.startsWith("FILE_SCHEMA")) {
				String fileschema = line.substring("FILE_SCHEMA".length()).trim();
				new IfcHeaderParser().parseFileSchema(fileschema.substring(1, fileschema.length() - 2), ifcHeader);

				String ifcSchemaVersion = ifcHeader.getIfcSchemaVersion();
				if (!ifcSchemaVersion.toLowerCase().equalsIgnoreCase(schema.getHeaderName().toLowerCase())) {
					throw new DeserializeException(lineNumber, ifcSchemaVersion + " is not supported by this deserializer (" + schema.getHeaderName() + " is)");
				}
				ifcHeader.setIfcSchemaVersion(ifcSchemaVersion);
			} else if (line.startsWith("ENDSEC;")) {
				// Do nothing
			}
		} catch (ParseException e) {
			throw new DeserializeException(lineNumber, e);
		}
	}

	private VirtualObject newVirtualObject(EClass eClass, int lineLength) {
		return new ByteBufferVirtualObject(reusable, eClass, metricCollector.estimateRequiredBytes(lineLength));
	}

	private WrappedVirtualObject newWrappedVirtualObject(EClass eClass) {
		return new ByteBufferWrappedVirtualObject(reusable, eClass);
	}
	
	public void processRecord(String line) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		int equalSignLocation = line.indexOf("=");
		int lastIndexOfSemiColon = line.lastIndexOf(";");
		if (lastIndexOfSemiColon == -1) {
			throw new DeserializeException(lineNumber, "No semicolon found in line");
		}
		int indexOfFirstParen = line.indexOf("(", equalSignLocation);
		if (indexOfFirstParen == -1) {
			throw new DeserializeException(lineNumber, "No left parenthesis found in line");
		}
		int indexOfLastParen = line.lastIndexOf(")", lastIndexOfSemiColon);
		if (indexOfLastParen == -1) {
			throw new DeserializeException(lineNumber, "No right parenthesis found in line");
		}
		int recordNumber = Integer.parseInt(line.substring(1, equalSignLocation).trim());
		String name = line.substring(equalSignLocation + 1, indexOfFirstParen).trim();
		EClass eClass = (EClass) getPackageMetaData().getEClassifierCaseInsensitive(name);

		if (eClass == null) {
			throw new DeserializeException(lineNumber, name + " is not a known entity");
		}
		
		VirtualObject object = newVirtualObject(eClass, line.length());

		AtomicInteger atomicInteger = summaryMap.get(eClass.getName());
		if (atomicInteger == null) {
			summaryMap.put(eClass.getName(), new AtomicInteger(1));
		} else {
			atomicInteger.incrementAndGet();
		}
		mappedObjects.put(recordNumber, object.getOid());

		boolean openReferences = false;

		if (eClass != null) {
			String realData = line.substring(indexOfFirstParen + 1, indexOfLastParen);
			int lastIndex = 0;
			EntityDefinition entityBN = getPackageMetaData().getSchemaDefinition().getEntityBN(name);
			if (entityBN == null) {
				throw new DeserializeException(lineNumber, "Unknown entity " + name);
			}
			for (EStructuralFeature eStructuralFeature : eClass.getEAllStructuralFeatures()) {
				if (getPackageMetaData().useForSerialization(eClass, eStructuralFeature)) {
					if (getPackageMetaData().useForDatabaseStorage(eClass, eStructuralFeature)) {
						int nextIndex = StringUtils.nextString(realData, lastIndex);
						if (nextIndex <= lastIndex && eStructuralFeature == Ifc4Package.eINSTANCE.getIfcSurfaceStyleShading_Transparency()) {
							// IFC4 add1/add2 hack
							object.set(eStructuralFeature.getName(), 0D);
							object.set(eStructuralFeature.getEContainingClass().getEStructuralFeature(eStructuralFeature.getName() + "AsString").getName(), "0");
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
							throw new DeserializeException(lineNumber, eClass.getName() + " expects " + expected + " fields, but less found (" + e.getMessage() + ")", e);
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
							if (!readReference(val, object, eStructuralFeature)) {
								openReferences = true;
							}
						} else if (firstChar == '.') {
							readEnum(val, object, eStructuralFeature);
						} else if (firstChar == '(') {
							if (!readList(val, (ListCapableVirtualObject) object, eStructuralFeature)) {
								openReferences = true;
							}
						} else if (firstChar == '*') {
							object.eUnset(eStructuralFeature);
						} else {
							if (!eStructuralFeature.isMany()) {
								Object converted = convert(eStructuralFeature, eStructuralFeature.getEType(), val);
								object.setAttribute(eStructuralFeature, converted);
								if (eStructuralFeature.getName().equals("GlobalId")) {
									try {
										GuidCompressor.getGuidFromCompressedString(converted.toString(), new Guid());
									} catch (InvalidGuidException e) {
										throw new DeserializeException("Invalid GUID: \"" + converted.toString() + "\": " + e.getMessage());
									}
								}
								if (eStructuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
									EStructuralFeature doubleStringFeature = eClass.getEStructuralFeature(eStructuralFeature.getName() + "AsString");
									object.setAttribute(doubleStringFeature, val);
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
			
			// Other objects waiting for me?
			if (waitingList.containsKey(recordNumber)) {
				waitingList.updateNode(recordNumber, eClass, object);
			}

			if (!openReferences) {
				int nrBytes = getDatabaseInterface().save(object);
				metricCollector.collect(line.length(), nrBytes);
			}
		}
	}
	
	private DatabaseInterface getDatabaseInterface() {
		return reusable.getDatabaseInterface();
	}

	private boolean readList(String val, ListCapableVirtualObject object, EStructuralFeature structuralFeature) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		int index = 0;
		if (!structuralFeature.isMany()) {
			throw new DeserializeException(lineNumber, "Field " + structuralFeature.getName() + " of " + structuralFeature.getEContainingClass().getName() + " is no aggregation");
		}
		boolean isDouble = structuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble();
		EStructuralFeature doubleStringFeature = null;
		if (isDouble) {
			doubleStringFeature = structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString");
			if (doubleStringFeature == null) {
				throw new DeserializeException(lineNumber, "Field not found: " + structuralFeature.getName() + "AsString");
			}
		}
		String realData = val.substring(1, val.length() - 1);
		int lastIndex = 0;
		object.startList(structuralFeature);
		// TODO not always instantiate
		List<String> doubles = new ArrayList<>();
		boolean complete = true;
		while (lastIndex != realData.length() + 1) {
			int nextIndex = StringUtils.nextString(realData, lastIndex);
			String stringValue = realData.substring(lastIndex, nextIndex - 1).trim();
			lastIndex = nextIndex;
			if (stringValue.length() > 0) {
				if (stringValue.charAt(0) == '#') {
					Integer referenceId = Integer.parseInt(stringValue.substring(1));
					if (mappedObjects.containsKey(referenceId)) {
						Long referencedOid = mappedObjects.get(referenceId);
						if (referencedOid != null) {
							EClass referenceEClass = getDatabaseInterface().getEClassForOid(referencedOid);
							if (((EClass) structuralFeature.getEType()).isSuperTypeOf(referenceEClass)) {
								// TODO unique checking?
								object.setListItemReference(structuralFeature, index, referenceEClass, referencedOid, -1);
							} else {
								throw new DeserializeException(lineNumber, referenceEClass.getName() + " cannot be stored in " + structuralFeature.getName());
							}
						}
					} else {
						int pos = object.reserveSpaceForListReference();
						waitingList.add(referenceId, new ListWaitingVirtualObject(lineNumber, (VirtualObject) object, structuralFeature, index, pos));
						complete = false;
					}
				} else if (stringValue.charAt(0) == '(') {
					// Two dimensional list
					if (structuralFeature.getEType() instanceof EClass) {
						ByteBufferList newObject = new ByteBufferList(reusable, (EClass) structuralFeature.getEType());
						readList(stringValue, newObject, newObject.eClass().getEStructuralFeature("List"));
						object.setListItem(structuralFeature, index, newObject);
					} else {
						PrimitiveByteBufferList newObject = new PrimitiveByteBufferList(reusable, (EDataType) structuralFeature.getEType());
						readList(stringValue, newObject, structuralFeature);
						object.setListItem(structuralFeature, index, newObject);
					}
				} else {
					Object convert = convert(structuralFeature, structuralFeature.getEType(), stringValue);
					if (convert != null) {
						object.setListItem(structuralFeature, index, convert);
						if (isDouble) {
							doubles.add(stringValue);
						}
					}
				}
			}
			index++;
		}
		object.endList();
		// TODO make more efficient
		if (isDouble) {
			object.startList(doubleStringFeature);
			int i=0;
			for (String d : doubles) {
				object.setListItem(doubleStringFeature, i++, d);
			}
			object.endList();
		}
		return complete;
	}

	private Object convert(EStructuralFeature eStructuralFeature, EClassifier classifier, String value) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		if (classifier != null) {
			if (classifier instanceof EClassImpl) {
				if (null != ((EClassImpl) classifier).getEStructuralFeature(WRAPPED_VALUE)) {
					WrappedVirtualObject newObject = newWrappedVirtualObject((EClass) classifier);
					Class<?> instanceClass = newObject.eClass().getEStructuralFeature(WRAPPED_VALUE).getEType().getInstanceClass();
					if (value.equals("")) {

					} else {
						if (instanceClass == Integer.class || instanceClass == int.class) {
							try {
								newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Integer.parseInt(value));
							} catch (NumberFormatException e) {
								throw new DeserializeException(lineNumber, value + " is not a valid integer value");
							}
						} else if (instanceClass == Long.class || instanceClass == long.class) {
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Long.parseLong(value));
						} else if (instanceClass == Boolean.class || instanceClass == boolean.class) {
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), value.equals(".T."));
						} else if (instanceClass == Double.class || instanceClass == double.class) {
							try {
								newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Double.parseDouble(value));
							} catch (NumberFormatException e) {
								throw new DeserializeException(lineNumber, value + " is not a valid double floating point number");
							}
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE + "AsString"), value);
						} else if (instanceClass == String.class) {
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), IfcParserWriterUtils.readString(value, lineNumber));
						} else if (instanceClass.getSimpleName().equals("Tristate")) {
							Object tristate = null;
							if (value.equals(".T.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "TRUE");
							} else if (value.equals(".F.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "FALSE");
							} else if (value.equals(".U.")) {
								tristate = getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED");
							}
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), tristate);
						} else if (instanceClass.isEnum()) {
							String realEnumValue = value.substring(1, value.length() - 1);
							EStructuralFeature feature = newObject.eClass().getEStructuralFeature(WRAPPED_VALUE);
							EEnumLiteral enumValue = (((EEnumImpl) feature.getEType()).getEEnumLiteral(realEnumValue));
							if (enumValue == null) {
								throw new DeserializeException(lineNumber, "Enum type " + feature.getEType().getName() + " has no literal value '" + realEnumValue + "'");
							}
							newObject.setAttribute(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), enumValue);
						} else {
							throw new DeserializeException(lineNumber, "Not implemented: " + instanceClass);
						}
					}
					return newObject;
				} else {
					return processInline(eStructuralFeature, value);
				}
			} else if (classifier instanceof EDataType) {
				return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, classifier.getInstanceClass(), value, lineNumber);
			}
		}
		return null;
	}

	private Object processInline(EStructuralFeature eStructuralFeature, String value) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		if (value.indexOf("(") != -1) {
			String typeName = value.substring(0, value.indexOf("(")).trim();
			String v = value.substring(value.indexOf("(") + 1, value.length() - 1);
			EClassifier eClassifier = getPackageMetaData().getEClassifierCaseInsensitive(typeName);
			if (eClassifier instanceof EClass) {
				return convert(eStructuralFeature, eClassifier, v);
			} else {
				throw new DeserializeException(lineNumber, typeName + " is not an existing IFC entity");
			}
		} else {
			return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, eStructuralFeature.getEType().getInstanceClass(), value, lineNumber);
		}
	}

	private void readEnum(String val, VirtualObject object, EStructuralFeature structuralFeature) throws DeserializeException, MetaDataException, BimserverDatabaseException {
		if (val.equals(".T.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.setAttribute(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
			} else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
				EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
				VirtualObject createIfcBoolean = newVirtualObject(eClass, val.length());
				createIfcBoolean.setAttribute(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
				object.setAttribute(structuralFeature, createIfcBoolean);
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.setAttribute(structuralFeature, true);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				VirtualObject createIfcBoolean = newVirtualObject(eClass, val.length());
				createIfcBoolean.setAttribute(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
				object.setAttribute(structuralFeature, createIfcBoolean);
			}
		} else if (val.equals(".F.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.setAttribute(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
			} else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
				EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
				VirtualObject createIfcBoolean = newVirtualObject(eClass, val.length());
				createIfcBoolean.setAttribute(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
				object.setAttribute(structuralFeature, createIfcBoolean);
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.setAttribute(structuralFeature, false);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				VirtualObject createIfcBoolean = newVirtualObject(eClass, val.length());
				createIfcBoolean.setAttribute(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
				object.setAttribute(structuralFeature, createIfcBoolean);
			}
		} else if (val.equals(".U.")) {
			if (structuralFeature.getEType().getName().equals("Tristate")) {
				object.setAttribute(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
			} else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
				object.eUnset(structuralFeature);
			} else {
				EClass eClass = getPackageMetaData().getEClass("IfcLogical");
				VirtualObject createIfcBoolean = newVirtualObject(eClass, val.length());
				createIfcBoolean.setAttribute(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
				object.setAttribute(structuralFeature, createIfcBoolean);
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
					throw new DeserializeException(lineNumber, "Enum type " + structuralFeature.getEType().getName() + " has no literal value '" + realEnumValue + "'");
				}
				object.setAttribute(structuralFeature, enumValue.getInstance());
			} else {
				throw new DeserializeException(lineNumber, "Value " + val + " indicates enum type but " + structuralFeature.getEType().getName() + " expected");
			}
		}
	}

	private boolean readReference(String val, VirtualObject object, EStructuralFeature structuralFeature) throws DeserializeException, BimserverDatabaseException {
		if (structuralFeature == Ifc4Package.eINSTANCE.getIfcIndexedColourMap_Opacity()) {
			// HACK for IFC4/add1/add2
			object.setAttribute(structuralFeature, 0D);
			object.setAttribute(structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString"), "0");
			return true;
		}
		try {
			int referenceId = Integer.parseInt(val.substring(1));
			if (mappedObjects.containsKey(referenceId)) {
				object.setReference(structuralFeature, mappedObjects.get(referenceId), -1);
				return true;
			} else {
				int pos = object.reserveSpaceForReference(structuralFeature);
				waitingList.add(referenceId, new SingleWaitingVirtualObject(lineNumber, object, structuralFeature, pos));
				return false;
			}
		} catch (NumberFormatException e) {
			throw new DeserializeException(lineNumber, "'" + val + "' is not a valid reference");
		}
	}
}