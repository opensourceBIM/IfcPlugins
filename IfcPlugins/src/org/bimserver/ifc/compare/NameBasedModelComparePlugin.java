package org.bimserver.ifc.compare;

/******************************************************************************
 * Copyright (C) 2009-2016  BIMserver.org
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

import org.bimserver.emf.PackageMetaData;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.modelcompare.ModelCompare;
import org.bimserver.plugins.modelcompare.ModelCompareException;
import org.bimserver.plugins.modelcompare.ModelComparePlugin;
import org.bimserver.plugins.objectidms.HideAllInversesObjectIDM;
import org.bimserver.plugins.objectidms.ObjectIDM;
import org.bimserver.shared.exceptions.PluginException;
import org.eclipse.emf.ecore.EPackage;

public class NameBasedModelComparePlugin implements ModelComparePlugin {

	private PluginContext pluginContext;

	@Override
	public void init(PluginContext pluginContext) throws PluginException {
		this.pluginContext = pluginContext;
	}

	@Override
	public ModelCompare createModelCompare(PluginConfiguration pluginConfiguration, PackageMetaData packageMetaData) throws ModelCompareException {
		Set<EPackage> packages = new HashSet<>();
		packages.add(packageMetaData.getEPackage());
		ObjectIDM objectIDM = new HideAllInversesObjectIDM(packages, packageMetaData);
		return new NameBasedModelCompare(objectIDM);
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		return null;
	}
}
