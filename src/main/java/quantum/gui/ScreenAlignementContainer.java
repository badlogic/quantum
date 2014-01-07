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


public class ScreenAlignementContainer extends Container 
{
	VerticalBoxContainer v_box;
	HorizontalAlignement h_align;
	VerticalAlignement v_align;
	
	public ScreenAlignementContainer(Gui gui, HorizontalAlignement h_align, VerticalAlignement v_align ) 
	{
		super(gui);	
		this.h_align = h_align;
		this.v_align = v_align;
		v_box = new VerticalBoxContainer( gui );
		getWidgets().add( v_box );
	}

	public void addWidget( Widget widget )
	{
		v_box.addWidget(widget);
	}
	
	public void addWidget( Widget widget, HorizontalAlignement alignement )
	{
		v_box.addWidget(widget, alignement);
	}	
	
	public void layout( )
	{
		super.layout();
		this.width = v_box.getWidth();
		this.height = v_box.getHeight();
		
		if( h_align == HorizontalAlignement.LEFT )
			pos.x = 0;
		if( h_align == HorizontalAlignement.CENTER )
			pos.x = gui.getCanvas().getWidth() / 2 - width / 2 ;
		if( h_align == HorizontalAlignement.RIGHT )
			pos.x = gui.getCanvas().getWidth() - width;
		
		if( v_align == VerticalAlignement.TOP )
			pos.y = gui.getCanvas().getHeight();
		if( v_align == VerticalAlignement.CENTER )
			pos.y = gui.getCanvas().getHeight() / 2 + height / 2;
		if( v_align == VerticalAlignement.BOTTOM )
			pos.y =  height;
		
		v_box.setY( height );	
		v_box.layout();
	}

	public void clear() 
	{
		v_box.getWidgets().clear();		
	}
}
