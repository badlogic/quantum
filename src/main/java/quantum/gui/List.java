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

public class List extends Widget 
{
	ArrayList<Object> items = new ArrayList<Object>();		
	Font font;
	int scroll_pos = 0;
	int SCROLL_SIZE = 16;
	
	boolean upper_hover = false;
	boolean lower_hover = false;
	boolean uper_hold = false;
	boolean lower_hold = false;
	Timer scroll_timer = new Timer();
	Object selected_item;
	SelectedListener listener;

	public List(Gui gui) 
	{
		super(gui);
		font = gui.getDefaultFont();
	}

	public void setSelectedListener( SelectedListener listener )
	{
		this.listener = listener;
	}
	
	public void addItem( Object item )
	{
		items.add( item );		
		layout();
	}
	
	public void unselect( )
	{
		selected_item = null;
	}
	
	public Object getSelectedItem( )
	{
		return selected_item;
	}
	
	public void removeAll( )
	{
		items.clear();
	}
	
	public void setFont( Font font )
	{
		this.font = font;
		layout();
	}
	
	private void layout( )
	{						
		scroll_pos = Math.max( items.size() - (int)Math.floor( height / font.getHeight()), 0 );
	}
	
	public Font getFont( )
	{
		return font;
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isFocusable() {
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
		lower_hover = false;
		upper_hover = false;
	}

	@Override
	public void mouseMoved(float x, float y) {
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
	public void mousePressed(float x, float y, int button) {
		if( internalIntersect( x, y, width - SCROLL_SIZE, 0, SCROLL_SIZE, SCROLL_SIZE ) )
		{
			scroll_pos = Math.max( 0, scroll_pos-1 );
			scroll_timer.stop();
			scroll_timer.start();
			uper_hold = true;
		}
		if( internalIntersect( x, y, width - SCROLL_SIZE,  -height + SCROLL_SIZE, SCROLL_SIZE, SCROLL_SIZE ) )
		{
			scroll_pos = Math.min( scroll_pos+1, items.size() - 1 );
			scroll_timer.stop();
			scroll_timer.start();
			lower_hold = true;
		}
		
		if( internalIntersect( x, y, 0, 0, width - SCROLL_SIZE, height ) )
		{
			int hit_pos = (int)(-y / font.getHeight()) + scroll_pos;
			if( hit_pos < items.size() )
				selected_item = items.get(hit_pos);
			else
				selected_item = null;
			
			if( listener != null )
				listener.selected( this, selected_item );
		}
	}

	@Override
	public void mouseReleased(float x, float y, int button) {
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
		gui.enableScissor( pos.x, pos.y - height, width - SCROLL_SIZE, height - 1 );
		for( int i = scroll_pos, j = 0; i >= 0 && i < items.size() && i < scroll_pos + (int)Math.floor( height / font.getHeight()); i++, j++ )
		{
			if( items.get(i) == selected_item )
			{
				gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
				renderQuad(pos.x, pos.y - j * font.getHeight(), width, font.getHeight());
				gl.glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
			}
			else
			{
				gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
			}
			font.renderText( (int)(pos.x + 5), (int)(pos.y - j * font.getHeight() + font.getDescent()), items.get(i).toString() );			
				
		}
		gui.disableScissor();
		
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
				scroll_pos = Math.min( scroll_pos+1, items.size() - 1 );
				scroll_timer.stop();
				scroll_timer.start();				
			}
		}
	}

	public void removeItem(Object selectedItem) 
	{
		if( selected_item == selectedItem )
			selected_item = null;
		
		items.remove( selectedItem );
		
	}

	public void setSelectedItem(int i) 
	{
		selected_item = items.get( i );
		
	}

	public Object[] getItems() 
	{		
		return items.toArray( new Object[0] );
	}

}
