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

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.gfx.Font;
import quantum.utils.Timer;

public class TextArea extends Widget 
{
	ArrayList<String> lines = new ArrayList<String>();
	String text = "";	
	Font font;
	int scroll_pos = 0;
	int SCROLL_SIZE = 16;
	
	boolean upper_hover = false;
	boolean lower_hover = false;
	boolean uper_hold = false;
	boolean lower_hold = false;
	Timer scroll_timer = new Timer();
	
	public TextArea(Gui gui) 
	{
		super(gui);
		font = gui.getDefaultFont();
	}
	
	public void setFont( Font font )
	{
		this.font = font;
		layout();
	}
	
	public Font getFont( )
	{
		return font;
	}

	public void setText( String text )
	{
		if( text == null )
			this.text = "";
		else
			this.text = text;		
		layout();
	}
	
	private void layout( )
	{
		lines.clear( );
		
		float width = 0;
		String line = "";
		for( int i = 0; i < text.length(); i++ )
		{
			char c = text.charAt( i );
			width += font.getWidth( c );
			
			if( width > this.width - SCROLL_SIZE - 5 || c == '\n' )
			{
				lines.add( line.trim() );
				line = "";	
				line += c;
				width = font.getWidth( c );
				continue;
			}				
			
			line += c;
		}
		
		if( line.length() != 0 )
			lines.add( line.trim() );
		
		scroll_pos = Math.max( lines.size() - (int)Math.floor( height / font.getHeight()), 0 );
	}
	
	public String getText( )
	{
		return text;
	}
	

	@Override
	public boolean isFocusable() 
	{	
		return false;
	}

	@Override
	public void keyPressed(int key_code) 
	{	
		
	}

	@Override
	public void keyReleased(int key_code) 
	{	
		
	}

	@Override
	public void keyTyped(char character) 
	{	
		
	}

	@Override
	public void mouseDragged(float x, float y, int button) 
	{	
		
	}

	@Override
	public void mouseMoved(float x, float y) 
	{	
		if( internalIntersect( x, y, width - SCROLL_SIZE, 0, SCROLL_SIZE, SCROLL_SIZE ) )
			upper_hover = true;
		else
			upper_hover = false;
		
		if( internalIntersect( x, y, width - SCROLL_SIZE,  -height + SCROLL_SIZE, SCROLL_SIZE, SCROLL_SIZE ) )
			lower_hover = true;
		else
			lower_hover = false;
		
	}
	
	@Override
	public void mouseExited() 
	{	
		lower_hover = false;
		upper_hover = false;
	}

	@Override
	public void mousePressed(float x, float y, int button) 
	{	
		if( internalIntersect( x, y, width - SCROLL_SIZE, 0, SCROLL_SIZE, SCROLL_SIZE ) )
		{
			scroll_pos = Math.max( 0, scroll_pos-1 );
			scroll_timer.stop();
			scroll_timer.start();
			uper_hold = true;
		}
		if( internalIntersect( x, y, width - SCROLL_SIZE,  -height + SCROLL_SIZE, SCROLL_SIZE, SCROLL_SIZE ) )
		{
			scroll_pos = Math.min( scroll_pos+1, lines.size() - 1 );
			scroll_timer.stop();
			scroll_timer.start();
			lower_hold = true;
		}
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{
		uper_hold = false;
		lower_hold = false;
		scroll_timer.stop();
		scroll_timer.reset();
	}

	@Override
	public void render(GLCanvas canvas) 
	{	
		GL gl = canvas.getGL();
		
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x, pos.y, width, height);
		
		
		gl.glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );		
		renderOutlinedQuad( pos.x + width - SCROLL_SIZE, pos.y, SCROLL_SIZE, height );
		renderOutlinedQuad( pos.x + width - SCROLL_SIZE, pos.y, SCROLL_SIZE, SCROLL_SIZE);
		renderOutlinedQuad( pos.x + width - SCROLL_SIZE, pos.y - height + SCROLL_SIZE, SCROLL_SIZE, SCROLL_SIZE );
		
		
		if( !upper_hover )
			gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		gl.glBegin( GL.GL_TRIANGLES );
			gl.glVertex2f( pos.x + width - SCROLL_SIZE, pos.y - SCROLL_SIZE );
			gl.glVertex2f( pos.x + width, pos.y - SCROLL_SIZE);
			gl.glVertex2f( pos.x + width - SCROLL_SIZE / 2, pos.y);
		gl.glEnd();
		if( !upper_hover )
			gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
			
		if( !lower_hover )
			gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		gl.glBegin( GL.GL_TRIANGLES );
			gl.glVertex2f( pos.x + width - SCROLL_SIZE, pos.y - height + SCROLL_SIZE );
			gl.glVertex2f( pos.x + width, pos.y - height + SCROLL_SIZE );
			gl.glVertex2f( pos.x + width - SCROLL_SIZE / 2, pos.y - height );
		gl.glEnd( );
		if( !lower_hover )
			gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
		
		gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		if( scroll_pos >= 0 && scroll_pos < lines.size() )
		{
			for( int i = scroll_pos, j = 0; i < lines.size() && i < scroll_pos + (int)Math.floor( height / font.getHeight()); i++, j++ )		
				font.renderText( (int)(pos.x + 5), (int)(pos.y - j * font.getHeight() + font.getDescent()), lines.get(i) );
		}
		
		if( uper_hold )
		{
			if( scroll_timer.getElapsedSeconds() > 0.1 )
			{
				scroll_pos = Math.max( 0, scroll_pos-1 );				
				scroll_timer.stop();
				scroll_timer.start();
			}
		}
		
		if( lower_hold )
		{
			if( scroll_timer.getElapsedSeconds() > 0.1 )
			{
				scroll_pos = Math.min( scroll_pos+1, lines.size() - 1 );
				scroll_timer.stop();
				scroll_timer.start();				
			}
		}
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
