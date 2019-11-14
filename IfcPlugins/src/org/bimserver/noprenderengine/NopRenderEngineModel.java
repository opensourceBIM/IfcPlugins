package org.bimserver.noprenderengine;

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

import java.util.ArrayList;
import java.util.Collection;

import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineFilter;
import org.bimserver.plugins.renderengine.RenderEngineInstance;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.bimserver.plugins.renderengine.RenderEngineSettings;

public class NopRenderEngineModel implements RenderEngineModel {

	@Override
	public void setFormat(int format, int mask) throws RenderEngineException {
	}

	@Override
	public void setSettings(RenderEngineSettings settings) throws RenderEngineException {
	}

	@Override
	public RenderEngineInstance getInstanceFromExpressId(long oid) throws RenderEngineException {
		return new NopRenderEngineInstance();
	}

	@Override
	public Collection<RenderEngineInstance> listInstances() throws RenderEngineException {
		return new ArrayList<>();
	}

	@Override
	public void generateGeneralGeometry() throws RenderEngineException {
	}

	@Override
	public void close() throws RenderEngineException {
	}

	@Override
	public void setFilter(RenderEngineFilter renderEngineFilter) throws RenderEngineException {
	}
}