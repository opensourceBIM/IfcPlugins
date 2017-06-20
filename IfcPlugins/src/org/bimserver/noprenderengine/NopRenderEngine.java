package org.bimserver.noprenderengine;

import java.io.InputStream;

import org.bimserver.plugins.renderengine.RenderEngine;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineModel;

public class NopRenderEngine implements RenderEngine {

	@Override
	public void init() throws RenderEngineException {
	}

	@Override
	public RenderEngineModel openModel(InputStream inputStream, long size) throws RenderEngineException {
		return new NopRenderEngineModel();
	}

	@Override
	public RenderEngineModel openModel(InputStream inputStream) throws RenderEngineException {
		return new NopRenderEngineModel();
	}

	@Override
	public void close() throws RenderEngineException {
	}
}
