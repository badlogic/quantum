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

import java.util.LinkedList;

import javax.media.opengl.GLCanvas;


public class HorizontalBoxContainer extends Container
{
	LinkedList<VerticalAlignement> v_aligns = new LinkedList<VerticalAlignement>( );
	
	public HorizontalBoxContainer(Gui gui ) 
	{
		super(gui);	
	}
	
	public void addWidget( Widget widget )
	{
		getWidgets( ).add( widget );
		v_aligns.add( VerticalAlignement.TOP );
	}
	
	public void addWidget( Widget widget, VerticalAlignement v_align )
	{
		getWidgets( ).add( widget );
		v_aligns.add( v_align );
	}
	
	public void render( GLCanvas canvas )
	{
		layout();
		
		super.render( canvas );
	}

	private float getMaxHeight( )
	{
		float height = 0;
		for( Widget widget: getWidgets() )
		{
			if( widget instanceof Container )
				((Container)widget).layout();
			height = Math.max( widget.getHeight(), height );
		}
		
		return height;
	}
	
	protected void layout() 
	{	
		super.layout();
		float x = 0;
		float max_height = getMaxHeight( );
		float width = 0;
		for( int i = 0; i < getWidgets().size(); i++ )
		{			
			Widget widget = getWidgets().get(i);
			VerticalAlignement v_align = v_aligns.get(i);
			widget.setX(x);
			x+=widget.getWidth();
			width += widget.getWidth();
			
			if( v_align == VerticalAlignement.TOP )
				widget.setY( max_height );
			
			if( v_align == VerticalAlignement.CENTER )			
				widget.setY( max_height / 2 + widget.getHeight() / 2 );			
			
			if( v_align == VerticalAlignement.BOTTOM )			
				widget.setY( widget.getHeight() );														
		}
		
		this.height = max_height;
		this.width = width;
	}
}
