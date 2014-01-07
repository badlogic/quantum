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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.sun.opengl.util.BufferUtil;

import quantum.math.Bounds;
import quantum.math.Matrix;
import quantum.math.Vector;

/**
 * simple mesh class allowing gl style immediate
 * mode commands and concatenating that to a
 * display list. the triangle list is preserverd
 * for intersection testing. modifications
 * after compiling the mesh will result in an
 * exception
 * 
 * @author mzechner@know-center.at
 *
 */
public class Mesh
{			
	public enum Type
	{
		DISPLAY_LIST,
		VERTEX_ARRAY
	}
	
	protected FloatBuffer va_coords = null;
	protected FloatBuffer va_cols = null;
	protected FloatBuffer va_nors = null;
	protected FloatBuffer[] va_texs = null;
	
	protected List<Vector> coords = new ArrayList<Vector>();
	protected List<Color> cols = new ArrayList<Color>();
	protected List<Vector> nors = new ArrayList<Vector>();
	protected List<Vector> texs = new ArrayList<Vector>();	
	protected Bounds bounds = new Bounds();
	protected int max_tex_units = 0;	
	protected boolean has_normals = false;
	protected boolean has_colors = false;
	protected boolean is_compiled = false;	
	protected Type type = Type.DISPLAY_LIST;
	protected int gl_id = -1;
	
	/**
	 * creates a new empty mesh. tex_units specifies
	 * how many texture coordinates are to be expected
	 * per vertex, has_normals specifies wheter each
	 * vertex has a normal and has_colors specifies
	 * wheter each vertex has a color. when creating
	 * the mesh geometry this values must fit the actual
	 * specified features ( e.g. tex_units = 2 -> you
	 * have to specifiy 2 texture coordinate pairs per
	 * vertex ). the type specifies wheter the added
	 * vertices are a triangle or a quad list.
	 * 
	 * @param tex_units
	 * @param has_normals
	 * @param has_colors
	 */
	public Mesh( int tex_units, boolean has_normals, boolean has_colors )
	{
		this.max_tex_units = tex_units;
		this.has_normals = has_normals;
		this.has_colors = has_colors;
		this.type = Type.DISPLAY_LIST;
	}	
	
	public Mesh( Type type, int tex_units, boolean has_normals, boolean has_colors )
	{
		this.max_tex_units = tex_units;
		this.has_normals = has_normals;
		this.has_colors = has_colors;
		this.type = type;
	}	
	
