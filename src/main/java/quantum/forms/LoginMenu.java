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
package quantum.forms;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Spacer;
import quantum.gui.TextField;
import quantum.gui.VerticalAlignement;
import quantum.gui.Widget;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class LoginMenu implements DisplayListener
{
	Quantum quantum;
	LoginMenu self = this;
	
	public LoginMenu( final Quantum quantum, final Gui gui )
	{ 
		this.quantum = quantum;
		quantum.addDisplayListener( self );
		final ScreenAlignementContainer cont2 = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.TOP );
		Image image;
		try {
			gui.getCanvas().getContext().makeCurrent();
			image = new Image( gui, FileManager.readFile( "quantum.png" ) );
			cont2.addWidget( new Spacer( gui, 0, 50 ) );
			cont2.addWidget( image );
		} catch (Exception e) 
		{
			Log.println( "[CreateMenu] couldn't load image 'quantum.png'" );
			gui.showConfirmDialog( "Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error" );			
		}		
		
		gui.add( cont2 );
		
		final ScreenAlignementContainer cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );		
		Button back = new Button( gui, "Back" );			
		back.setSize( 70, 25 );		
		back.setClickedListener( new ClickedListener() {
			
			public void clicked(Widget widget) 
			{
				gui.remove( cont );
				gui.remove( cont2 );
				quantum.removeDisplayListener( self );
				new StartMenu(quantum, gui);			
			}			
		});		
		
		final TextField user_name = new TextField( gui );				
		
		user_name.setSize( 200, 25 );
		user_name.setText( quantum.getLastName() );		
		
		Button login = new Button( gui, "Login" );
		login.setSize( 70, 25 );
		login.setClickedListener( new ClickedListener( ) {
		
			public void clicked(Widget widget) 
			{	
				if( user_name.getText().length() == 0 )
				{
					gui.showConfirmDialog( "Please enter a username", "Error" );
					return;
				}								
				
				quantum.setLastName( user_name.getText() );				
				gui.remove( cont );
				gui.remove( cont2 );
				quantum.removeDisplayListener( self );
				new JoinMenu(quantum, gui );
			}
			
		});
		
		cont.addWidget( new Label( gui, "Enter Nickname" ) );
		cont.addWidget( new Spacer( gui, 0, 5 ) );
		cont.addWidget( user_name );				
		cont.addWidget( new Spacer( gui, 0, 10 ) );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( login );
		h_box.addWidget( new Spacer( gui, 0, 5 ) );
		h_box.addWidget( back );
		
		cont.addWidget( h_box, HorizontalAlignement.RIGHT );		
		gui.add( cont );
	}

	public void display(GLCanvas canvas) 
	{
		try {
			Thread.sleep( 10 );
		} catch (InterruptedException e) {
		}
		
	}
}
