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

import javax.media.opengl.GLCanvas;

import quantum.gfx.Font;

public class Label extends Widget
{
	String text = "";
	boolean adapt = false;
	Font font;
	ArrayList<String> lines = new ArrayList<String>();
	boolean render_border = true;
	
	public Label( Gui gui, String text )
	{
		super(gui);
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );		
		font = gui.getDefaultFont();		
		setText( text );
	}
	
	public Label( Gui gui, String text, float width )
	{
		super(gui);
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );		
		font = gui.getDefaultFont();	
		super.setWidth( width );		
		setAdaptTextToWidth( true );
		setText( text );
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
	
	public void setWidth( float width )
	{
		super.setWidth( width );
		layout();
	}
	
	public void setAdaptTextToWidth( boolean adapt )
	{
		this.adapt = adapt;
		layout();
	}
	
	public boolean adaptsTextToWidth( )
	{
		return adapt;
	}
	
	private void layout( )
	{
		lines.clear( );
		
		if( adapt )
		{					
			float width = 0;
			String line = "";
			for( int i = 0; i < text.length(); i++ )
			{
				char c = text.charAt( i );
				width += font.getWidth( c );
				
				if( width > this.width - 5 || c == '\n' )
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
			
			height = lines.size() * font.getHeight();
		}		
		else
		{
			float width = 0;
			String line = "";
			float max_width = 0;
			for( int i = 0; i < text.length(); i++ )
			{
				char c = text.charAt( i );
				width += font.getWidth( c );
				
				if( c == '\n' )
				{
					max_width += Math.max( max_width, width );
					lines.add( line.trim() );
					line = "";	
					line += c;
					width = font.getWidth( c );					
					continue;
				}				
				
				line += c;
			}
			
			if( line.length() != 0 )
			{
				lines.add( line.trim() );
				max_width += Math.max( max_width, width );
			}
			
			this.width = max_width;
			this.height = lines.size() * font.getHeight();
		}
	}
	
	public void setText( String text )
	{
		if( text == null )
			this.text = "";
		else
			this.text = text;
		
		layout();
	}
	
	public String getText( )
	{
		return text;
	}
	
	@Override
	public boolean isFocusable() 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void keyPressed(int key_code) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(int key_code) {
		// TODO Auto-generated method stub
		
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
	public void mouseExited() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(float x, float y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(GLCanvas canvas) 
	{		
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad( pos.x-2, pos.y+2, width+2, height+2 );
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x-2, pos.y+2, width+2, height+2 );	
		
		canvas.getGL().glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );	
		for( int i = 0; i < lines.size(); i++ )		
			font.renderText( (int)pos.x, (int)(pos.y - i * font.getHeight() + font.getDescent()), lines.get(i) );
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void appendText( String text ) 
	{
		
		if( text == null )
			this.text += "";
		else
			this.text += text;
		
		layout();
	}

}
