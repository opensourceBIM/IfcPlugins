package org.bimserver.noprenderengine;

import org.bimserver.plugins.renderengine.RenderEngineGeometry;

public class NopRenderEngineGeometry extends RenderEngineGeometry {

	public NopRenderEngineGeometry(int[] indices, float[] vertices, float[] normals, float[] materials, int[] materialIndices) {
		super(indices, vertices, normals, materials, materialIndices);
	}
}
