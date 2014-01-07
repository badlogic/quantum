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
import java.util.List;

import javax.media.opengl.GLCanvas;


public class Container extends Widget
{
	private ArrayList<Widget> widgets = new ArrayList<Widget>( );
	private Widget key_focus = null;
	private Widget last_pressed_widget = null;
	private Widget last_hovered_widget = null;
	
	public Container(Gui gui) 
	{
		super(gui);
	}
	
	public List<Widget> getWidgets( )
	{
		return widgets;
	}
	
	private Widget getWidget( float x, float y )
	{
		for( Widget widget: widgets )
		{
			if( widget.isVisible() == false )
				continue;
			
			if( widget.intersects( x, height + y ) )
				return widget;
		}
		
		return null;
	}

	@Override
	public boolean isFocusable() 
	{	
		return true;
	}

	@Override
	public void keyPressed(int key_code) 
	{			
		if( key_focus == null )
			return;
		
		key_focus.keyPressed( key_code );
	}

	@Override
	public void keyReleased(int key_code) 
	{	
		if( key_focus == null )
			return;
		
		key_focus.keyReleased( key_code );
	}

	@Override
	public void keyTyped(char character) 
	{	
		if( key_focus == null )
			return;
		
		key_focus.keyTyped( character );
	}

	@Override
	public void mouseDragged(float x, float y, int button) 
	{			
		if( last_pressed_widget == null )
			return;
		
		last_pressed_widget.mouseDragged( x - last_pressed_widget.getPosition().x, height + y - last_pressed_widget.getPosition().y, button );
	}

	@Override
	public void mouseExited() 
	{	
		if( last_hovered_widget != null )
			last_hovered_widget.mouseExited();
	}

	@Override
	public void mouseMoved(float x, float y) 
	{	
		Widget widget = getWidget( x, y );
		
		if( last_hovered_widget != widget )
		{
			if( last_hovered_widget != null )
				last_hovered_widget.mouseExited();
			last_hovered_widget = widget;
		}
		
		if( widget == null )
			return;			
		
		widget.mouseMoved( x - widget.getPosition().x, height + y - widget.getPosition().y );		
	}

	@Override
	public void mousePressed(float x, float y, int button) 
	{	
		Widget widget = getWidget( x, y );
		if( widget == null )
			return;
		
		if( widget.isFocusable( ) )
		{
			if( key_focus != null )
				key_focus.setFocus( false );
			key_focus = widget;
			widget.setFocus( true );
		}
		
		widget.mousePressed( x - widget.getPosition().x, height + y - widget.getPosition().y, button );
		last_pressed_widget = widget;
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{	
		if( last_pressed_widget == null )
			return;		
		
		last_pressed_widget.mouseReleased( x - last_pressed_widget.getPosition().x, height + y - last_pressed_widget.getPosition().y, button );
		last_pressed_widget = null;
	}

	@Override
	public void render(GLCanvas canvas) 
	{			
		layout();
		canvas.getGL().glPushMatrix();
		canvas.getGL().glTranslatef( (int)pos.x, (int)(pos.y - height), 0 );
		gui.pushTranslation( (int)pos.x, (int)(pos.y - height));
		
		for( Widget widget: widgets )
			if( widget.isVisible() )
				widget.render( canvas );
		
		gui.pushTranslation( (int)-pos.x, (int)(-(pos.y - height)));
		canvas.getGL().glPopMatrix();			
	}	
	
	protected void layout( )
	{
		for( Widget widget: getWidgets() )
		{
			if( widget instanceof Container )
				if( widget.isVisible() )
					((Container)widget).layout();
		}
	}

	@Override
	public void dispose() 
	{
		for( Widget widget: widgets )
			widget.dispose();		
	}
}
