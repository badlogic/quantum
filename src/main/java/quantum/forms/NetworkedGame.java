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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Bot;
import quantum.game.GameLoop;
import quantum.game.Simulation;
import quantum.gfx.Color;
import quantum.gui.Button;
import quantum.gui.CheckBox;
import quantum.gui.ClickedListener;
import quantum.gui.ConfirmDialog;
import quantum.gui.CustomDialog;
import quantum.gui.EnterListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Slider;
import quantum.gui.Spacer;
import quantum.gui.TextArea;
import quantum.gui.TextField;
import quantum.gui.ValueChangedListener;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.math.Matrix;
import quantum.net.messages.Message;
import quantum.net.messages.SimulationMessage;
import quantum.net.messages.TextMessage;
import quantum.sound.SoundManager;
import quantum.sound.SoundStream;
import quantum.utils.Log;

public class NetworkedGame implements DisplayListener, KeyListener
{
	Quantum quantum;
	GameLoop loop;
	Gui gui;
	String game_name;
	Simulation sim;
	Matrix ortho_mat = new Matrix( );
	NetworkedGame self = this;	
	ScreenAlignementContainer cont = null;	
	CustomDialog game_menu;
	ScreenAlignementContainer chat = null;
	TextArea text_area;
	int known_messages = 0;
	ArrayList<Bot> bots = new ArrayList<Bot>();
	boolean is_host = false;
	SoundStream music_stream;
	
	public NetworkedGame( final Quantum quantum, final Gui gui, String game_name, ArrayList<Bot> bots, boolean is_host )
	{
		music_stream = SoundManager.playStream( "sounds/bgsound.ogg" );		
		music_stream.setVolume( quantum.getConfig().getVolumeMusic() );
		music_stream.setLooping( true );
		
		this.quantum = quantum;
		this.game_name = game_name;		
		this.gui = gui;
		this.bots = bots;
		this.is_host = is_host;
		
		
		quantum.addDisplayListener( this );
		gui.getCanvas().addKeyListener( this );
		
		chat = new ScreenAlignementContainer( gui, HorizontalAlignement.LEFT, VerticalAlignement.BOTTOM );
		text_area = new TextArea( gui );
		TextField text_field = new TextField( gui );
		text_area.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		text_area.setSize( 350, 80 );
		text_field.setSize( 350, 20 );
		text_field.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		text_field.setEnterListener( new EnterListener( ) {

			public void pressedEnter(Widget widget) 
			{
				TextField text = (TextField)widget;
				try {
					quantum.getClient().sendMessage( new TextMessage( quantum.getClient().getPlayer().getId(), quantum.getClient().getPlayer().getName(), text.getText() ) );
				} catch (Exception e) 
				{							
					Log.println( "[GameMenu] couldn't send text message to server: " + e.getMessage() );
				}
				text.setText( "" );
			}
			
		} );
		
		chat.addWidget( text_area );		
		chat.addWidget( text_field );
		gui.add( chat );
	}
	
