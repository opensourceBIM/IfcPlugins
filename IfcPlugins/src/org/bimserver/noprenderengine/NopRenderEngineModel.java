package org.bimserver.noprenderengine;

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
	public RenderEngineInstance getInstanceFromExpressId(int oid) throws RenderEngineException {
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