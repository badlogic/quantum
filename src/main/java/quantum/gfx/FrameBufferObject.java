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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import quantum.gfx.Texture;
import quantum.utils.Log;

public class FrameBufferObject {
	int fbo = -1;
	int color = -1;
	int depth = -1;

	Texture texture;
	int width = 0;
	int height = 0;
	int old_dim[] = new int[4];

	private static int fbo_count = 0;

	public FrameBufferObject (int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		if (!gl.isExtensionAvailable("GL_EXT_framebuffer_object")) throw new Exception("Missing: GL_EXT_framebuffer_object");

		final int[] fboHandleBuffer = new int[1];		
		gl.glGenFramebuffers(1, fboHandleBuffer, 0);
		fbo = fboHandleBuffer[0];

		final int[] rboHandleBuffer = new int[1];
		gl.glGenRenderbuffers(1, rboHandleBuffer, 0);
		depth = rboHandleBuffer[0];

		final int[] texHandleBuffer = new int[1];
		gl.glGenTextures(1, texHandleBuffer, 0);
		color = texHandleBuffer[0];
		texture = new Texture(color);

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fbo);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depth);
		gl.glBindTexture(GL.GL_TEXTURE_2D, color);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

		gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
		gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depth);
		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, color, 0);

		if (gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE) {
			Log.println("Error: GL.GL_FRAMEBUFFER_EXT");
			System.exit(1);
		}

		gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		fbo_count++;
	}

	public void bind () {
		GLContext.getCurrent().getGL().glGetIntegerv(GL.GL_VIEWPORT, old_dim, 0);
		GLContext.getCurrent().getGL().glViewport(0, 0, width, height);
		GLContext.getCurrent().getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
	}

	public void unbind () {
		GLContext.getCurrent().getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		GLContext.getCurrent().getGL().glViewport(old_dim[0], old_dim[1], old_dim[2], old_dim[3]);
	}

	public void bindTexture (int unit) {
		texture.bind(unit, GL.GL_LINEAR, GL.GL_LINEAR, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
	}

	public void unbindTexture () {
		texture.unbind();
	}

	public void renderFullScreenQuad () {
		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		bindTexture(0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2f(-1f, -1f);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2f(1f, -1f);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2f(1f, 1f);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2f(-1f, 1f);
		gl.glEnd();
		unbindTexture();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	public void renderFullScreenQuadNearest () {
		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		texture.bind(0, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2f(-1f, -1f);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2f(1f, -1f);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2f(1f, 1f);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2f(-1f, 1f);
		gl.glEnd();
		unbindTexture();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	public void renderFullScreenQuad (float x, float y) {
		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		texture.bind(0, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2f(-1f + x, -1f + y);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2f(1f + x, -1f + y);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2f(1f + x, 1f + y);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2f(-1f + x, 1f + y);
		gl.glEnd();
		unbindTexture();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	public void dispose () {
		GL gl = GLContext.getCurrent().getGL();
		int array[] = new int[1];
		array[0] = fbo;
		gl.glDeleteFramebuffers(1, array, 0);
		array[0] = color;
		gl.glDeleteTextures(1, array, 0);
		if (depth != -1) {
			array[0] = depth;
			gl.glDeleteRenderbuffers(1, array, 0);
		}
		fbo_count--;
	}

	public static int getFBOCount () {
		return fbo_count;
	}
}
