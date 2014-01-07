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

import quantum.gfx.Font;
import quantum.utils.Timer;

public class TextField extends Widget
{
	public enum Alignement
	{
		LEFT, 
		CENTER,
		RIGHT
	}
	
	String text = "";
	Font font;
	Alignement alignement = Alignement.LEFT;	
	Timer blink_timer = new Timer( );
	boolean blink = false;
	private boolean limit;
	private EnterListener listener;
	private boolean enabled = true;
	
	public TextField(Gui gui) 
	{
		super(gui);	
		this.font = gui.getDefaultFont();
		blink_timer.start();
	}
	
	public void setEnterListener( EnterListener listener )
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
		
	}

	@Override
	public void keyTyped(char character) 
	{				
		if( !enabled )
			return;
		
		if( character != '\b' && character != '\n' && Character.isISOControl( character ) )
			return;
		
		if( character == '\n' )
		{
			if( listener != null )
				listener.pressedEnter( this );
			return;
		}
		
		if( character == '\b' )
		{
			if( text.length() == 0 )
				return;
			else
			{
				if( text.length() == 1 )
					text = "";
				else
					text = text.substring( 0, text.length() - 1 );
				return;
			}
		}
				
		text += character;			
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
		
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{	
		
	}

	public void setText( String text )
	{
		if( text == null )
			this.text = "";
		else
			this.text = text;
	}
	
	public String getText( )
	{
		return text;
	}
	
	@Override
	public void render(GLCanvas canvas) 
	{	
		GL gl = canvas.getGL();
		
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x, pos.y, width, height);
		
		gl.glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );				
		
		float x = 0;
		float y = pos.y - height / 2 + font.getHeight() / 2 + font.getDescent();
		
		if( alignement == Alignement.LEFT )		
			x = pos.x + 2;
		
		if( alignement == Alignement.CENTER )
			x = pos.x + ( width - 4 ) / 2 - font.getWidth( text ) / 2;
		
		if( alignement == Alignement.RIGHT )
			x = pos.x + width - font.getWidth( text );
		
		gui.getGL().glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		
		if( blink_timer.getElapsedSeconds() > 0.5 )
		{
			blink_timer.stop();
			blink_timer.start();
			blink = !blink;
			
			if( !hasFocus() )
				blink = false;
		}
		
		gui.enableScissor( (int)pos.x + 1, (int)pos.y - (int)height, (int)width - 2, (int)height );
		
		if( x + font.getWidth(text + "_") > pos.x + width )
			x = x - (x + font.getWidth(text + "_") - (pos.x + width) ); 
		
		font.renderText( (int)x, (int)y, text + (blink?"_":"") );
		gui.disableScissor();
	}
	
	public void setAlignement( Alignement alignement )
	{
		this.alignement = alignement;
	}
	
	public Alignement getAlignement( )
	{
		return alignement;
	}
	
	public void setLimitToWidth( boolean limit )
	{
		this.limit = limit;
	}
	
	public boolean isLimitedToWidth( )
	{
		return limit;
	}


	@Override
	public void mouseExited() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void setEnabled(boolean b) 
	{
		enabled = b;
		
	}
}
