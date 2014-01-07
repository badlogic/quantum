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

import java.awt.event.KeyEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.gfx.Font;

public class CheckBox extends Widget 
{
	private boolean checked = false;
	private String caption = "";
	private ClickedListener listener = null;
	private Font font;
	
	public CheckBox( Gui gui, String caption )
	{
		super( gui );
		this.caption = caption;
		this.font = gui.getDefaultFont();
		width = 16 + 20 + font.getWidth( caption );
	}

	public void setClickedListener( ClickedListener listener )
	{
		this.listener = listener;
	}
	
	@Override
	public boolean isFocusable() 
	{	
		return true;
	}

	@Override
	public void keyPressed(int key_code) 
	{		
		
	}

	@Override
	public void keyReleased(int key_code) 
	{
		if( key_code == KeyEvent.VK_ENTER || key_code == KeyEvent.VK_SPACE )
		{
			checked = !checked;
			if( listener != null )
				listener.clicked( this );
		}		
	}

	@Override
	public void keyTyped(char character) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(float x, float y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(float x, float y, int button) {
		if( internalIntersect( x, y, 0, 0, 16, 16 ) )
		{
			checked = !checked;
			if( listener != null )
				listener.clicked( this );
		}		
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{
	}

	@Override
	public void render(GLCanvas canvas) 
	{
		GL gl = canvas.getGL();
		
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		gl.glLineWidth( 1.5f );
		
		gl.glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
			gl.glBegin( GL.GL_LINES );
				gl.glVertex2f( pos.x, pos.y );
				gl.glVertex2f( pos.x + 16, pos.y );
				
				gl.glVertex2f( pos.x + 16, pos.y );
				gl.glVertex2f( pos.x + 16, pos.y - 16);
				
				gl.glVertex2f( pos.x + 16, pos.y - 16);
				gl.glVertex2f( pos.x, pos.y - 16);
				
				gl.glVertex2f( pos.x, pos.y - 16);
				gl.glVertex2f( pos.x, pos.y );
				
			gl.glEnd();
		
		if( checked )
		{
			gl.glBegin( GL.GL_LINES );
				gl.glVertex2f( pos.x, pos.y - 8 );
				gl.glVertex2f( pos.x + 8, pos.y - 16 );
				gl.glVertex2f( pos.x + 8, pos.y - 16);
				gl.glVertex2f( pos.x + 16, pos.y );
			gl.glEnd();
		}
		
		gl.glLineWidth( 1 );
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
				
		gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		if( caption != null )
			font.renderText( (int)(pos.x + 20), (int)(pos.y - 8 + font.getHeight() / 2), caption);
		width = 16 + 20 + font.getWidth( caption );
	}	
	
	public void setFont( Font font )
	{
		this.font = font;
		width = 16 + 20 + font.getWidth( caption );
	}
	
	public Font getFont( )
	{
		return this.font;
	}

	public boolean isChecked() 
	{	
		return checked;
	}

	public void setChecked( boolean checked )
	{
		this.checked = checked;
	}
	
	public void setCaption(String caption) 
	{
		this.caption = caption;		
		width = 16 + 20 + font.getWidth( caption );
	}

	@Override
	public void mouseExited() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
