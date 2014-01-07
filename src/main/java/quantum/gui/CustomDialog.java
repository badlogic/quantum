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

public class CustomDialog extends ScreenAlignementContainer
{			
	ArrayList<Button> buttons = new ArrayList<Button>();
	String caption = "";
	Widget center_box = null;
	Label caption_label = null;
	
	public CustomDialog( Gui gui, float width, String caption, Widget center_box, Button ... buttons ) 
	{
		super( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );			
		this.width = width;
		this.caption = caption;
		this.center_box = center_box;
		setBackgroundColor( 0, 0, 0, 1 );		
		for( Button button: buttons )
			this.buttons.add( button );
		
		pack( );
	}  		
	
	public CustomDialog( Gui gui, HorizontalAlignement h_align, VerticalAlignement v_align, float width, String caption, Widget center_box, Button ... buttons ) 
	{
		super( gui, h_align, v_align );			
		this.width = width;
		this.caption = caption;
		this.center_box = center_box;
		setBackgroundColor( 0, 0, 0, 1 );		
		for( Button button: buttons )
			this.buttons.add( button );
		
		pack( );
	}  		
	
	public void setCaption( String text )
	{
		caption_label.setText( text );
	}
	
	protected void pack( )
	{			
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		caption_label = new Label( gui, caption );
		v_box.addWidget( new Spacer( gui, 10, 5 ) );
		v_box.addWidget( caption_label, HorizontalAlignement.CENTER );
		v_box.addWidget( new Spacer( gui, 10, 5 ) );
		Spacer spacer = new Spacer( gui, this.getWidth() + 20, 0 );
		spacer.setBorderColor( gui.getDefaultBorderColor() );
		v_box.addWidget( spacer );
		v_box.addWidget( new Spacer( gui, 10, 10 ) );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( new Spacer( gui, 10, 10 ) );
		h_box.addWidget( center_box );
		h_box.addWidget( new Spacer( gui, 10, 10 ) );
		
		v_box.addWidget( h_box );
		v_box.addWidget( new Spacer( gui, 10, 10 ) );
		
		h_box = new HorizontalBoxContainer( gui );
		for( Button button: buttons )
		{
			h_box.addWidget( button );
			h_box.addWidget( new Spacer( gui, 5, 10 ) );
		}
		v_box.addWidget( h_box, HorizontalAlignement.RIGHT );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		this.addWidget( v_box );
		
		v_box.layout();
		this.width = v_box.getWidth();
		this.height = v_box.getHeight();
		v_box.setY(v_box.getHeight() );
		v_box.layout();			
		layout();
	}	
	
	public void render( GLCanvas canvas )
	{		
		layout();
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x, pos.y, width, height);			
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );	
		
		super.render( canvas );
	}
}
