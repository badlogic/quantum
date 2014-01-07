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

import java.io.Serializable;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

/**
 * a simple rgba color class. colors are given
 * in the range [0,1].
 * 
 * @author mzechner@know-center.at
 *
 */
public class Color implements Serializable
{	
	private static final long serialVersionUID = -6211798957483093166L;
	protected float r, g, b, a;
	
	public Color( )
	{
		
	}
	
	public Color( float r, float g, float b, float a )
	{
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public Color( Color c )
	{
		this.set( c );
	}
	
	public void set( Color c )
	{
		r = c.r;
		g = c.g;
		b = c.b;
		a = c.a;
	}
	
	public void set( float r, float g, float b, float a )
	{
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public void enable( GL gl )
	{
		gl.glColor4d( r, g, b, a );
	}
	
	
	public float getR( )
	{
		return r;
	}
	
	public void setR( float r )
	{
		this.r = r;
	}
	
	public float getG( )
	{
		return g;
	}
	
	public void setG( float g )
	{
		this.g = g;
	}
	
	public float getB( )
	{
		return b;
	}
	
	public void setB( float b )
	{
		this.b = b;
	}
	
	public float getA( )
	{
		return a;
	}
	
	public void setA( float a )
	{
		this.a = a;
	}
	
	public FloatBuffer toFloatBuffer( )
	{
		FloatBuffer buffer = FloatBuffer.allocate( 4 );
		buffer.put( r );
		buffer.put( g );
		buffer.put( b );
		buffer.put( a );
		buffer.flip();
		return buffer;
	}
}
