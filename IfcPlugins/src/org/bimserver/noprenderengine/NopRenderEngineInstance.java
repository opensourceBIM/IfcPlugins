package org.bimserver.noprenderengine;

import java.nio.ByteBuffer;

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

import org.bimserver.geometry.Matrix;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineGeometry;
import org.bimserver.plugins.renderengine.RenderEngineInstance;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NopRenderEngineInstance implements RenderEngineInstance {

	@Override
	public double[] getTransformationMatrix() throws RenderEngineException {
		return Matrix.identity();
	}

	@Override
	public RenderEngineGeometry generateGeometry() throws RenderEngineException {
		return new NopRenderEngineGeometry(ByteBuffer.allocate(0), ByteBuffer.allocate(0), ByteBuffer.allocate(0), ByteBuffer.allocate(0), ByteBuffer.allocate(0));
	}

	@Override
	public ObjectNode getAdditionalData() throws RenderEngineException {
		return null;
	}
}