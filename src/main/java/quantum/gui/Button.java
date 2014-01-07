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

import javax.media.opengl.GLCanvas;

import quantum.gfx.Font;

public class Button extends Widget
{	
	String caption = "";
	Font font;
	boolean pressed = false;
	
	ClickedListener listener = null;
	
	public Button(Gui gui, String caption ) 
	{
		super(gui);
		this.caption = caption;
		font = gui.getDefaultFont();		
	}

	public void setCaption( String caption )
	{
		this.caption = caption;
	}
	
	public String getCaption( )
	{
		return caption;
	}
	
	public void setClickedListener( ClickedListener listener )
	{
		this.listener = listener;
	}
	
	@Override
	public void keyPressed(int key_code) 
	{
		if( key_code == KeyEvent.VK_ENTER || key_code == KeyEvent.VK_SPACE )
			pressed = true;		
	}

	@Override
	public void keyReleased(int key_code) 
	{	
		if( key_code == KeyEvent.VK_ENTER || key_code == KeyEvent.VK_SPACE )
		{
			pressed = false;
			if( listener != null )
				listener.clicked( this );
		}
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
	}

	@Override
	public void mousePressed(float x, float y, int button) 
	{	
		pressed = true;		
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{	
		pressed = false;
		if( listener != null && internalIntersect( x, y ) )
			listener.clicked( this );
					
	}

	public void setFont( Font font )
	{
		this.font = font;
	}
	
	public Font getFont( )
	{
		return this.font;
	}
	
	@Override
	public void render(GLCanvas canvas) 
	{			
		if( pressed )
			renderPressed( );
		else
			renderReleased( );
	}
	
	protected void renderPressed( )
	{
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x + 2, pos.y - 2, width, height);			
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x + 2, pos.y - 2, width, height );		
				
		float text_width = font.getWidth( caption );
				
		gui.getGL().glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		gui.enableScissor( (int)pos.x + 2, (int)pos.y - (int)height - 2, (int)width, (int)height );	
		if( caption != null )
			font.renderText( (int)(pos.x + width / 2 - text_width / 2 + 2), (int)(pos.y - height / 2 + font.getHeight() / 2 - 2 + font.getDescent()), caption );
		gui.disableScissor( );
	}
	
	protected void renderReleased( )
	{
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x, pos.y, width, height);			
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );	
		
		float text_width = font.getWidth( caption );
						
		gui.getGL().glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		gui.enableScissor( (int)pos.x, (int)pos.y - (int)height, (int)width, (int)height );
		if( caption != null )
			font.renderText( (int)(pos.x + width / 2 - text_width / 2 ), (int)(pos.y - height / 2 + font.getHeight() / 2 + font.getDescent() ), caption );
		gui.disableScissor( );
	}

	@Override
	public boolean isFocusable() 
	{	
		return true;
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
