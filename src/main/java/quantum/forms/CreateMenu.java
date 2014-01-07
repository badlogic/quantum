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

public class CreateMenu implements DisplayListener
{
	Quantum quantum;
	CreateMenu self = this;
	
	public CreateMenu( final Quantum quantum, final Gui gui )
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
				new JoinMenu(quantum, gui);			
			}			
		});		
		
		final TextField user_name = new TextField( gui );
		final TextField game_name = new TextField( gui );		
		final TextField port = new TextField( gui );
		final TextField ip = new TextField( gui );
		port.setText( "7777" );
		
		user_name.setSize( 200, 25 );
		user_name.setText( quantum.getLastName() );
		game_name.setSize( 200, 25 );
		port.setSize( 200, 25 );
		ip.setSize( 200, 25 );		
		
		Button create = new Button( gui, "Create" );
		create.setSize( 70, 25 );
		create.setClickedListener( new ClickedListener( ) {
		
			public void clicked(Widget widget) 
			{	
				if( user_name.getText().length() == 0 )
				{
					gui.showConfirmDialog( "Please enter a username", "Error" );
					return;
				}
				
				if( game_name.getText().length() == 0 )
				{
					gui.showConfirmDialog( "Please enter a gamename", "Error" );
					return;
				}
				
				if( port.getText().length() == 0 )
				{
					gui.showConfirmDialog( "Please enter a port number", "Error" );
					return;
				}
				
				int port_number = 0;
				try
				{
					port_number = Integer.parseInt( port.getText() );
				}
				catch( Exception ex )
				{
					gui.showConfirmDialog( "Please enter a valid port number", "Error" );
					return;
				}
				
				try
				{
					quantum.createServer( port_number, game_name.getText(), ip.getText() );					
				}
				catch( Exception ex )
				{
					Log.println( "[CreateMenu] couldn't create server on port " + port_number );
					quantum.closeServerAndClient( );
					gui.showConfirmDialog( "Couldn't create server on port" + port_number + ".\nMake sure the port is not used by another program!", "Error" );
					return;
				}
				
				try
				{
					quantum.createClient( user_name.getText(), "localhost", port_number );					
				}
				catch( Exception ex )
				{					
					Log.println( "[CreateMenu] couldn't create client connecting to localhost" );
					quantum.closeServerAndClient( );
					gui.showConfirmDialog( "Couldn't connect server on port" + port_number + "!", "Error" );
					return;
				}								
				
				quantum.setLastName( user_name.getText() );				
				gui.remove( cont );
				gui.remove( cont2 );
				quantum.removeDisplayListener( self );
				new LobbyMenu(quantum, gui, game_name.getText(), true );	
			}
			
		});
		
		cont.addWidget( new Label( gui, "Player Name" ) );
		cont.addWidget( new Spacer( gui, 0, 5 ) );
		cont.addWidget( user_name );
		cont.addWidget( new Label( gui, "Game Name" ) );
		cont.addWidget( new Spacer( gui, 0, 5 ) );
		cont.addWidget( game_name );
		cont.addWidget( new Label( gui, "Port" ) );
		cont.addWidget( new Spacer( gui, 0, 5 ) );		
		cont.addWidget( port );
		cont.addWidget( new Label( gui, "ip (optional)" ) );
		cont.addWidget( new Spacer( gui, 0, 5 ) );		
		cont.addWidget( ip );
		
		cont.addWidget( new Spacer( gui, 0, 10 ) );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( create );
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
