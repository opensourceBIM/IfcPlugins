package org.bimserver.noprenderengine;

import org.bimserver.geometry.Matrix;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineGeometry;
import org.bimserver.plugins.renderengine.RenderEngineInstance;

public class NopRenderEngineInstance implements RenderEngineInstance {

	@Override
	public double[] getTransformationMatrix() throws RenderEngineException {
		return Matrix.identity();
	}

	@Override
	public RenderEngineGeometry generateGeometry() throws RenderEngineException {
		return new NopRenderEngineGeometry(new int[0], new float[0], new float[0], new float[0], new int[0]);
	}

	@Override
	public double getArea() throws RenderEngineException {
		return 0;
	}

	@Override
	public double getVolume() throws RenderEngineException {
		return 0;
	}
}