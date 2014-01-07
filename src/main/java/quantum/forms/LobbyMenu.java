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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Bot;
import quantum.gfx.Color;
import quantum.gfx.Renderer;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.ConfirmDialog;
import quantum.gui.EnterListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.List;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.SelectedListener;
import quantum.gui.Spacer;
import quantum.gui.TextArea;
import quantum.gui.TextField;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.net.messages.DisconnectedMessage;
import quantum.net.messages.MapImageMessage;
import quantum.net.messages.MapListMessage;
import quantum.net.messages.Message;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.ReadyMessage;
import quantum.net.messages.TextMessage;
import quantum.net.messages.VoteMessage;
import quantum.utils.FileManager;
import quantum.utils.Log;
import quantum.utils.Timer;

public class LobbyMenu implements DisplayListener
{
	class Map
	{
		String filename;
		String name;
		
		public Map( String filename, String name )
		{
			this.filename = filename;
			this.name = name;
		}
		
		public String toString( )
		{
			return name;
		}
		
		public String getFilename( )
		{
			return filename;
		}
	}
	
	Quantum quantum;
	LobbyMenu self = this;
	ScreenAlignementContainer cont;	
	Gui gui;
	final TextArea text_area;
	String game_name;
	Renderer renderer;
	
	List map_list;	
	List avail_bots;
	
	String last_vote = "";
	Timer last_received = new Timer( );
	
	HashMap<String, BufferedImage> map_images = new HashMap<String, BufferedImage>( );
	Image map_image;
	
	HashMap<String, Integer> votes = new HashMap<String, Integer>();
	
	ArrayList<Bot> bots = new ArrayList<Bot>( );
	
	boolean is_host = false; 
	boolean disconnected = false;
	
	public LobbyMenu( final Quantum quantum, final Gui gui, String game_name, boolean is_host )
	{
		last_received.start();
		
		try {
			quantum.getClient().sendMessage( new PlayerListMessage() );
		} catch (Exception e1) {
			Log.println( "[LobbyMenu] couldn't send PlayerListMessage: " + e1.getMessage() );
		}
		renderer = new Renderer( );
		this.game_name = game_name;
		this.gui = gui;
		this.quantum = quantum;
		this.is_host = is_host;		
		cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.TOP );
		Image image;
		try {
			gui.getCanvas().getContext().makeCurrent();
			image = new Image( gui, FileManager.readFile( "quantum.png" ) );
			cont.addWidget( new Spacer( gui, 0, 50 ) );
			cont.addWidget( image, HorizontalAlignement.CENTER );
		} catch (Exception e) {
			Log.println( "[LobbyMenu] couldn't load image 'quantum.png'" );
			gui.showConfirmDialog( "Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error" );			
		}		
								
		Button back = new Button( gui, "Back" );		
		back.setSize( 70, 25 );		
		back.setClickedListener( new ClickedListener() {
			
			public void clicked(Widget widget) 
			{
				renderer.dispose();
				gui.remove( cont );				
				quantum.closeServerAndClient();
				quantum.removeDisplayListener( self );
				new JoinMenu(quantum, gui);			
			}			
		});		
		
