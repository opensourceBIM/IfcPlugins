package org.bimserver.serializers;

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

import java.util.HashSet;
import java.util.Set;

import org.bimserver.emf.Schema;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.StringType;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.serializers.StreamingSerializer;
import org.bimserver.plugins.serializers.StreamingSerializerPlugin;
import org.bimserver.shared.exceptions.PluginException;

public class JsonStreamingSerializerPlugin implements StreamingSerializerPlugin {

	@Override
	public void init(PluginContext pluginContext, PluginConfiguration systemSettings) throws PluginException {
	}

	@Override
	public ObjectDefinition getUserSettingsDefinition() {
		ObjectDefinition objectDefinition = StoreFactory.eINSTANCE.createObjectDefinition();

		ParameterDefinition extensionParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		extensionParameter.setIdentifier(EXTENSION);
		extensionParameter.setDescription("Extension of the downloaded file");
		PrimitiveDefinition stringType = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringType.setType(PrimitiveEnum.STRING);
		extensionParameter.setType(stringType);
		StringType defaultExtensionValue = StoreFactory.eINSTANCE.createStringType();
		defaultExtensionValue.setValue("json");
		extensionParameter.setDefaultValue(defaultExtensionValue);
		objectDefinition.getParameters().add(extensionParameter);

		ParameterDefinition contentTypeParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		contentTypeParameter.setIdentifier(CONTENT_TYPE);
		contentTypeParameter.setDescription("Content-Type in the HTTP header of the downloaded file");
		contentTypeParameter.setType(stringType);
		StringType defaultContentTypeValue = StoreFactory.eINSTANCE.createStringType();
		defaultContentTypeValue.setValue("application/json");
		contentTypeParameter.setDefaultValue(defaultContentTypeValue);
		objectDefinition.getParameters().add(contentTypeParameter);
		
		PrimitiveDefinition stringDefinition = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringDefinition.setType(PrimitiveEnum.STRING);
		
		ParameterDefinition zipExtension = StoreFactory.eINSTANCE.createParameterDefinition();
		zipExtension.setIdentifier(ZIP_EXTENSION);
		zipExtension.setDescription("Extension of the downloaded file when using zip compression");
		zipExtension.setType(stringDefinition);
		StringType defaultZipExtensionValue = StoreFactory.eINSTANCE.createStringType();
		defaultZipExtensionValue.setValue("zip");
		zipExtension.setDefaultValue(defaultZipExtensionValue);
		objectDefinition.getParameters().add(zipExtension);
		
		return objectDefinition;
	}
	
	@Override
	public ObjectDefinition getSystemSettingsDefinition() {
		return null;
	}

	@Override
	public StreamingSerializer createSerializer(PluginConfiguration pluginConfiguration) {
		return new StreamingJsonSerializer(pluginConfiguration);
	}

	@Override
	public Set<Schema> getSupportedSchemas() {
		Set<Schema> schemas = new HashSet<>();
		schemas.add(Schema.IFC2X3TC1);
		schemas.add(Schema.IFC4);
		return schemas;
	}

	@Override
	public String getOutputFormat(Schema schema) {
		switch (schema) {
		case IFC2X3TC1:
			return "IFC_JSON_2X3TC1";
		case IFC4:
			return "IFC_JSON_4";
		default:
			return null;
		}
	}
}