	/**
	 * adds a normal to this mesh
	 * @param nor
	 */
	public void nor( Vector nor )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		nors.add( nor.cpy() );
	}
	
	/**
	 * adds a normal to this mesh
	 * @param x
	 * @param y
	 * @param z
	 */
	public void nor( float x, float y, float z )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		nor( new Vector( x, y, z ) );
	}	
	
	/** adds a texture coordinate to this mesh.
	 * tex.getX() specifies the texture unit,
	 * tex.getY() specifies the s coordinate
	 * tex.getZ() specifies the t coordinate
	 * @param tex
	 */
	public void tex( Vector tex )
	{					
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		if( type == Type.DISPLAY_LIST )
			tex.setX( GL.GL_TEXTURE0 + (int)tex.getX() );		
		texs.add( tex.cpy() );
	}
	
	/**
	 * adds a texture coordinate to this mesh
	 * @param unit
	 * @param u
	 * @param v
	 */
	public void tex( int unit, float u, float v )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		tex( new Vector( unit, u, v ) );
	}
	
	/**
	 * adds a color to this mesh
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public void col( float r, float g, float b, float a )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		cols.add( new Color( r, g, b, a ) );
	}		
	
	/**
	 * adds a vertex to this mesh
	 * @param coord
	 */
	public void coord( Vector coord )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		bounds.ext( coord );
		coords.add( coord.cpy() );			
	}
	
	/**
	 * adds a vertex to this mesh
	 * @param x
	 * @param y
	 * @param z
	 */
	public void coord( float x, float y, float z )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		coord( new Vector( x, y, z ) );
	}	
	
	/**
	 * returns the bounds of this mesh
	 * @return
	 */
	public Bounds getBounds( )
	{
		return bounds;
	}
		
	/**
	 * centers the vertices of this mesh based on the
	 * bounding box of the vertices added so far.
	 * recalculates the bounding box afterwards#
	 * only has an effect before compile is invoked
	 */
	public void center( )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		Vector center = bounds.getCenter().cpy();
		bounds = new Bounds();
		bounds.inf();
		for( Vector v: coords )		
		{
			v.sub( center );
			bounds.ext(v);			
		}
	}
	
	/**
	 * centers the vertices of this mesh based on the
	 * bounding box of the vertices added so far and clips
	 * it to integer values.
	 * recalculates the bounding box afterwards#
	 * only has an effect before compile is invoked
	 */
	public void centerInteger( )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		Vector center = bounds.getCenter().cpy();
		center.setX( (int)center.getX() );
		center.setY( (int)center.getY() );
		center.setZ( (int)center.getZ() );
		bounds = new Bounds();
		bounds.inf();
		for( Vector v: coords )		
		{
			v.sub( center );
			bounds.ext(v);			
		}
	}
	
	/**
	 * normalizes the vertices of this mesh to
	 * fit in a unit cube with bounding box ( -1, -1, -1 )-(1, 1, 1 )
	 * recalculates the boundingbox afterwards
	 * only has an effect before compile is invoked
	 */
	public void normalize( )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		float x = Math.max( Math.abs(bounds.getMin().getX()), Math.abs(bounds.getMax().getX()) );
		float y = Math.max( Math.abs(bounds.getMin().getY()), Math.abs(bounds.getMax().getY()) );
		float z = Math.max( Math.abs(bounds.getMin().getZ()), Math.abs(bounds.getMax().getZ()) );
		
		if( x != 0 )
			x = 1 / x;
		if( y != 0 )
			y = 1 / y;
		if( z != 0 )
			z = 1 / z;
		
		bounds = new Bounds();
		bounds.inf();
		for( Vector v: coords )
		{
			v.setX( v.getX() * x );
			v.setY( v.getY() * y );
			v.setZ( v.getZ() * z );
			bounds.ext( v );
		}
	}
	
	/**
	 * translates all vertices by the given matrix
	 * @param matrix
	 */
	public void transform( Matrix matrix )
	{
		if( is_compiled )
			throw new RuntimeException( "modification of mesh after compilation not allowed" );
		bounds = new Bounds();
		bounds.inf();
		for( Vector v: coords )
		{
			v.mul( matrix );
			bounds.ext( v );
		}
	}
	
	/**
	 * compiles the mesh to a display list and removes
	 * any added texture coordinates, normals, colors
	 * and vertices. the bounds reflect the last bounds
	 * calculated from the added vertices. the mesh
	 * can be rendered via the render method after compilation
	 */
	public void compile( )
	{
		if( is_compiled )
			return;	
		
		if( coords.size() % 3 != 0 )
			throw new RuntimeException( "wrong number of vertices, should be a multiple of 3 ( only triangle lists supported for now )" );
		
		if( max_tex_units != 0 )
			if( texs.size() / max_tex_units != coords.size() )
				throw new RuntimeException( "wrong number of texture coordinates per vertex, should be " + max_tex_units * coords.size() + " is " + texs.size() );
		
		if( has_normals )
			if( nors.size() != coords.size() )
				throw new RuntimeException( "wrong number of normals, should be " + coords.size() + " is " + nors.size() );
		
		if( has_colors )
			if( cols.size() != coords.size() )
				throw new RuntimeException( "wrong number of colors, should be " + coords.size() + " is " + cols.size() );
		
		if( type == Type.DISPLAY_LIST )
			compileDisplayList( );
				
		if( type == Type.VERTEX_ARRAY )
			compileVertexArray( );
		
		is_compiled = true;
	}
	
    private FloatBuffer getBuffer(int a_floats)
    {
        return BufferUtil.newFloatBuffer( a_floats );
    }
	
	protected void compileVertexArray( )
	{
		va_coords = getBuffer( coords.size() * 3 );
		for( int i = 0; i < coords.size(); i++ )
		{
			va_coords.put( coords.get(i).getX() );
			va_coords.put( coords.get(i).getY() );
			va_coords.put( coords.get(i).getZ() );
		}
		
		if( has_normals )
		{
			va_nors = getBuffer( nors.size() * 3 );
			for( int i = 0; i < nors.size(); i++ )
			{
				va_nors.put( nors.get(i).getX() );
				va_nors.put( nors.get(i).getY() );
				va_nors.put( nors.get(i).getZ() );
			}
		}
		
		if( has_colors )
		{
			va_cols = getBuffer( cols.size() * 4 );
			for( int i = 0; i < cols.size(); i++ )
			{
				va_cols.put( cols.get(i).getR() );
				va_cols.put( cols.get(i).getG() );
				va_cols.put( cols.get(i).getB() );
				va_cols.put( cols.get(i).getA() );
			}
		}
		if( max_tex_units != 0 )
		{
			va_texs = new FloatBuffer[max_tex_units];
			for( int i = 0; i < max_tex_units; i++ )
			{
				va_texs[i] = getBuffer( coords.size() * 2 );				
			}				
			
			for( int i = 0; i < texs.size(); i++ )
			{				
				va_texs[(int)texs.get(i).getX()].put( texs.get(i).getY() );
				va_texs[(int)texs.get(i).getX()].put( texs.get(i).getZ() );			
			}
		}	
		
		cols = null;
		nors = null;
		texs = null;		
	}
	
	protected void compileDisplayList( )
	{
		GL gl = GLContext.getCurrent().getGL();
		gl_id = gl.glGenLists( 1 );
		gl.glNewList( gl_id, GL.GL_COMPILE );
		gl.glBegin( GL.GL_TRIANGLES );

		
			if( !has_normals && !has_colors && max_tex_units == 0 )
				compileCoord( gl );
			else
			if( !has_normals && has_colors && max_tex_units == 0 )
				compileCoordCol( gl );
			else
			if( has_normals && !has_colors && max_tex_units == 0 )
				compileCoordNor( gl );
			else
			if( has_normals && has_colors && max_tex_units == 0 )
				compileCoordColNor( gl );
			else
			if( !has_normals && !has_colors && max_tex_units != 0 )
				compileCoordTex( gl );
			else
			if( has_normals && !has_colors && max_tex_units != 0 )
				compileCoordNorTex( gl );
			else
			if( !has_normals && has_colors && max_tex_units != 0 )
				compileCoordColTex( gl );
			else
			if( has_normals && has_colors && max_tex_units != 0 )
				compileCoordColNorTex( gl );
			else
				throw new RuntimeException( "couldn't compile format!" );
		
		gl.glEnd();		
		gl.glEndList();
				
		cols = null;
		nors = null;
		texs = null;
	}
	
	protected void compileCoord( GL gl )
	{
		for( Vector c: coords )
			gl.glVertex3d( c.getX(), c.getY(), c.getZ() );
	}
	
	protected void compileCoordCol( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			Color col = cols.get( i );
			Vector coord = coords.get( i );
			gl.glColor4d( col.getR(), col.getG(), col.getB(), col.getA() );
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}
	}
	
	protected void compileCoordNor( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			Vector nor = nors.get( i );
			Vector coord = coords.get( i );			
			gl.glNormal3d( nor.getX(), nor.getY(), nor.getZ() );
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}		
	}
	
	protected void compileCoordColNor( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			Color col = cols.get( i );	
			Vector nor = nors.get( i );
			Vector coord = coords.get( i );
			gl.glColor4d( col.getR(), col.getG(), col.getB(), col.getA() );
			gl.glNormal3d( nor.getX(), nor.getY(), nor.getZ() );			
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}
	}
	
	protected void compileCoordTex( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			for( int j = 0; j < max_tex_units; j++ )
			{
				Vector tex = texs.get(i * max_tex_units + j );				
				gl.glMultiTexCoord2d( (int)tex.getX(), tex.getY(), tex.getZ() );
			}
			Vector coord = coords.get( i );
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}		
	}
	
	protected void compileCoordColTex( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			for( int j = 0; j < max_tex_units; j++ )
			{
				Vector tex = texs.get(i * max_tex_units + j );				
				gl.glMultiTexCoord2d( (int)tex.getX(), tex.getY(), tex.getZ() );
			}
			Color col = cols.get( i );	
			Vector coord = coords.get( i );
			gl.glColor4d( col.getR(), col.getG(), col.getB(), col.getA() );
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}		
	}
	
	protected void compileCoordNorTex( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			for( int j = 0; j < max_tex_units; j++ )
			{
				Vector tex = texs.get(i * max_tex_units + j );					
				gl.glMultiTexCoord2d( (int)tex.getX(), tex.getY(), tex.getZ() );
			}
			Vector nor = nors.get( i );
			Vector coord = coords.get( i );
			gl.glNormal3d( nor.getX(), nor.getY(), nor.getZ() );
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}				
	}
	
	protected void compileCoordColNorTex( GL gl )
	{
		for( int i = 0; i < coords.size(); i++ )
		{
			for( int j = 0; j < max_tex_units; j++ )
			{
				Vector tex = texs.get(i * max_tex_units + j );				
				gl.glMultiTexCoord2d( (int)tex.getX(), tex.getY(), tex.getZ() );
			}
			Color col = cols.get( i );	
			Vector nor = nors.get( i );
			Vector coord = coords.get( i );
			gl.glColor4d( col.getR(), col.getG(), col.getB(), col.getA() );
			gl.glNormal3d( nor.getX(), nor.getY(), nor.getZ() );			
			gl.glVertex3d( coord.getX(), coord.getY(), coord.getZ() );
		}	
	}
	
	/**
	 * returns wheter this mesh is compiled or
	 * not
	 * @return
	 */
	public boolean isCompiled( )
	{
		return is_compiled;
	}
	
	/**
	 * renders this mesh, no states are set
	 */
	public void render( )
	{		
		if( type == Type.DISPLAY_LIST )
			GLContext.getCurrent().getGL().glCallList( gl_id );
		if( type == Type.VERTEX_ARRAY )
			renderVertexArray( );
	}
	
	protected void renderVertexArray( )
	{
		GL gl = GLContext.getCurrent().getGL();
		
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		va_coords.rewind();
		gl.glVertexPointer( 3, GL.GL_FLOAT, 0, va_coords );
		
		if( has_colors )
		{
			gl.glEnableClientState( GL.GL_COLOR_ARRAY );
			va_cols.rewind();
			gl.glColorPointer( 4, GL.GL_FLOAT, 0, va_cols );
		}
		
		if( has_normals )
		{
			gl.glEnableClientState( GL.GL_NORMAL_ARRAY );
			va_nors.rewind();
			gl.glNormalPointer( GL.GL_FLOAT, 0, va_nors );
		}
		
		if( max_tex_units != 0 )
		{
			for( int i = 0; i < max_tex_units; i++ )
			{
				gl.glClientActiveTexture( GL.GL_TEXTURE0 + i );
				gl.glEnableClientState( GL.GL_TEXTURE_COORD_ARRAY );
				va_texs[i].rewind();
				gl.glTexCoordPointer( 2, GL.GL_FLOAT, 0, va_texs[i] );
			}
		}
		
		gl.glDrawArrays( GL.GL_TRIANGLES, 0, coords.size() );
		
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);               
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        
        if( max_tex_units != 0 )
        {
        	for( int i = 0; i < max_tex_units; i++ )
			{
				gl.glClientActiveTexture( GL.GL_TEXTURE0 + i );
				gl.glDisableClientState( GL.GL_TEXTURE_COORD_ARRAY );				
			}
        }
	}
	
	/**
	 * pushes the modelview matrix, sets 
	 * the position, renders the mesh
	 * and pops the modelview matrix again
	 * @param position
	 */
	public void render( Vector position )
	{		
		GL gl = GLContext.getCurrent().getGL();
		gl.glPushMatrix();
		gl.glTranslated( position.getX(), position.getY(), position.getZ() );
		this.render();
		gl.glPopMatrix();
	}
	
	/**
	 * pushes the modelview matrix, sets 
	 * the position and rotation, renders the mesh
	 * and pops the modelview matrix again
	 * @param position
	 */
	public void render( Vector position, Vector axis, float angle )
	{		
		GL gl = GLContext.getCurrent().getGL();
		gl.glPushMatrix();
		gl.glTranslated( position.getX(), position.getY(), position.getZ() );
		gl.glRotated( angle, axis.getX(), axis.getY(), axis.getZ() );
		this.render();
		gl.glPopMatrix();
	}
	
	/**
	 * pushes the modelview matrix, multiplies
	 * it with the given matrix, renders the mesh
	 * and pops the modelview matrix again
	 * @param matrix
	 */
	public void render( Matrix matrix )
	{
		GL gl = GLContext.getCurrent().getGL();
		gl.glPushMatrix();
		gl.glMultMatrixf(matrix.toFloatBuffer());
		this.render();
		gl.glPopMatrix();
	}
	
	/**
	 * wrapper around glBegin( GL_TRIANGLES )
	 */
	public static void beginTriangles( )
	{		
		GLContext.getCurrent().getGL().glBegin( GL.GL_TRIANGLES );
	}
	
	/**
	 * wrapper around glBegin( GL_QUADS )
	 */
	public static void beginQuads( )
	{
		GLContext.getCurrent().getGL().glBegin( GL.GL_QUADS );
	}
	
	/**
	 * wrapper around glBegin( GL_LINES )
	 */
	public static void beginLines()
	{
		GLContext.getCurrent().getGL().glBegin( GL.GL_LINES );
		
	}
	
	/**
	 * wrapper around glBegin( GL_LINE_STRIP )
	 */
	public static void beginLineStrip()
	{
		GLContext.getCurrent().getGL().glBegin( GL.GL_LINE_STRIP );
		
	}
	
	/**
	 * wrapper around glEnd()
	 */
	public static void end( )
	{
		GLContext.getCurrent().getGL().glEnd();
	}
		
	
	public void dispose( )
	{		
 		GLContext ctx = GLContext.getCurrent();
		if( ctx == null )
			return;
		GL gl = ctx.getGL();
		gl.glDeleteLists( gl_id, 1);				
	}		
	
	/**
	 * @return the triangles of this mesh in form
	 * of a triangle list composed of vectors
	 */
	public List<Vector> getTriangles( )
	{
		return coords;
	}
	
	public static Mesh loadFromObj( InputStream in ) 
	{
		List<Vector> coords = new ArrayList<Vector>();
		List<Vector> nors = new ArrayList<Vector>();
		List<Vector> texs = new ArrayList<Vector>();
		Mesh m = null;
		
		boolean has_texcoords = false;
		boolean has_normals = false;

		BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
		String line = null;
		try
		{
			while( (line=reader.readLine()) != null )
			{
				line = line.trim().toLowerCase();
				String tokens[] = line.split( "\\s+" );
				
				if( tokens[0].trim().equals( "v" ) )			
					coords.add( new Vector( Float.parseFloat(tokens[1]),
							 				Float.parseFloat(tokens[2]),
							 				Float.parseFloat(tokens[3])) );
				
				if( tokens[0].trim().equals( "vn" ) )
				{
					has_normals = true;
					nors.add( new Vector( Float.parseFloat(tokens[1]),
							 			  Float.parseFloat(tokens[2]),
							 			  Float.parseFloat(tokens[3])).nor() );
				}
				
				if( tokens[0].trim().equals( "vt" ) )
				{
					has_texcoords = true;
					texs.add( new Vector( 0,
										  Float.parseFloat(tokens[1]),
				 			  			  Float.parseFloat(tokens[2])
				 			  			  ) );
				}
				
				if( tokens[0].trim().equals( "f" ) )
				{
					if( tokens.length > 4 )
						throw new Exception("faces must be triangles" );
					
					if( m == null )
					{
						m = new Mesh( Mesh.Type.VERTEX_ARRAY, has_texcoords?1:0, has_normals, false );
						m.getBounds().inf();
					}
					
					String[] idx_1 = tokens[1].split("/" );
					String[] idx_2 = tokens[2].split("/" );
					String[] idx_3 = tokens[3].split("/" );												
									
					//
					// vertex #1
					// 
					int index = 0;
					if( has_normals )
					{
						index = Integer.parseInt(idx_1[2]);
						if( index < 0 )
							index = nors.size() + index;
						else
							index = index - 1;
						m.nor(nors.get( index ));
					}
					if( has_texcoords )
					{
						if( !idx_1[1].equals( "" ) )
						{
							index = Integer.parseInt(idx_1[1]);						
							if( index < 0 )
								index = texs.size() + index;
							else
								index = index - 1;
							m.tex(texs.get( index ));
						}
						else
							m.tex( 0, 0, 0 );
					}
					index = Integer.parseInt(idx_1[0]);
					if( index < 0 )
						index = coords.size() + index;
					else
						index = index - 1;
					m.coord( coords.get( index ) );
					
					//
					// vertex #2
					//
					if( has_normals )
					{
						index = Integer.parseInt(idx_2[2]);
						if( index < 0 )
							index = nors.size() + index;
						else
							index = index - 1;
						m.nor(nors.get( index ));
					}
					if( has_texcoords )
					{
						if( !idx_2[1].equals( "" ) )
						{
							index = Integer.parseInt(idx_2[1]);						
							if( index < 0 )
								index = texs.size() + index;
							else
								index = index - 1;
							m.tex(texs.get( index ));
						}
						else
							m.tex( 0, 0, 0 );
					}
					index = Integer.parseInt(idx_2[0]);
					if( index < 0 )
						index = coords.size() + index;
					else
						index = index - 1;
					m.coord( coords.get( index ) );
					
					//
					// vertex #3
					//
					if( has_normals )
					{
						index = Integer.parseInt(idx_3[2]);
						if( index < 0 )
							index = nors.size() + index;
						else
							index = index - 1;
						m.nor(nors.get( index ));
					}
					if( has_texcoords )
					{
						if( !idx_3[1].equals( "" ) )
						{
							index = Integer.parseInt(idx_3[1]);						
							if( index < 0 )
								index = texs.size() + index;
							else
								index = index - 1;
							m.tex(texs.get( index ));
						}
						else
							m.tex( 0, 0, 0 );
					}
					index = Integer.parseInt(idx_3[0]);
					if( index < 0 )
						index = coords.size() + index;
					else
						index = index - 1;
					m.coord( coords.get( index ) );
				}
			}
			
			reader.close();	
		}
		catch( Exception ex )
		{
			try
			{
				reader.close();
			} catch (IOException e)
			{
				throw new RuntimeException( "couldn't load mesh" );
			}
			ex.printStackTrace();
			throw new RuntimeException( "couldn't load mesh, " + ex.getMessage() );
		}
				
		return m;
	}
	
	public void coordva( int idx, float x, float y, float z )
	{
		idx *= 3;
		va_coords.put( idx, x );
		va_coords.put( idx + 1, y );
		va_coords.put( idx + 2, z );
	}
	
	public void norva( int idx, float x, float y, float z )
	{
		idx *= 3;
		va_nors.put( idx, x );
		va_nors.put( idx + 1, y );
		va_nors.put( idx + 1, z );
	}
	
	public void colva( int idx, float r, float g, float b, float a )
	{
		idx *= 4;
		va_cols.put( idx, r );
		va_cols.put( idx, g );
		va_cols.put( idx, b );
		va_cols.put( idx, a );	
	}
	
	public void texva( int idx, int unit, float s, float t )
	{
		idx *= 2;
		va_texs[unit].put( idx, s );
		va_texs[unit].put( idx + 1, t );
	}
	
	/**
	 * wrapper around glNormal3f
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public static void nori( float x, float y, float z )
	{
		GLContext.getCurrent().getGL().glNormal3d( x, y, z );
	}	
	
	/**
	 * wrapper around glMultiTexCoord2fARB
	 * @param unit
	 * @param x
	 * @param y
	 */	
	public static void texi( int unit, float x, float y )
	{
		GLContext.getCurrent().getGL().glMultiTexCoord2d( GL.GL_TEXTURE0 + unit, x, y);
	}
	
	/**
	 * wrapper around glColor4f
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public static void coli( float r, float g, float b, float a )
	{
		GLContext.getCurrent().getGL().glColor4d(r,g,b,a);
	}
	
	/**
	 * wrapper around glVertex3f
	 * @param x
	 * @param y
	 * @param z
	 */
	public static void coordi( float x, float y, float z )
	{		
		GLContext.getCurrent().getGL().glVertex3d( x, y, z );
	}
	
	/**
	 * wrapper around glVertex2f
	 * @param x
	 * @param y 
	 */
	public static void coordi( float x, float y )
	{
		GLContext.getCurrent().getGL().glVertex2d( x, y);
	}
	
	/**
	 * wrapper around glVertex3f
	 */
	public static void coordi( Vector coords )
	{
		coordi( coords.getX(), coords.getY(), coords.getZ() );
	}
	
	/**
	 * renders the bounds of this mesh
	 */
	public void renderBounds( )
	{			
		renderBounds( bounds );
	}
	
	public static void renderBounds( Bounds bounds )
	{
		Vector min = bounds.getMin();
		Vector max = bounds.getMax();
				
		beginLineStrip();
			coordi( min.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), min.getY(), max.getZ() );
			coordi( min.getX(), min.getY(), max.getZ() );
			coordi( min.getX(), min.getY(), min.getZ() );
			coordi( min.getX(), max.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), min.getZ() );						
		end();
		
		beginLines();
			coordi( max.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), min.getZ() );
			
			coordi( max.getX(), min.getY(), max.getZ() );
			coordi( max.getX(), max.getY(), max.getZ() );
			
			coordi( min.getX(), min.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), max.getZ() );
		end();
	}
	
	public static void renderBoundsSolid( Bounds bounds )
	{
		Vector min = bounds.getMin();
		Vector max = bounds.getMax();					
		
		beginQuads();
			coordi( min.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), min.getY(), max.getZ() );
			coordi( min.getX(), min.getY(), max.getZ() );
			
			coordi( min.getX(), max.getY(), min.getZ() );
			coordi( min.getX(), max.getY(), max.getZ() );
			coordi( max.getX(), max.getY(), max.getZ() );
			coordi( max.getX(), max.getY(), min.getZ() );
			
			coordi( min.getX(), min.getY(), max.getZ() );
			coordi( max.getX(), min.getY(), max.getZ() );
			coordi( max.getX(), max.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), max.getZ() );
			
			coordi( min.getX(), min.getY(), min.getZ() );
			coordi( min.getX(), max.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), min.getZ() );
			coordi( max.getX(), min.getY(), min.getZ() );
			
			coordi( min.getX(), min.getY(), min.getZ() );
			coordi( min.getX(), min.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), max.getZ() );
			coordi( min.getX(), max.getY(), min.getZ() );
			
			coordi( max.getX(), min.getY(), max.getZ() );
			coordi( max.getX(), min.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), min.getZ() );
			coordi( max.getX(), max.getY(), max.getZ() );
			
		end();			
	}
	
	/**
	 * helper method for drawing a rectangle. does not
	 * specifiy texture coordinates or a color. uses
	 * the color specified last via the gl.
	 * @param gl
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public static void renderRectangle( GL gl, float x, float y, float width, float height )
	{		
		gl.glBegin( GL.GL_QUADS );
			gl.glVertex2f( x, y );
			gl.glVertex2f( x + width, y );
			gl.glVertex2f( x + width, y + height );
			gl.glVertex2f( x, y + height );
		gl.glEnd();
	}
	
	public static void renderRectangleTextured( GL gl, float x, float y, float width, float height )
	{
		gl.glBegin( GL.GL_QUADS );
			gl.glTexCoord2f( 0, 1 );
			gl.glVertex2f( x, y );
			gl.glTexCoord2f( 1, 1 );
			gl.glVertex2f( x + width, y );
			gl.glTexCoord2f( 1, 0 );
			gl.glVertex2f( x + width, y + height );
			gl.glTexCoord2f( 0, 0 );
			gl.glVertex2f( x, y + height );
		gl.glEnd();
	}
	
	public static void renderLine( GL gl, float x, float y, float x2, float y2 )
	{
		gl.glBegin( GL.GL_LINES );
			gl.glVertex2f( x, y );
			gl.glVertex2f( x2, y2 );
		gl.glEnd();
	}
}
