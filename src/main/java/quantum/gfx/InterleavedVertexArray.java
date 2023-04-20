//
// Copyright (c) 2009 Mario Zechner.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the GNU Lesser Public License v2.1
// which accompanies this distribution, and is available at
// http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
// 
// Contributors:
//     Mario Zechner - initial API and implementation
//

package quantum.gfx;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

import com.jogamp.common.nio.Buffers;

public class InterleavedVertexArray {
	boolean has_tex = false;

	FloatBuffer buf;
	int coord_size;
	int col_size;
	int nor_size;
	int tex_size;

	int format;

	public InterleavedVertexArray (int capacity, int format) {
		switch (format) {
		case GL2.GL_V2F:
			coord_size = 2;
			break;
		case GL2.GL_V3F:
			coord_size = 3;
			break;
		case GL2.GL_C3F_V3F:
			coord_size = 3;
			col_size = 3;
			break;
		case GL2.GL_N3F_V3F:
			coord_size = 3;
			nor_size = 3;
			break;
		case GL2.GL_C4F_N3F_V3F:
			coord_size = 3;
			col_size = 4;
			nor_size = 3;
			break;
		case GL2.GL_T2F_V3F:
			coord_size = 3;
			tex_size = 2;
			break;
		case GL2.GL_T4F_V4F:
			coord_size = 4;
			tex_size = 4;
			break;
		case GL2.GL_T2F_C3F_V3F:
			coord_size = 3;
			col_size = 3;
			tex_size = 2;
			break;
		case GL2.GL_T2F_N3F_V3F:
			coord_size = 3;
			nor_size = 3;
			tex_size = 2;
			break;
		case GL2.GL_T2F_C4F_N3F_V3F:
			coord_size = 3;
			nor_size = 3;
			col_size = 4;
			tex_size = 2;
			break;
		case GL2.GL_T4F_C4F_N3F_V4F:
			coord_size = 4;
			nor_size = 3;
			col_size = 4;
			tex_size = 4;
			break;
		default:
			throw new RuntimeException("unsupported vertex format");
		}

		buf = Buffers.newDirectFloatBuffer((coord_size + col_size + tex_size + nor_size) * capacity);

		this.format = format;
		if (tex_size > 0) has_tex = true;
	}

	public void render (int shape, int vertices) {
		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		buf.rewind();
		gl.glInterleavedArrays(format, 0, buf);
		gl.glDrawArrays(shape, 0, vertices);
	}

	public void dipose () {

	}

	public void rewind () {
		buf.rewind();
	}

	public void put (float... val) {
		for (float v : val)
			buf.put(v);
	}
}
