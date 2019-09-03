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

import java.io.IOException;

import org.bimserver.emf.Schema;

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

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.deserializers.IfcSchemaDeterminer;
import org.bimserver.shared.exceptions.PluginException;

/**
 * @author Ruben de Laat
 *	Replaced by the streaming deserializers
 */
@Deprecated
public abstract class IfcStepDeserializerPlugin implements DeserializerPlugin, IfcSchemaDeterminer {

	@Override
	public void init(PluginContext pluginContext, PluginConfiguration systemSettings) throws PluginException {
	}

	@Override
	public boolean canHandleExtension(String extension) {
		return extension.equalsIgnoreCase("ifc") || extension.equalsIgnoreCase("ifczip");
	}

	@Override
	public ObjectDefinition getUserSettingsDefinition() {
		return null;
	}
	
	@Override
	public ObjectDefinition getSystemSettingsDefinition() {
		return null;
	}

	@Override
	public Schema determineSchema(byte[] head, boolean usesZip) throws DeserializeException {
		DetectIfcVersion detectIfcVersion = new DetectIfcVersion();
		try {
			String schemaString = detectIfcVersion.detectVersion(head, usesZip);
			Schema schema = Schema.fromIfcHeader(schemaString);
			if (schema == null) {
				throw new DeserializeException(DeserializerErrorCode.UNSUPPORTED_IFC_SCHEMA_VERSION, "Unsupported IFC schema: " + schemaString);
			}
			return schema;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}