package org.bimserver.deserializers;

import java.io.InputStream;
import java.util.Map;

import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.SharedJsonStreamingDeserializer;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.StreamingDeserializer;
import org.bimserver.shared.QueryContext;
import org.eclipse.emf.ecore.EClass;

public class JsonStreamingDeserializer implements StreamingDeserializer {

	private PackageMetaData packageMetaData;

	@Override
	public void init(PackageMetaData packageMetaData) {
		this.packageMetaData = packageMetaData;
	}

	@Override
	public void setProgressReporter(ByteProgressReporter byteProgressReporter) {
	}

	@Override
	public long read(InputStream inputStream, String fileName, long fileSize, QueryContext reusable)
			throws DeserializeException {
		return new SharedJsonStreamingDeserializer(packageMetaData).read(inputStream);
	}

	@Override
	public IfcHeader getIfcHeader() {
		return null;
	}

	@Override
	public Map<EClass, Integer> getSummaryMap() {
		return null;
	}
}