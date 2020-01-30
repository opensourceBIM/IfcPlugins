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

import java.util.Set;

import org.bimserver.emf.Schema;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.serializers.AbstractSerializerPlugin;
import org.bimserver.plugins.serializers.EmfSerializer;
import org.bimserver.shared.exceptions.PluginException;

/**
 * @author Ruben de Laat
 * Deprecated, use the JsonStreamingSerializer
 */
@Deprecated
public class JsonSerializerPluginWithGeometry extends AbstractSerializerPlugin {

	@Override
	public EmfSerializer createSerializer(PluginConfiguration pluginConfiguration) {
		return new JsonSerializerWithGeometry();
	}

	@Override
	public void init(PluginContext pluginContext, PluginConfiguration systemSettings) throws PluginException {
	}

	@Override
	public String getDefaultContentType() {
		return "application/json";
	}

	@Override
	public String getDefaultExtension() {
		return "json";
	}

	@Override
	public ObjectDefinition getUserSettingsDefinition() {
		return super.getUserSettingsDefinition();
	}
	
	@Override
	public Set<Schema> getSupportedSchemas() {
		return Schema.asSet(Schema.IFC2X3TC1, Schema.IFC4);
	}

	@Override
	public String getOutputFormat(Schema schema) {
		switch (schema) {
		case IFC2X3TC1:
			return "IFC_JSON_GEOM_2x3TC1";
		case IFC4:
			return "IFC_JSON_GEOM_4";
		default: return null;
		}
	}
}