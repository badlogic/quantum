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
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

public class Shader 
{
	int program = -1;
	int vertex_shader = -1;
	int fragment_shader = -1;
	
	public Shader( InputStream vertex_shader, InputStream fragment_shader ) throws Exception
	{
		String vs_string = vertex_shader == null?null:"";
		String fs_string = fragment_shader == null?null:"";
		
		if( vertex_shader != null )
		{
			BufferedReader reader = new BufferedReader( new InputStreamReader( vertex_shader ) );
			String line;
			while( (line = reader.readLine()) != null )
				vs_string += line + "\n";
			reader.close();
		}
		
		if( fragment_shader != null )
		{
			BufferedReader reader = new BufferedReader( new InputStreamReader( fragment_shader ) );
			String line;
			while( (line = reader.readLine()) != null )
				fs_string += line  + "\n";
			reader.close();
		}
		
		create( vs_string, fs_string );
	}
	
	public Shader( String vertex_shader, String fragment_shader ) throws Exception
	{			
		create( vertex_shader, fragment_shader );
	}	
	
	protected void create( String vertex_shader, String fragment_shader ) throws Exception
	{
		GL gl = GLContext.getCurrent().getGL();
		
		if (!gl.isExtensionAvailable("GL_ARB_vertex_shader") || 
			!gl.isExtensionAvailable("GL_ARB_fragment_shader") )
			throw new Exception( "glsl: not supported" );
		
		if( vertex_shader != null )
		{
			this.vertex_shader = gl.glCreateShaderObjectARB( GL.GL_VERTEX_SHADER );
			gl.glShaderSourceARB( this.vertex_shader, 1, new String[] { vertex_shader }, new int[] { vertex_shader.length() }, 0 );			
			gl.glCompileShaderARB(this.vertex_shader);
			int[] status = new int[1];
			gl.glGetObjectParameterivARB( this.vertex_shader, GL.GL_COMPILE_STATUS, status, 0 );
			if( status[0] != GL.GL_TRUE )
				throw new Exception( "glsl: error in vertex shader, " + getInfoLog( this.vertex_shader ) );
		}
		
		if( fragment_shader != null )
		{
			this.fragment_shader = gl.glCreateShaderObjectARB( GL.GL_FRAGMENT_SHADER );
			gl.glShaderSourceARB( this.fragment_shader, 1, new String[] { fragment_shader }, new int[] { fragment_shader.length() }, 0 );			
			gl.glCompileShaderARB(this.fragment_shader);
			int[] status = new int[1];
			gl.glGetObjectParameterivARB( this.fragment_shader, GL.GL_COMPILE_STATUS, status, 0 );
			if( status[0] != GL.GL_TRUE )
				throw new Exception( "glsl: error in fragment shader, " + getInfoLog( this.fragment_shader ) );
		}
		
		program = gl.glCreateProgramObjectARB( );
		if( vertex_shader != null )
			gl.glAttachObjectARB( program, this.vertex_shader );
		if( fragment_shader != null )
			gl.glAttachObjectARB( program, this.fragment_shader );
		
		gl.glLinkProgramARB( program );
		int[] status = new int[1];
		gl.glGetObjectParameterivARB( program, GL.GL_LINK_STATUS, status, 0 );
		if( status[0] != GL.GL_TRUE )
			throw new Exception( "glsl: error linking shader program, " + getInfoLog( this.program ) );
	}
	
	public String getInfoLog( int object )
	{
		String text = "";
		GL gl = GLContext.getCurrent().getGL();
				
		int len[] = new int[1];
		gl.glGetObjectParameterivARB( object, GL.GL_INFO_LOG_LENGTH, len, 0 );
		
		if( len[0] > 0 )
		{			
			byte[] bytes = new byte[len[0]];
			gl.glGetInfoLogARB( object, len[0], len, 0, bytes, 0 );
			text = new String( bytes );
		}
		
		return text;
	}
	
	public void bind( )
	{
		GLContext.getCurrent().getGL().glUseProgramObjectARB( program );
	}
	
	public void unbind( )
	{ 
		GLContext.getCurrent().getGL().glUseProgramObjectARB(0);
	}	
	
	public void dispose( )
	{
		GL gl = GLContext.getCurrent().getGL();
		if( vertex_shader != -1 )
		{
			gl.glDetachObjectARB( program, vertex_shader );
			gl.glDeleteObjectARB( vertex_shader );
		}
		if( fragment_shader != -1 )
		{
			gl.glDetachObjectARB( program, fragment_shader );
			gl.glDeleteObjectARB( fragment_shader );
		}
		
		gl.glDeleteObjectARB( program );
	}
}
