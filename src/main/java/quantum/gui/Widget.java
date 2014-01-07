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
package quantum.gui;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;

import quantum.gfx.Color;
import quantum.math.Vector2D;

public abstract class Widget 
{
	protected Vector2D pos = new Vector2D( );
	protected float width, height;
	protected Gui gui;
	protected Color bg_col = new Color( 1, 1, 1, 1 );
	protected Color fg_col = new Color( 0, 0, 0, 1 );
	protected Color border_col = new Color( 1, 1, 1, 1 );
	private boolean has_focus;
	private int z_order = 0;
	private boolean visible = true;
	
	public Widget( Gui gui )
	{
		this.gui = gui;
		bg_col.set( gui.getDefaultBackgroundColor( ) );
		fg_col.set( gui.getDefaultForegroundColor( ) );
		border_col.set( gui.getDefaultBorderColor() );
	}

	public void setVisible( boolean visible )
	{
		this.visible = visible;
	}
	
	public boolean isVisible( )
	{
		return this.visible;
	}
	
	public void setZOrder( int z_order )
	{
		this.z_order = z_order;
	}
	
	public int getZOrder( )
	{
		return z_order;
	}
	
	public Color getBorderColor( )
	{
		return border_col;
	}
	
	public void setBorderColor( float r, float g, float b, float a )
	{
		border_col.set(r, g, b, a);
	}
	
	public void setBorderColor( Color col )
	{
		border_col.set( col );
	}
	
	public void setBackgroundColor( float r, float g, float b, float a )
	{
		bg_col.set(r, g, b, a);
	}
	
	public Color getBackgroundColor( )
	{
		return bg_col;
	}
	
	public void setBackgroundColor( Color col )
	{
		bg_col.set( col );
	}
	
	public void setForegroundColor( float r, float g, float b, float a)
	{
		fg_col.set(r, g, b, a);
	}
	
	public Color getForegroundColor( )
	{
		return fg_col;
	}
	
	public void setForegroundColor( Color col )
	{
		fg_col.set( col );
	}
	
	public void setPosition( float x, float y )
	{
		pos.set(x, y);
	}
	
	public Vector2D getPosition( )
	{
		return pos;
	}
	
	public void setX( float x )
	{
		pos.x = x;
	}
	
	public float getX( )
	{
		return pos.x;
	}
	
	public void setY( float y )
	{
		pos.y = y;
	}
	
	public float getY( )
	{
		return pos.y;
	}
	
	public float getWidth( )
	{
		return width;
	}
	
	public float getHeight( )
	{
		return height;
	}
	
	public void setWidth( float width )
	{
		this.width = width;				
	}
	
	public void setHeight( float height )
	{
		this.height = height;
	}
	
	public void setSize( float width, float height )
	{
		this.width = width;
		this.height = height;
	}	
	
	public abstract void render( GLCanvas canvas );		
	
	
	public static void renderQuad( float x, float y, float width, float height )
	{
		GL gl = GLContext.getCurrent().getGL();			
		gl.glBegin( GL.GL_QUADS );
			gl.glVertex2f( x, y );
			gl.glVertex2f( x + width, y );
			gl.glVertex2f( x + width, y - height );
			gl.glVertex2f( x, y - height );
		gl.glEnd( );
	}
	
	public static void renderOutlinedQuad( float x, float y, float width, float height )
	{
		GL gl = GLContext.getCurrent().getGL();
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		gl.glBegin( GL.GL_LINES );
		gl.glVertex2f( x, y );
		gl.glVertex2f( x + width, y );
		gl.glVertex2f( x + width, y );
		gl.glVertex2f( x + width, y - height);
		
		gl.glVertex2f( x + width, y - height);
		gl.glVertex2f( x, y - height);
		
		gl.glVertex2f( x, y - height);
		gl.glVertex2f( x, y );
		
		gl.glEnd();
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
	}
	
	public static void renderTexturedQuad( float x, float y, float width, float height )
	{
		GL gl = GLContext.getCurrent().getGL();
		gl.glBegin( GL.GL_QUADS );
			gl.glTexCoord2f( 0, 0 );
			gl.glVertex2f( x, y );
			gl.glTexCoord2f( 1, 0 );
			gl.glVertex2f( x + width, y );
			gl.glTexCoord2f( 1, 1 );
			gl.glVertex2f( x + width, y - height );
			gl.glTexCoord2f( 0, 1 );
			gl.glVertex2f( x, y - height );
		gl.glEnd( );		
	}
	
	public boolean intersects( float x, float y )
	{
		return pos.x <= x && pos.x + width >= x && pos.y > y && pos.y - height <= y;
	}
	
	public boolean internalIntersect( float x, float y )
	{
		return 0 <= x && width >= x && 0 > y && -height <= y;
	}
	
	public boolean internalIntersect( float x, float y, float x_w, float y_w, float w, float h )
	{
		return x_w <= x && x_w + w >= x && y_w > y && y_w - h < y;
	}
	
	public abstract void mouseMoved( float x, float y );	
	public abstract void mousePressed( float x, float y, int button );
	public abstract void mouseReleased( float x, float y, int button );
	public abstract void mouseDragged( float x, float y, int button );
	public abstract void mouseExited( );
	public abstract void keyPressed( int key_code );
	public abstract void keyReleased( int key_code );
	public abstract void keyTyped( char character );

	public abstract boolean isFocusable();
	public abstract void dispose( );
	
	
	public void setFocus( boolean focus )
	{
		has_focus = focus;
	}
	public boolean hasFocus( )
	{
		return has_focus;
	}
}
