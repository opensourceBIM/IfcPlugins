package org.bimserver.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializer;
import org.bimserver.ifc.step.serializer.Ifc2x3tc1StepSerializer;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.serializers.SerializerException;
import org.junit.Test;

public class TestReadWrite {
	@Test
	public void test() {
		Ifc2x3tc1StepDeserializer deserializer = new Ifc2x3tc1StepDeserializer();
		PackageMetaData packageMetaData = new PackageMetaData(Ifc2x3tc1Package.eINSTANCE, Schema.IFC2X3TC1, Paths.get("tmp"));
		deserializer.init(packageMetaData);
		
		try {
			URL url = new URL("https://raw.githubusercontent.com/opensourceBIM/IFC-files/master/HHS%20Office/construction.ifc");
			InputStream openStream = url.openStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(openStream, baos);
			IfcModelInterface model = deserializer.read(new ByteArrayInputStream(baos.toByteArray()), "", baos.size(), null);
			
			Ifc2x3tc1StepSerializer serializer = new Ifc2x3tc1StepSerializer(new PluginConfiguration());
			serializer.init(model, null, false);
			serializer.writeToFile(Paths.get("output.ifc"), null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (SerializerException e) {
			e.printStackTrace();
		}
	}
}
