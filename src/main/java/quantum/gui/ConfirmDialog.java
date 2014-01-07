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

import javax.media.opengl.GLCanvas;
;


public class ConfirmDialog extends Container
{	
	VerticalBoxContainer v_box;
	Label label;
	Button ok_button;	
	boolean is_confirmed = false;
	ClickedListener listener;
	ConfirmDialog self = this;
	
	public ConfirmDialog(Gui gui, String text, String caption ) 
	{
		super(gui);		
		setBackgroundColor( 0, 0, 0, 1 );
		layout( text, caption, new ClickedListener( ) {

			public void clicked(Widget widget) 
			{			
				self.gui.remove( self );
			}			
		}, 400);
	}
	
	public ConfirmDialog(Gui gui, String text, String caption, ClickedListener listener, int width )
	{
		super(gui);		
		setBackgroundColor( 0, 0, 0, 1 );
		layout( text, caption, listener, width );
	}
	
	public ConfirmDialog(Gui gui, String text, String caption, ClickedListener listener ) 
	{
		super(gui);		
		setBackgroundColor( 0, 0, 0, 1 );
		layout( text, caption, listener, 400 );
	}	
	
	protected void layout( String text, String caption, ClickedListener listener, int width )
	{
		super.layout();
		label = new Label( gui, text );
		label.setAdaptTextToWidth( true );
		label.setWidth( width );		
		
		ok_button = new Button( gui, "OK" );
		ok_button.setSize( 40, 20 );
		
		v_box = new VerticalBoxContainer( gui );
		Label caption_label = new Label( gui, caption );					
		v_box.addWidget( caption_label, HorizontalAlignement.CENTER );
		
		v_box.addWidget( new Spacer( gui, 10, 10 ) );
		
		Spacer spacer = new Spacer( gui, width + 20, 1 );
		spacer.setBorderColor( gui.getDefaultBorderColor() );
		v_box.addWidget( spacer );
		v_box.addWidget( new Spacer( gui, 10, 10 ) );		
		
		HorizontalBoxContainer center_box = new HorizontalBoxContainer( gui );
		center_box.addWidget( new Spacer( gui, 10, 10 ) );		
		center_box.addWidget( label );
		center_box.addWidget( new Spacer( gui, 10, 10 ) );
		v_box.addWidget( center_box );
		v_box.addWidget( new Spacer( gui, 10, 10 ) );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( ok_button );
		h_box.addWidget( new Spacer( gui, 10, 10 ) );
		v_box.addWidget( h_box, HorizontalAlignement.RIGHT );
		
		v_box.addWidget( new Spacer( gui, 10, 10 ) );
		this.getWidgets().add( v_box );
		
		this.listener = listener;
		
		ok_button.setClickedListener( new ClickedListener( ){

			public void clicked(Widget widget) 
			{
				is_confirmed = true;
				if( self.listener != null )
					self.listener.clicked( self );
			}			
		});
		
		v_box.layout();
		this.width = v_box.getWidth();
		this.height = v_box.getHeight();	
		v_box.setY(v_box.getHeight() );
		v_box.layout();
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
	
	public boolean isConfirmed( )
	{
		return true;
	}
}
