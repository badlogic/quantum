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

import java.io.File;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.ConfirmDialog;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Spacer;
import quantum.gui.VerticalAlignement;
import quantum.gui.Widget;
import quantum.net.AutoUpdater;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class UpdateMenu implements DisplayListener 
{
	AutoUpdater updater;
	Quantum quantum;
	Gui gui;
	UpdateMenu self = this;
	Label progress_label;
	ScreenAlignementContainer cont2;
	ScreenAlignementContainer cont3;
	
	public UpdateMenu( final Quantum quantum, final Gui gui )	
	{
		this.gui = gui;
		this.quantum = quantum;
		quantum.addDisplayListener( this );		
		gui.getCanvas().getContext().makeCurrent();
		cont2 = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.TOP );
		cont3 = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
		Image image;
		try {
			image = new Image( gui, FileManager.readFile( "quantum.png" ) );
			cont2.addWidget( new Spacer( gui, 0, 50 ) );
			cont2.addWidget( image );
		} catch (Exception e) {
			Log.println( "[StartMenu] couldn't load image 'quantum.png'" );
			gui.showConfirmDialog( "Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error" );			
		}
		
		try {
			updater = new AutoUpdater( );
		} catch (Exception e) {
			final ScreenAlignementContainer c = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
			ConfirmDialog dialog = new ConfirmDialog( gui, "Couldn't connect to update site!", "Error", new ClickedListener() {

				public void clicked(Widget widget) {
					gui.remove( cont2 );
					gui.remove( cont3 );					
					gui.remove( c );
					quantum.removeDisplayListener( self );					
					new StartMenu(quantum, gui);					
				}							
			});
			dialog.setBackgroundColor( 0, 0, 0, 1 );
			c.addWidget( dialog );					
			gui.add( c );
			return;
		}
				
		Label version = new Label( gui, updater.updateAvailable()?"Update available!":"Version up to date" );
		cont3.addWidget( version, HorizontalAlignement.CENTER );
		cont3.addWidget( new Spacer( gui, 0, 10 ) );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		Button back = new Button( gui, "Back" );
		back.setSize( 70, 25 );
		h_box.addWidget( back );
		h_box.addWidget( new Spacer( gui, 5, 0 ) );
		cont3.addWidget( h_box, HorizontalAlignement.CENTER );
		back.setClickedListener( new ClickedListener( ) {
			public void clicked(Widget widget) 
			{
				gui.remove( cont2 );	
				gui.remove( cont3 );					
				quantum.removeDisplayListener( self );					
				new StartMenu(quantum, gui);				
			}			
		});
		
		if( updater.updateAvailable() )
		{
			Button update = new Button( gui, "Update" );
			update.setSize( 70, 25 );
			h_box.addWidget( update );
			update.setClickedListener( new ClickedListener( ) {

				public void clicked(Widget widget) 
				{				
					cont3.clear();									
					progress_label = new Label( gui, "Updating, please wait..." );
					cont3.addWidget( progress_label );					
					try {
						updater.update();
					} catch (Exception e) {
						updater = null;
						final ScreenAlignementContainer c = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
						ConfirmDialog dialog = new ConfirmDialog( gui, "Update failed!", "Error", new ClickedListener() {

							public void clicked(Widget widget) {
								gui.remove( cont2 );
								gui.remove( cont3 );					
								gui.remove( c );
								quantum.removeDisplayListener( self );					
								new StartMenu(quantum, gui);					
							}							
						});
						dialog.setBackgroundColor( 0, 0, 0, 1 );
						c.addWidget( dialog );					
						gui.add( c );		
												
						return;
					}
				}				
			});
		}
		
		gui.add( cont2 );
		gui.add( cont3 );
	}

	public void display(GLCanvas canvas) 
	{
		if( updater != null )
		{
			if( updater.isUpdating() )
			{
				progress_label.setText("Updating, please wait ... " +  updater.readSize() / 1024 + "kb / " + updater.totalSize() / 1024 + "kb");
			}	
			
			if( updater.updateDone() )
			{							
				gui.remove( cont3 );
				if( updater.updateFailed() )
				{
					updater = null;
					
					final ScreenAlignementContainer c = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
					ConfirmDialog dialog = new ConfirmDialog( gui, "Update failed!", "Information", new ClickedListener() {

						public void clicked(Widget widget) {
							gui.remove( cont2 );
							gui.remove( cont3 );					
							gui.remove( c );
							quantum.removeDisplayListener( self );					
							new StartMenu(quantum, gui);					
						}							
					});
					dialog.setBackgroundColor( 0, 0, 0, 1 );
					c.addWidget( dialog );					
					gui.add( c );			
					return;
				}
				else
				{
					updater = null;
					final ScreenAlignementContainer c = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
					ConfirmDialog dialog = new ConfirmDialog( gui, "Update successfull! Click OK to restart Quantum.", "Information", new ClickedListener() {

						public void clicked(Widget widget) {
							gui.remove( cont2 );
							gui.remove( cont3 );					
							gui.remove( c );
							quantum.removeDisplayListener( self );
							try
							{
								if( new File( "jre" ).exists() )
									new ProcessBuilder( "jre/bin/javaw", "-server", "-jar", "quantum.jar" ).start();
								else
									new ProcessBuilder( "javaw", "-jar", "quantum.jar" ).start();
							}
							catch( Exception ex )
							{
								Log.println( "[UpdateMenu] couldn't restart quantum: " + Log.getStackTrace( ex ) );
							}
							System.exit(0);					
						}							
					});
					dialog.setBackgroundColor( 0, 0, 0, 1 );
					c.addWidget( dialog );					
					gui.add( c );		
					return;
				}							
			}
		}
	}
	
	
}