		Button ready = new Button( gui, "ready" );		
		ready.setSize( 70, 25 );		
		ready.setClickedListener( new ClickedListener() {

			public void clicked(Widget widget) 
			{
				try {
					quantum.getClient().sendMessage( new ReadyMessage( quantum.getClient().getPlayer().getId(), quantum.getClient().getPlayer().getName() ) );
				} catch (Exception e) {
					Log.println( "[LobbyMenu] couldn't send ReadyMessage to server: " + e.getMessage() );
					ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!", "Error", new ClickedListener() {

						public void clicked(Widget widget) {
							gui.remove( cont );							
							quantum.closeServerAndClient();
							quantum.removeDisplayListener( self );
							renderer.dispose();
							new StartMenu(quantum, gui);
							gui.remove( widget );
						}							
					});
					
					gui.add( dialog );
				}
			}			
		});		
		
		Button vote = new Button( gui, "Vote" );
		vote.setSize( 75, 25 );		
		
		text_area = new TextArea( gui );
		text_area.setSize( 830, 300 );
				
		final TextField text_field = new TextField( gui );
		text_field.setSize( 830, 25 );
		text_field.setEnterListener( new EnterListener( ){

			public void pressedEnter(Widget widget) 
			{
				if( text_field.getText().length() > 0 )
				{
					TextMessage msg = new TextMessage( quantum.getClient().getPlayer().getId(), quantum.getClient().getPlayer().getName(), text_field.getText() );
					try {
						quantum.getClient().sendMessage( msg );						
					} catch (Exception e) {
						Log.println( "[LobbyMenu] couldn't send TextMessage: " + e.getMessage() );
						ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!", "Error", new ClickedListener() {

							public void clicked(Widget widget) {
								gui.remove( cont );								
								quantum.closeServerAndClient();
								quantum.removeDisplayListener( self );
								renderer.dispose();
								new StartMenu(quantum, gui);
								gui.remove( widget );
							}							
						});
						
						gui.add( dialog );
					}
				}	
				
				text_field.setText( "" );
			}
			
		});			
		
		cont.addWidget( new Label( gui, "Player: " + quantum.getClient().getPlayer().getName()+ " Game: " + game_name ));
		cont.addWidget( new Spacer( gui, 10, 10 ) );
		cont.addWidget( text_area );
		cont.addWidget( text_field );
		cont.addWidget( new Spacer( gui, 10, 10 ) );		
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
				
		h_box = new HorizontalBoxContainer( gui );
		
		if( is_host )
		{
			avail_bots = new List( gui );		
			avail_bots.setBackgroundColor( 0, 0, 0, 1 );		
			avail_bots.setSize( 200, 200 );		
			v_box = new VerticalBoxContainer( gui );
			v_box.addWidget( new Label( gui, "Available Bots" ) );
			v_box.addWidget( new Spacer( gui, 0, 10 ) );
			v_box.addWidget( avail_bots );		
			h_box.addWidget( v_box );
			h_box.addWidget( new Spacer( gui, 10, 0 ) );
			
			File file = FileManager.newFile( "dat/scripts/" );
			String[] bot_files = file.list( new FilenameFilter( ) {
				
				public boolean accept(File dir, String name) {					
					return name.endsWith(".bsh");
				}				
			} );
			
			for( String bot_file: bot_files )
			{
				avail_bots.addItem( bot_file );
			}				
					
			if( bot_files.length != 0 )
				avail_bots.setSelectedItem( 0 );
		}
				
		
		map_list = new List( gui );
		map_list.setBackgroundColor( 0, 0, 0, 1 );
		map_list.setSize( 200, 200 );
		v_box = new VerticalBoxContainer( gui );
		v_box.addWidget( new Label( gui, "Maps" ) );
		v_box.addWidget( new Spacer( gui, 0, 10 ) );
		v_box.addWidget( map_list );
		v_box.addWidget( new Spacer( gui, 0, 10 ) );
		h_box.addWidget( v_box );
		
		map_image = new Image( gui, new BufferedImage( 256, 256, BufferedImage.TYPE_3BYTE_BGR) );
		map_image.setWidth(200);
		map_image.setHeight(200);
		map_image.setBorderColor( new Color( 1, 1, 1, 1 ) );
		v_box = new VerticalBoxContainer( gui );
		v_box.addWidget( new Label( gui, "Preview" ) );		
		v_box.addWidget( new Spacer( gui, 0, 10 ) );
		v_box.addWidget( map_image );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( v_box );
		cont.addWidget( h_box, HorizontalAlignement.CENTER );
				
		final Button remove_bot = new Button( gui, "Remove Bot" );
		remove_bot.setSize( 75, 25 );
		remove_bot.setClickedListener( new ClickedListener( ) {
			
			public void clicked(Widget widget) 
			{			
				try {
					if( bots.size() == 0 )					
						return;					
					
					Bot bot = bots.remove( 0 );		
					bot.dispose( quantum.getClient( ) );
				} catch (Exception e) 
				{
					gui.showConfirmDialog( "couldn't load bot\n" + e.getMessage(), "Error" );
					e.printStackTrace();
				}
			}			
		} );
			
		final Button add_bot = new Button( gui, "Add Bot" );
		add_bot.setSize( 75, 25 );
		add_bot.setClickedListener( new ClickedListener( ) {
			
			public void clicked(Widget widget) 
			{			
				try {
					if( bots.size() == 8 )
					{
						gui.showConfirmDialog( "Only 4 bots allowed", "Information" );
						return;
					}
					
					if( avail_bots.getSelectedItem() == null )
						return;
					
					Bot bot = new Bot( FileManager.getPath() + "dat/scripts/" + avail_bots.getSelectedItem(), "bot " + ( bots.size() + 1 ) + " [" + avail_bots.getSelectedItem().toString().replace( ".bsh", "" ) + "]", quantum.getClient() );
					bots.add( bot );
				} catch (Exception e) 
				{
					Log.println( "[LobbyMenu] couldn't load bot: " + e.getMessage() );
					gui.showConfirmDialog( "couldn't load bot\n" + e.getMessage(), "Error" );
					e.printStackTrace();
				}
			}			
		} );
				
		
		h_box = new HorizontalBoxContainer( gui );
		
		if( is_host )
		{
			h_box.addWidget( add_bot );
			h_box.addWidget( new Spacer( gui, 5, 0 ) );
			h_box.addWidget( remove_bot );
			h_box.addWidget( new Spacer( gui, 5, 0 ) );
		}
		h_box.addWidget( vote );
		h_box.addWidget( new Spacer( gui, 5, 0 ) );
		h_box.addWidget( ready );
		h_box.addWidget( new Spacer( gui, 5, 0 ) );
		h_box.addWidget( back );	
		cont.addWidget( new Spacer( gui, 0, 10 ) );
		cont.addWidget( h_box, HorizontalAlignement.CENTER );
				
		
		vote.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{								
				if( map_list.getSelectedItem() != null )
					try {
						if( !map_list.getSelectedItem().toString().equals(last_vote) )
						{
							quantum.getClient().sendMessage( new VoteMessage( quantum.getClient().getPlayer().getId(), quantum.getClient().getPlayer().getName(), ((Map)map_list.getSelectedItem()).getFilename()) );
							last_vote = map_list.getSelectedItem().toString();
						}
					} catch (Exception e) 
					{					
						Log.println( "[LobbyMenu] couldn't send VoteMessage: " + e.getMessage() );
						ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!", "Error", new ClickedListener() {

							public void clicked(Widget widget) {
								gui.remove( cont );								
								quantum.closeServerAndClient();
								quantum.removeDisplayListener( self );
								renderer.dispose();
								new StartMenu(quantum, gui);
								gui.remove( widget );
							}							
						});
						
						gui.add( dialog );
					}
			}			
		});			
		
		map_list.setSelectedListener( new SelectedListener( ) {
			public void selected(Widget widget, Object selection) 
			{
				if( selection != null )
				{
					if( !map_images.containsKey( ((Map)selection).getFilename() ) )
					{
						MapImageMessage msg = new MapImageMessage( ((Map)selection).getFilename(), null );
						try {
							quantum.getClient().sendMessage( msg );
						} catch (Exception e) 
						{
							Log.println( "[LobbyMenu] couldn't send MapImageMessage: " + e.getMessage() );
							ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!", "Error", new ClickedListener() {

								public void clicked(Widget widget) {
									gui.remove( cont );									
									quantum.closeServerAndClient();
									quantum.removeDisplayListener( self );
									renderer.dispose();
									new StartMenu(quantum, gui);
									gui.remove( widget );
								}							
							});
							
							gui.add( dialog );
						}
					}
					else
					{
						map_image.setImage( map_images.get( ((Map)selection).getFilename() ) );
					}
				}								
			}			
		});
		
		gui.add( cont );		
		quantum.addDisplayListener( this );
		
		try {
			quantum.getClient().sendMessage( new MapListMessage( ) );
		} catch (Exception e) 
		{		
			Log.println( "[LobbyMenu] couldn't send MapListMessage: " + e.getMessage() );
			ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!", "Error", new ClickedListener() {

				public void clicked(Widget widget) {
					gui.remove( cont );					
					quantum.closeServerAndClient();
					quantum.removeDisplayListener( self );
					renderer.dispose();
					new StartMenu(quantum, gui);
					gui.remove( widget );
				}							
			});
			
			gui.add( dialog );
		}
	}
	
	public void display( GLCanvas canvas )
	{
		if( disconnected )
			return;
		
		try {
			if( last_received.getElapsedSeconds() > 8 )
				throw new Exception( "server timed out" );
			
			Message msg = quantum.getClient().readMessage();
			if( msg != null )
			{
				last_received.stop();
				last_received.start();
				
				if( msg instanceof TextMessage )
				{
					text_area.setText( text_area.getText() + "\n" + ((TextMessage)msg).getName() + ": " + ((TextMessage)msg).getMessage() );
				}
				
				if( msg instanceof PlayerListMessage )
				{
					text_area.setText( text_area.getText() + "\nIn this channel:\n" + ((PlayerListMessage)msg).toString() );
				}
				
				if( msg instanceof ReadyMessage )
				{
					ReadyMessage ready = (ReadyMessage)msg;
					
					if( ready.getId() == -1 )
					{
						gui.remove( cont );						
						renderer.dispose();
						quantum.removeDisplayListener( self );
						new NetworkedGame(quantum, gui, game_name, bots, is_host );		
					}
					else
					{
						text_area.setText( text_area.getText() + "\n" + ready.getName() + " is ready!" );
					}					
				}
				
				if( msg instanceof MapListMessage )
				{
					MapListMessage map_list = (MapListMessage)msg;
					this.map_list.removeAll();
					for( int i = 0; i < map_list.getMaps().size(); i++ )
					{
						Log.println( "[LobbyMenu] added map '" + map_list.getNames().get(i)  + "'" );
						this.map_list.addItem( new Map( map_list.getMaps().get(i), map_list.getNames().get(i) ) );
					}
				}
				
				if( msg instanceof DisconnectedMessage )
				{
					DisconnectedMessage disconnected = (DisconnectedMessage)msg;
					text_area.setText( text_area.getText() + "\n" + disconnected.getName() + " disconnected");
				}
				
				if( msg instanceof MapImageMessage )
				{
					MapImageMessage image_msg = (MapImageMessage)msg;					
					BufferedImage img = renderer.takeCenteredScreenShot( canvas, image_msg.getSimulation());
					map_images.put( ((MapImageMessage) msg).getName(), img );					
					
					if( image_msg.getName().equals( ((Map)map_list.getSelectedItem()).getFilename() ) )
						map_image.setImage( map_images.get( image_msg.getName() ) );
				}
				
				if( msg instanceof VoteMessage )
				{					
					VoteMessage v_msg = (VoteMessage)msg;
					
					if( !votes.containsKey( v_msg.getName() ) )
						votes.put( v_msg.getName(), 1 );
					else
						votes.put( v_msg.getName(), votes.get( v_msg.getName() ) + 1 );									
					
					text_area.setText( text_area.getText() + "\n" + v_msg.getUser() + " vote for '" + v_msg.getName() );
				}
			}			
		} 
		catch (Exception e) 
		{		
			disconnected = true;
			Log.println( "[LobbyMenu] error in display method, handling incoming messages failed: " + e.getMessage() );
			gui.showConfirmDialog( "Disconnected!", "Error", new ClickedListener() {

				public void clicked(Widget widget) {
					gui.remove( cont );					
					quantum.closeServerAndClient();
					quantum.removeDisplayListener( self );
					renderer.dispose();					
					new StartMenu(quantum, gui);					
				}							
			});
		}
		
		try {
			Thread.sleep( 10 );
		} catch (InterruptedException e) {
		}
				
	}
}
