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


public class VerticalBoxContainer extends Container 
{	
	LinkedList<HorizontalAlignement> h_aligns = new LinkedList<HorizontalAlignement>( );
	
	public VerticalBoxContainer(Gui gui ) 
	{
		super(gui);	
	}
	
	public void addWidget( Widget widget )
	{
		getWidgets( ).add( widget );
		h_aligns.add( HorizontalAlignement.LEFT );
		layout();
	}
	
	public void addWidget( Widget widget, HorizontalAlignement h_align )
	{
		getWidgets( ).add( widget );
		h_aligns.add( h_align );
		layout();
	}
	
	public void render( GLCanvas canvas )
	{
		layout();
		
		super.render( canvas );
	}

	private float getMaxWidth( )
	{
		float width = 0;
		for( Widget widget: getWidgets() )
		{			
			width = Math.max( widget.getWidth(), width );
		}
		
		return width;
	}
	
	protected void layout() 
	{	
		super.layout();
		float y = height;
		float max_width = getMaxWidth( );
		float height = 0;
		for( int i = 0; i < getWidgets().size(); i++ )
		{			
			Widget widget = getWidgets().get(i);
			HorizontalAlignement h_align = h_aligns.get(i);
			widget.setY(y);
			y-=widget.getHeight();
			height += widget.getHeight();
			
			if( h_align == HorizontalAlignement.CENTER )			
				widget.setX( max_width / 2 - widget.getWidth() / 2 );			
			
			if( h_align == HorizontalAlignement.RIGHT )			
				widget.setX( max_width - widget.getWidth() );			
			
			if( h_align == HorizontalAlignement.LEFT )			
				widget.setX( 0 );							
		}
		
		this.height = height;
		this.width = max_width;
	}
}
