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

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.sun.opengl.util.BufferUtil;

public class VertexArray 
{
	boolean has_color = false;
	boolean has_tex = false;
	
	FloatBuffer v_buf;
	FloatBuffer c_buf;
	FloatBuffer t_buf;
	int coord_size;
	int col_size;
	int tex_size;
	
	public VertexArray( int capacity, int coord_size, int col_size, int tex_size )
	{							
		v_buf = BufferUtil.newFloatBuffer( coord_size * capacity );				
		c_buf = BufferUtil.newFloatBuffer( col_size * capacity );				
		t_buf = BufferUtil.newFloatBuffer( tex_size * capacity );
			
		if( col_size > 0 )			
			this.has_color = true;
		if( tex_size > 0 )
			this.has_tex = true;
		
		this.coord_size = coord_size;
		this.col_size = col_size;
		this.tex_size = tex_size;
	}
	
	public void coord( int i, float ... val )
	{		
		i = i * coord_size;
		for( int j = 0; j < val.length; i++, j++ )
			v_buf.put( i, val[j] );
	}
	
	public void col( int i, float ... val )
	{		
		i = i * col_size;
		for( int j = 0; j < val.length; i++, j++ )
			c_buf.put( i, val[j] );
	}
	
	public void tex( int i, float ... val )
	{		
		i = i * tex_size;
		for( int j = 0; j < val.length; i++, j++ )
			t_buf.put( i, val[j] );
	}
	
	public void coordi( float ... val )
	{				
		for( int j = 0; j < val.length; j++ )
			v_buf.put( val[j] );
	}
	
	public void coli( float ... val )
	{		
		
		for( int j = 0; j < val.length; j++ )
			c_buf.put( val[j] );
	}
	
	public void texi( int i, float ... val )
	{		
		for( int j = 0; j < val.length; j++ )
			t_buf.put( val[j] );
	}
	
	public void coord( float[] val )
	{
		v_buf.rewind();
		v_buf.put( val, 0, val.length );
	}
	
	public void col( float[] val )
	{
		c_buf.rewind();
		c_buf.put( val, 0, val.length );
	}
	
	public void tex( float[] val )
	{
		t_buf.rewind();
		t_buf.put( val, 0, val.length );
	}
	
	public void render( int shape, int vertices )
	{
		GL gl = GLContext.getCurrent().getGL();
		
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		if( has_color )
			gl.glEnableClientState( GL.GL_COLOR_ARRAY );
		if( has_tex )
		{
			gl.glClientActiveTexture( GL.GL_TEXTURE0 );
			gl.glEnableClientState( GL.GL_TEXTURE_COORD_ARRAY );			
		}			
		
		v_buf.rewind();
		c_buf.rewind();
		t_buf.rewind();
		
		gl.glVertexPointer( coord_size, GL.GL_FLOAT, 0, v_buf );
		if( has_color )
			gl.glColorPointer( col_size, GL.GL_FLOAT, 0, c_buf );
		if( has_tex )
			gl.glTexCoordPointer( tex_size, GL.GL_FLOAT, 0, t_buf );
		
		gl.glDrawArrays( shape, 0, vertices );
		
		gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		if( has_color )
			gl.glDisableClientState( GL.GL_COLOR_ARRAY );
		if( has_tex )
		{
			gl.glClientActiveTexture( GL.GL_TEXTURE0 );
			gl.glDisableClientState( GL.GL_TEXTURE_COORD_ARRAY );			
		}
	}
	
	public void dipose( )
	{
		
	}
}
