package org.bimserver.ifc.step.serializer;

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

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.StringType;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.serializers.AbstractSerializerPlugin;
import org.bimserver.shared.exceptions.PluginException;

public abstract class IfcStepSerializerPlugin extends AbstractSerializerPlugin {

	@Override
	public void init(PluginContext pluginContext, PluginConfiguration systemSettings) throws PluginException {
	}

	@Override
	public String getDefaultContentType() {
		return "application/ifc";
	}

	@Override
	public String getDefaultExtension() {
		return "ifc";
	}

	@Override
	public ObjectDefinition getUserSettingsDefinition() {
		ObjectDefinition objectDefinition = super.getUserSettingsDefinition();

		PrimitiveDefinition stringDefinition = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringDefinition.setType(PrimitiveEnum.STRING);
		
		ParameterDefinition organizationParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		organizationParameter.setIdentifier("organization");
		organizationParameter.setName("Organization");
		organizationParameter.setDescription("Organization name to put in the header");
		StringType defaultValue = StoreFactory.eINSTANCE.createStringType();
		defaultValue.setValue("BIMserver.org");
		organizationParameter.setDefaultValue(defaultValue);
		organizationParameter.setType(stringDefinition);
		objectDefinition.getParameters().add(organizationParameter);
		
		ParameterDefinition zipExtension = StoreFactory.eINSTANCE.createParameterDefinition();
		zipExtension.setIdentifier(ZIP_EXTENSION);
		zipExtension.setName(ZIP_EXTENSION);
		zipExtension.setDescription("Extension of the downloaded file when using zip compression");
		zipExtension.setType(stringDefinition);
		StringType defaultZipExtensionValue = StoreFactory.eINSTANCE.createStringType();
		defaultZipExtensionValue.setValue("ifczip");
		zipExtension.setDefaultValue(defaultZipExtensionValue);
		objectDefinition.getParameters().add(zipExtension);
		
		return objectDefinition;
	}
}