	public void showMenu( )
	{
		VerticalBoxContainer content = new VerticalBoxContainer( gui );
		
		Button leave = new Button( gui, "Quit Game" );
		leave.setSize( 200, 25 );		
		
		Button save = new Button( gui, "Save Recording" );
		save.setSize( 200, 25 );		
				
		content.addWidget( save, HorizontalAlignement.CENTER );
		content.addWidget( leave, HorizontalAlignement.CENTER );
		
		final CheckBox glow = new CheckBox( gui, "Glow Enabled" );
		glow.setSize( 200, 25 );
		glow.setChecked( loop.getRenderer().isGlowOn() );
		content.addWidget( new Spacer( gui, 0, 10 ) );
		content.addWidget( glow, HorizontalAlignement.CENTER );
		glow.setClickedListener( new ClickedListener( ) {
			
			public void clicked(Widget widget) 
			{					
				loop.getRenderer().useGlow( glow.isChecked() );
			}
			
		});
		
		final Label label = new Label( gui, "Popup Delay [" + loop.getGameInterface().getHoverDelay() * 1000 + " ms]" );
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( label, HorizontalAlignement.CENTER );
		
		final Slider delay = new Slider( gui, 0, 0.25f, loop.getGameInterface().getHoverDelay() );
		delay.setSize( 100, 5 );
		delay.setBackgroundColor( new Color( 0.3f, 0.3f, 0.3f, 1 ) );				
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( delay, HorizontalAlignement.CENTER );
		delay.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{			
				loop.getGameInterface().setHoverDelay( delay.getValue() );
				label.setText( "Popup Delay [" + loop.getGameInterface().getHoverDelay() * 1000 + " ms]" );
				quantum.setDelay( delay.getValue() );
			}			
		});
		
		final Label label_music = new Label( gui, "Music Volume" );
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( label_music, HorizontalAlignement.CENTER );
		
		final Slider music = new Slider( gui, 0, 1, quantum.getConfig().getVolumeMusic() );
		music.setSize( 100, 5 );
		music.setBackgroundColor( new Color( 0.3f, 0.3f, 0.3f, 1 ) );				
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( music, HorizontalAlignement.CENTER );
		music.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{			
				quantum.getConfig().setVolumeMusic( music.getValue() );
				music_stream.setVolume( music.getValue() );
			}			
		});
		
		final Label label_effect = new Label( gui, "Effects Volume" );
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( label_effect, HorizontalAlignement.CENTER );
		
		final Slider effect = new Slider( gui, 0, 1, quantum.getConfig().getVolumeSfx() );
		effect.setSize( 100, 5 );
		effect.setBackgroundColor( new Color( 0.3f, 0.3f, 0.3f, 1 ) );				
		content.addWidget( new Spacer( gui, 0, 10 ) );				
		content.addWidget( effect, HorizontalAlignement.CENTER );
		effect.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{			
				quantum.getConfig().setVolumeSfx( effect.getValue() );
				SoundManager.setBufferVolume( effect.getValue() );
			}			
		});
		
		
		game_menu = new CustomDialog( gui, 210, "Game Menu", content );
		gui.add( game_menu );
		
		leave.setClickedListener( new ClickedListener( ) {
			public void clicked(Widget widget) 
			{						
				quantum.closeServerAndClient();
				quantum.removeDisplayListener( self );
				gui.getCanvas().removeKeyListener( self );
				loop.dispose();						
				hideMenu( );		
				removeChat( );
				SoundManager.stopAll();
				new StartMenu(quantum, gui);
				return;
			}
		} );	
		
		save.setClickedListener( new ClickedListener( ) {
		public void clicked(Widget widget) 			
		{
			hideMenu( );
			showSaveRecordingDialog( true );						
		}
		});
		
	}
	
	private void showSaveRecordingDialog( final boolean in_game ) 
	{	
		save_menu_visible = true;
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		
		Button save = new Button( gui, "Save" );
		Button back = new Button( gui, "Cancel" );
		
		save.setSize( 75, 25 );
		back.setSize( 75, 25 );
		
		v_box.addWidget( new Label( gui, "Enter Filename:" ) );
		v_box.addWidget( new Spacer( gui, 0, 10 ) );
		
		final TextField file = new TextField( gui );
		file.setFocus( true );
		file.setText( "game-" + new SimpleDateFormat( "yyyy-MM-dd-hh-mm" ).format(Calendar.getInstance().getTime()) + ".rec" );
		file.setSize( 200, 25 );
		
		v_box.addWidget( file );
		
		CustomDialog dialog = new CustomDialog( gui, 300, "Save Game Recording", v_box, save, back );				
		final CustomDialog ref = dialog;
		
		save.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 			
			{
				if( file.getText().equals( "" ) )
				{
					gui.showConfirmDialog( "Error", "You have to specify the filename!" );
					return;
				}							
				
				if( !in_game )
				{
					try {
						loop.saveRecording( "dat/recordings/" + file.getText() );
					} catch (Exception e) {
						Log.println( "[LocalGame] couldn't save recording to '" + file.getText() + "': " + Log.getStackTrace( e ) );
					}
					quantum.removeDisplayListener( self );
					gui.getCanvas().removeKeyListener( self );
					loop.dispose();						
					hideMenu();
					gui.remove( ref );
					SoundManager.stopAll();
					new SinglePlayerMenu(quantum, gui);
				}
				else
				{
					try {
						loop.saveRecording( "dat/recordings/" + file.getText() );
					} catch (Exception e) {
						Log.println( "[LocalGame] couldn't save recording to '" + file.getText() + "': " + Log.getStackTrace( e ) );
					}
					gui.remove( ref );
					showMenu( );
				}
				save_menu_visible = false;
				return;									
			}
			
		});
		
		back.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) {
				save_menu_visible = false;
				if( !in_game )
				{
					quantum.removeDisplayListener( self );
					gui.getCanvas().removeKeyListener( self );
					loop.dispose();						
					hideMenu( );
					gui.remove( ref );
					new SinglePlayerMenu(quantum, gui);
					SoundManager.stopAll();
					return;
				}
				else
				{
					gui.remove( ref );
					showMenu( );
				}
			}
			
		});			
		
		gui.add( dialog );
	}
	
	boolean game_over_triggered = false;
	private void showGameOverMenu( )
	{				
		Button save = new Button( gui, "Save Recording" );
		Button back = new Button( gui, "Back" );
		
		save.setSize( 100, 25 );
		back.setSize( 75, 25 );
		
		CustomDialog dialog = null;
		
		if( loop.getSimulation().getPlayerStats().get( loop.getClient().getPlayer().getId() ) != null )
			dialog = new CustomDialog( gui, 300, "Game Over", new Label( gui, "You have won! You can save the recording of the game for later playback.", 300 ), save, back );
		else
			dialog = new CustomDialog( gui, 300, "Game Over", new Label( gui, "You have lost! You can save the recording of the game for later playback.", 300 ), save, back );
		
		final CustomDialog ref = dialog;
		
		save.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 			
			{
				gui.remove( ref );
				showSaveRecordingDialog( );						
			}
			
		});
		
		back.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) {
				removeBots( );
				quantum.removeDisplayListener( self );
				gui.getCanvas().removeKeyListener( self );
				loop.dispose();
				hideMenu( );
				removeChat( );	
				gui.remove( ref );
				SoundManager.stopAll();
				new LobbyMenu(quantum, gui, game_name, is_host );
			}
			
		});					
		
		gui.add( dialog );			
	}
	
	boolean save_menu_visible = false;
	private void showSaveRecordingDialog() 
	{	
		save_menu_visible = true;
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		
		Button save = new Button( gui, "Save" );
		Button back = new Button( gui, "Cancel" );
		
		save.setSize( 75, 25 );
		back.setSize( 75, 25 );
		
		v_box.addWidget( new Label( gui, "Enter Filename:" ) );
		v_box.addWidget( new Spacer( gui, 0, 10 ) );
		
		final TextField file = new TextField( gui );
		file.setFocus( true );
		file.setText( "game-" + new SimpleDateFormat( "yyyy-MM-dd-hh-mm" ).format(Calendar.getInstance().getTime()) + ".rec" );
		file.setSize( 200, 25 );
		
		v_box.addWidget( file );
		
		CustomDialog dialog = new CustomDialog( gui, 300, "Save Game Recording", v_box, save, back );				
		final CustomDialog ref = dialog;
		
		save.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 			
			{
				if( file.getText().equals( "" ) )
				{
					gui.showConfirmDialog( "Error", "You have to specify the filename!" );
					return;
				}
				
				try {
					loop.saveRecording( "dat/recordings/" + file.getText() );
				} catch (Exception e) {
					Log.println( "[LocalGame] couldn't save recording to '" + file.getText() + "': " + Log.getStackTrace( e ) );
				}
				
				removeBots( );
				quantum.removeDisplayListener( self );
				gui.getCanvas().removeKeyListener( self );
				loop.dispose();
				hideMenu( );
				removeChat( );		
				gui.remove( ref );
				save_menu_visible = false;
				SoundManager.stopAll();
				new LobbyMenu(quantum, gui, game_name, is_host );
				return;									
			}
			
		});
		
		back.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) {
				removeBots( );
				quantum.removeDisplayListener( self );
				gui.getCanvas().removeKeyListener( self );
				loop.dispose();
				hideMenu( );
				removeChat( );		
				gui.remove( ref );
				save_menu_visible = false;
				SoundManager.stopAll();
				new LobbyMenu(quantum, gui, game_name, is_host );
				return;
			}
			
		});			
		
		gui.add( dialog );
	}

	public void display(GLCanvas canvas) 
	{	
		Thread.currentThread().setName( "Networked Game Thread" );
		
		if( sim == null )		
			readSimulation( canvas );		
		else				
			doGame( canvas );	
		
		if( quantum.getClient().getLog().size() > known_messages )
		{
			for( int i = known_messages; i < quantum.getClient().getLog().size(); i++ )
			{
				text_area.setText( text_area.getText() + "\n" + quantum.getClient().getLog().get(i) );
				known_messages++;
			}
		}
		
		if( loop != null && loop.isGameOver() && !game_over_triggered )
		{
			game_over_triggered = true;
			hideMenu( );
			showGameOverMenu();
		}
		
		try {
			Thread.sleep( 1 );
		} catch (InterruptedException e) {
		}
	}	
	
	private void hideMenu( )
	{
		gui.remove( game_menu );
		game_menu = null;
	}
	
	private void removeChat( )
	{
		if( chat != null )
		{
			gui.remove( chat );
			chat = null;
		}
	}
	
	private void doGame( GLCanvas canvas )
	{							
		if( loop.isGameOver() == false && loop.isDisconnected() && cont == null )
		{
			hideMenu( );
			cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
			cont.setZOrder( 10000 );
			ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!\nUnknown Reason", "Error", new ClickedListener() {
				
				public void clicked(Widget widget) {
					removeBots( );
					quantum.closeServerAndClient();
					quantum.removeDisplayListener( self );
					gui.getCanvas().removeKeyListener( self );
					loop.dispose();
					gui.remove( cont );
					hideMenu( );
					removeChat( );			
					SoundManager.stopAll();
					new StartMenu(quantum, gui);					
				}							
			});
			
			cont.addWidget( dialog );
			gui.add( cont );
			return;			
		}		
		
		loop.update(canvas);			
		loop.render(canvas);
		for( Bot bot: bots )
			bot.update( loop.getSimulation() );
		loop.getGameInterface().setHoverDelay( quantum.getDelay() );		
	}
	
	private void readSimulation( GLCanvas canvas )
	{
		Message msg = null;
		try {
			msg = quantum.getClient().readMessage();
		} catch (Exception e) 
		{					
			Log.println( "[GameMenu] error while reading message from server: " + e.getMessage() );
			hideMenu( );
			cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
			cont.setZOrder( 10000 );
			ConfirmDialog dialog = new ConfirmDialog( gui, "Disconnected!\n" + e.getMessage(), "Error", new ClickedListener() {

				public void clicked(Widget widget) {
					removeBots( );
					quantum.closeServerAndClient();
					quantum.removeDisplayListener( self );
					gui.getCanvas().removeKeyListener( self );
					loop.dispose();
					gui.remove( cont );
					hideMenu( );
					removeChat( );				
					SoundManager.stopAll();
					new StartMenu(quantum, gui);					
				}							
			});
			
			cont.addWidget( dialog );
			gui.add( cont );
			return;			
		}
		
		if( msg != null )
		{
			if( msg instanceof SimulationMessage )
			{
				SimulationMessage sim_msg = (SimulationMessage)msg;
				this.sim = sim_msg.getSimulation();
				this.sim.setClient(quantum.getClient());
				if( loop == null )
					loop = new GameLoop( quantum.getClient(), sim );
				else
					loop.setSimulation( sim );
			}
		}
		
		GL gl = canvas.getGL();
		gl.glClear( GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT );
		ortho_mat.setToOrtho2D(0, 0, canvas.getWidth(), canvas.getHeight() );
		gl.glLoadMatrixf( ortho_mat.toFloatBuffer() );
		gui.getDefaultFont().renderText( 10, canvas.getHeight(), "Awaiting game state" );
	}

	private void removeBots( )
	{
		for( Bot bot: bots )
			bot.dispose( quantum.getClient() );
	}
	
	public void keyPressed(KeyEvent e) 
	{	
		if( e.getKeyCode() == KeyEvent.VK_ESCAPE && save_menu_visible == false )
		{
			if( game_menu == null )
				showMenu( );
			else
				hideMenu( );
				
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }
	
}
