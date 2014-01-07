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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import quantum.gfx.Color;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Constants;
import quantum.game.Planet;
import quantum.game.Simulation;
import quantum.gfx.Renderer;
import quantum.gui.Button;
import quantum.gui.CheckBox;
import quantum.gui.ClickedListener;
import quantum.gui.CustomDialog;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Slider;
import quantum.gui.Spacer;
import quantum.gui.TextField;
import quantum.gui.ValueChangedListener;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.gui.WorldAlignementContainer;
import quantum.math.Vector2D;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class Editor implements DisplayListener, MouseListener, MouseMotionListener, KeyListener
{		
	Editor self = this;
	Simulation sim = new Simulation( false );		
	Renderer renderer;
	boolean dragged = false;
	boolean ctrl_pressed = false;
	boolean shift_pressed = false;
	int button = 0;
	Vector2D drag_start;
	Vector2D drag_end;
	Gui gui;
	File last_directory = FileManager.newFile( "dat/maps" );
	
	ArrayList<Planet> selected_planets = new ArrayList<Planet>();	
	ArrayList<WorldAlignementContainer> conts = new ArrayList<WorldAlignementContainer>();
	Vector2D mouse_pos = new Vector2D( );
	Planet last_clicked_planet = null;
	Quantum quantum;
	private boolean no_input = false;
	
	public Editor( final Quantum quantum, final Gui gui )
	{ 				
		this.gui = gui;
		this.quantum = quantum;
		
		//
		// add this as a display and mouse listener
		//
		quantum.addDisplayListener( this );
		gui.getCanvas().addMouseListener( this );
		gui.getCanvas().addMouseMotionListener( this );
		gui.getCanvas().addKeyListener( this );
		
		//
		// create the simulation renderer and the simulation itself
		//
		
		renderer = new Renderer( );
		renderer.setRenderAllPaths( true );
		renderer.setRenderIsStartPlanet( true );
		sim = new Simulation( false );	
		renderer.getCamera().setScale( 20 );
		
		//
		// create the toolbar
		//		
		final ScreenAlignementContainer cont = new ScreenAlignementContainer( gui, HorizontalAlignement.LEFT, VerticalAlignement.TOP );
		final ScreenAlignementContainer cont2 = new ScreenAlignementContainer( gui, HorizontalAlignement.LEFT, VerticalAlignement.BOTTOM );
		final HorizontalBoxContainer toolbar = new HorizontalBoxContainer( gui );					
		
		Button back = new Button( gui, "back" );
		back.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		back.setSize( 50, 24 );	
		back.setClickedListener( new ClickedListener( ) {
			public void clicked(Widget widget) {
				clearSelection( );		
				gui.remove( cont );				
				gui.remove( cont2 );
				gui.getCanvas().removeMouseListener( self );
				gui.getCanvas().removeMouseMotionListener( self );
				gui.getCanvas().removeKeyListener( self );
				quantum.removeDisplayListener( self );				
				renderer.dispose();
				new StartMenu( quantum, gui );
			}			
		});
		
		Button new_b = new Button( gui, "new" );
		new_b.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		new_b.setSize( 50, 24 );
		new_b.setClickedListener( new ClickedListener( ) {			
			public void clicked(Widget widget) 
			{			
				clearSelection( );
				sim.clear( );
			}			
		});
		
		Button open = new Button( gui, "open" );
		open.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		open.setSize( 50, 24 );
		open.setClickedListener( new ClickedListener( ) {			
			public void clicked(Widget widget) 
			{							
				
				JFileChooser fc = new JFileChooser( last_directory );
				fc.setFileFilter( new FileFilter() {
					
					public boolean accept(File pathname) 
					{
						if( pathname.getPath().endsWith( ".map" ) )
							return true;
						else
							return false;
					}
					
					public String getDescription() 
					{					
						return "Quantum map files (*.map)";
					}
					
				});
				if( fc.showOpenDialog( quantum ) == JFileChooser.APPROVE_OPTION )
				{				
					clearSelection( );
					last_directory = fc.getCurrentDirectory();
					try {						
						DataInputStream in = new DataInputStream( new FileInputStream( fc.getSelectedFile() ) );
						sim = new Simulation( false );
						sim.readState(in);
						in.close();
					} catch (Exception e) {		
						Log.println( "[Editor] couldn't save file" );
						gui.showConfirmDialog( "Couldn't save file", "Error" );
						sim = new Simulation( false );
					}
				}
			}			
		});
		
		Button save = new Button( gui, "save" );
		save.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		save.setSize( 50, 24 );
		save.setClickedListener( new ClickedListener( ) {
			
			public void clicked(Widget widget) 
			{			
				if( sim.getPlanets().size() == 0 )
					return;							
				
				for( Planet planet: sim.getPlanets() )
					if( planet.getReachablePlanets().size() == 0 )
					{
						gui.showConfirmDialog( "All planets must be connected to at least one other planet!", "Error" );
						return;
					}
				
				showSaveDialog( );							
			}
			
		});		
		
		Button help = new Button( gui, "Help" );
		help.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		help.setSize(50, 24 );
		help.setClickedListener( new ClickedListener() 
		{			
			public void clicked(Widget widget) 
			{			
				String help_text = "   mouse wheel/key up/down-> zoom\n" +
								   "   right/middle mouse button + drag -> pan\n" +
								   "   middle mouse button/space -> create planet\n" +
								   "   left click -> select planet\n" +
								   "   ctrl + left click -> select another planet\n" +
								   "   left click + drag -> select multiple planets\n" + 								   
								   "\n\nWhen multiple planets are selected, changing the property of one planet automatically changes the property of all other selected planets accordingly\n\n" +
								   "Holding down shift and clicking on two planets will create a path between them or delete an already established path";
				gui.showConfirmDialog( help_text , "Editor Usage", 500 );
			}			
		});			
		
		toolbar.addWidget(new_b);
		toolbar.addWidget(open);
		toolbar.addWidget(save);
		toolbar.addWidget(help);
		toolbar.addWidget(back);
		toolbar.addWidget( new Spacer( gui, 50, 0 ) );
		cont.addWidget( toolbar );
		gui.add( cont );	
		
		Button triangulate = new Button( gui, "Calculate Paths" );
		triangulate.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		triangulate.setSize( 120, 24 );
		triangulate.setClickedListener( new ClickedListener( ) 
		{

			public void clicked(Widget widget) {
				sim.calculatePaths();				
			}			
		} );
		
		Button clear = new Button( gui, "Clear Paths" );
		clear.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		clear.setSize( 120, 24 );
		clear.setClickedListener( new ClickedListener( ) 
		{

			public void clicked(Widget widget) {
				for( Planet planet: sim.getPlanets() )
					planet.getReachablePlanets().clear();
			}			
		} );
		
		HorizontalBoxContainer toolbar2 = new HorizontalBoxContainer( gui );
		toolbar2.addWidget( triangulate );	
		toolbar2.addWidget( clear );
		cont2.addWidget( toolbar2 );
		gui.add( cont2 );
	}
	
	public void showSaveDialog( )
	{
		no_input = true;
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		v_box.addWidget( new Label( gui, "Author" ) );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );		
		final TextField author = new TextField( gui );
		if( sim.getAuthor().equals( "" ) == false )
			author.setEnabled( false );
		author.setText( sim.getAuthor() );
		author.setSize( 280, 25 );
		v_box.addWidget( author );
		
		v_box.addWidget( new Label( gui, "Map Name" ) );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		final TextField name = new TextField( gui );
		name.setText( sim.getName() );
		name.setSize( 280, 25 );
		v_box.addWidget( name );
		
		v_box.addWidget( new Label( gui, "Map Description" ) );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		final TextField description = new TextField( gui );
		description.setText( sim.getDescription() );
		description.setSize( 280, 25 );
		v_box.addWidget( description );
		
		Button ok = new Button( gui, "Ok" );
		Button cancel = new Button( gui, "Cancel" );
		ok.setSize( 70, 25 );
		cancel.setSize( 70, 25 );
		
		final CustomDialog dialog = new CustomDialog( gui, 300, "Map Information", v_box, ok, cancel );
		gui.showCustomDialog( dialog );
		
		
		ok.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{
				no_input = false;
				gui.removeCustomDialog( dialog );
				
				if( name.getText().equals( "" ) )
				{
					gui.showConfirmDialog( "You have to enter a map name!" , "Error", new ClickedListener( ) {

						public void clicked(Widget widget) {
							showSaveDialog( );							
						}						
					});
					return;
				}
				
				if( author.getText().equals( "" ) )
				{
					gui.showConfirmDialog( "You have to enter a map author!" , "Error", new ClickedListener( ) {

						public void clicked(Widget widget) {
							showSaveDialog( );							
						}						
					});
					return;
				}
				
				if( description.getText().equals( "" ) )
				{
					gui.showConfirmDialog( "You have to enter a map description!" , "Error", new ClickedListener( ) {

						public void clicked(Widget widget) {
							showSaveDialog( );							
						}						
					});
					return;
				}

				sim.setAuthor( author.getText() );
				sim.setName( name.getText() );
				sim.setDescription( description.getText() );
				
				JFileChooser fc = new JFileChooser( last_directory );
				fc.setFileFilter( new FileFilter() {

					@Override
					public boolean accept(File pathname) 
					{
						if( pathname.getPath().endsWith( ".map" ) )
							return true;
						else
							return false;
					}

					@Override
					public String getDescription() 
					{					
						return "Quantum map files (*.map)";
					}
					
				});
				if( fc.showSaveDialog( quantum ) == JFileChooser.APPROVE_OPTION )
				{									
					last_directory = fc.getCurrentDirectory();
					try {						
						DataOutputStream out = new DataOutputStream( new FileOutputStream( fc.getSelectedFile() ) );
						sim.writeState(out);						 					
						out.close();
					} catch (Exception e) {			
						Log.println( "[Editor] couldn't save file" );
						gui.showConfirmDialog( "Couldn't save file", "Error" );
					}
				}
				
			}
			
		});
		
		cancel.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) {
				no_input = false;
				gui.removeCustomDialog( dialog );				
			}			
		});
	}
	
	public void display(GLCanvas canvas) 
	{			
		GL gl = canvas.getGL();		
		
		// 
		// render the simulation
		// 
		try {
			sim.update();			
		} catch (Exception e) {
			Log.println( "[Editor] exception in simulation update/calculate paths: " + e.getMessage() );
			e.printStackTrace();
		}
		renderer.render( sim, canvas);
		
		
		//
		// render the grid
		//	
		gl.glColor4f( 0.4f, 0.4f, 0.4f, 0.4f );
		gl.glEnable( GL.GL_BLEND );
		gl.glBegin( GL.GL_LINES );
		for( int x = 0; x < 100; x++ )
		{
			gl.glVertex2f( -50 * Constants.PLANET_MAX_SIZE * 4 + x * Constants.PLANET_MAX_SIZE * 4, -50 * Constants.PLANET_MAX_SIZE * 4 );
			gl.glVertex2f( -50 * Constants.PLANET_MAX_SIZE * 4 + x * Constants.PLANET_MAX_SIZE * 4, 50 * Constants.PLANET_MAX_SIZE * 4 );
		}
		
		for( int x = 0; x < 100; x++ )
		{
			gl.glVertex2f( -50 * Constants.PLANET_MAX_SIZE * 4, -50 * Constants.PLANET_MAX_SIZE * 4 + x * Constants.PLANET_MAX_SIZE * 4);
			gl.glVertex2f( +50 * Constants.PLANET_MAX_SIZE * 4, -50 * Constants.PLANET_MAX_SIZE * 4 + x * Constants.PLANET_MAX_SIZE * 4);
		}
		gl.glDisable( GL.GL_BLEND );
		gl.glEnd( );
		
		//
		// render rectangle selection
		//		
		if( drag_start != null && drag_end != null && selected_planets.size() == 0 )
		{
			gl.glLineWidth( 2 );
			gl.glColor4f( 1, 1, 1, 1 );
			Widget.renderOutlinedQuad( drag_start.x, drag_start.y, drag_end.x - drag_start.x, -(drag_end.y - drag_start.y) );
			gl.glLineWidth( 1 );
		}
		
		//
		// render reference system
		//	
		gl.glColor3f( 1, 1, 1 );
		gl.glBegin( GL.GL_LINES );
			gl.glColor3f( 1, 0, 0 );
			gl.glVertex2f( Constants.PLANET_MAX_SIZE * 50, 0 );
			gl.glVertex2f( -Constants.PLANET_MAX_SIZE * 50, 0 );
			gl.glColor3f( 0, 1, 0 );
			gl.glVertex2f( 0, Constants.PLANET_MAX_SIZE * 50 );
			gl.glVertex2f( 0, -Constants.PLANET_MAX_SIZE * 50 );
		gl.glEnd( );
		
		try {
			Thread.sleep( 5 );
		} catch (InterruptedException e) {
		}
		
		//
		// check mouse over planet
		//
		if( sim.getPlanet( renderer.getCamera().getScreenToWorldX(mouse_pos.x), renderer.getCamera().getScreenToWorldY(mouse_pos.y) ) != null &&
			!this.wasGuiIntersected( mouse_pos.x, mouse_pos.y ) && drag_start != null )
			for( WorldAlignementContainer cont: conts )
				cont.setVisible( false );
		else
			for( WorldAlignementContainer cont: conts )
				cont.setVisible( true );
			
	}

	public void mouseClicked(MouseEvent e) 
	{		
			
	}
	
	private void clearSelection( )
	{
		selected_planets.clear( );
		for( WorldAlignementContainer cont: conts )
			gui.remove( cont );
		conts.clear( );
	}
	
	private boolean wasGuiIntersected( float x, float y )
	{
		return gui.getWidget( x, y ) != null;			
	}
	
	private void createPlanetGui( final Planet planet )
	{
		WorldAlignementContainer cont = new WorldAlignementContainer( gui, renderer.getCamera(), planet.getPosition() );
		cont.setBackgroundColor( new Color( 0, 0, 0, 0.5f ) );
		Label label_strength = new Label( gui, "Strength" );
		Slider slider_strength = new Slider( gui, 0.1f, 1, 0.5f );
		Label label_health = new Label( gui, "Health" );
		Slider slider_health = new Slider( gui, 0.1f, 1, 0.5f );
		Label label_speed = new Label( gui, "Speed" );		
		Slider slider_speed = new Slider( gui, Constants.BOID_MIN_SPEED, 1, 0.5f );
		Label label_resources = new Label( gui, "Resources" );
		Slider slider_resources = new Slider( gui, 10, Constants.PLANET_MAX_CREATURES, 20 );
		CheckBox start_planet = new CheckBox( gui, "Start Planet" );
		
		slider_strength.setSize( 100, 8 );
		slider_health.setSize( 100, 8 );
		slider_speed.setSize( 100, 8 );
		slider_resources.setSize( 100, 8 );
		
		Color bg = new Color( 0.3f, 0.3f, 0.3f, 1 );
		slider_strength.setBackgroundColor( bg );
		slider_health.setBackgroundColor( bg );
		slider_speed.setBackgroundColor( bg );
		slider_resources.setBackgroundColor( bg );
		
		slider_strength.setValue(planet.getStrength());
		slider_health.setValue(planet.getHealth());
		slider_speed.setValue(planet.getSpeed() );
		slider_resources.setValue( planet.getMaxResources() );
		
		start_planet.setChecked( planet.isStartPlanet() );
		start_planet.setSize( 150, 24 );
		
		cont.addWidget( label_strength );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( slider_strength );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( label_health );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( slider_health );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( label_speed );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( slider_speed );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( label_resources );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( slider_resources );
		cont.addWidget( new Spacer( gui, 0, 3 ) );
		cont.addWidget( start_planet  );
		cont.addWidget( new Spacer( gui, slider_resources.getWidth() + 20, 10 ) );
		gui.add( cont );
		conts.add( cont );
		
		slider_strength.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{	
				for( Planet planet: selected_planets )
					planet.setStrength( ((Slider)widget).getValue() );		
				
				for( WorldAlignementContainer cont: conts )
				{
					VerticalBoxContainer v_box = (VerticalBoxContainer)cont.getWidgets().get(0);					
					((Slider)v_box.getWidgets().get(2)).setValue(((Slider)widget).getValue());
				}
			}			
		});
		
		slider_health.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) {
				for( Planet planet: selected_planets )
					planet.setHealth( ((Slider)widget).getValue() );
				
				for( WorldAlignementContainer cont: conts )
				{
					VerticalBoxContainer v_box = (VerticalBoxContainer)cont.getWidgets().get(0);					
					((Slider)v_box.getWidgets().get(6)).setValue(((Slider)widget).getValue());
				}
			}
			
		} );
		
		slider_speed.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{
				for( Planet planet: selected_planets )
					planet.setSpeed( ((Slider)widget).getValue() );
				
				for( WorldAlignementContainer cont: conts )
				{
					VerticalBoxContainer v_box = (VerticalBoxContainer)cont.getWidgets().get(0);					
					((Slider)v_box.getWidgets().get(10)).setValue(((Slider)widget).getValue());
				}
			}			
		});
		
		slider_resources.setValueChangedListener( new ValueChangedListener( ) {

			public void valueChanged(Widget widget) 
			{
				for( Planet planet: selected_planets )
					planet.setResources( (int)((Slider)widget).getValue() );
				
				for( WorldAlignementContainer cont: conts )
				{
					VerticalBoxContainer v_box = (VerticalBoxContainer)cont.getWidgets().get(0);					
					((Slider)v_box.getWidgets().get(14)).setValue(((Slider)widget).getValue());
				}
			}			
		});
		
		start_planet.setClickedListener( new ClickedListener( ) {
			
			public void clicked(Widget widget) {
				for( Planet planet: selected_planets )
					planet.setStartPlanet( ((CheckBox)widget).isChecked() );
				
				for( WorldAlignementContainer cont: conts )
				{
					VerticalBoxContainer v_box = (VerticalBoxContainer)cont.getWidgets().get(0);					
					((CheckBox)v_box.getWidgets().get(16)).setChecked(((CheckBox)widget).isChecked());
				}
			}
			
		});
	}

	public void mouseEntered(MouseEvent e) 
	{	
		
	}

	public void mouseExited(MouseEvent e) 
	{	
		
	}

	public void mousePressed(MouseEvent e) 
	{			
		if( no_input ) 
			return;
		
		if( e.getButton() != MouseEvent.BUTTON1 )
			return;
		
		if( wasGuiIntersected(e.getX(), e.getY() ) )
			return;
		
		float x = renderer.getCamera().getScreenToWorldX( e.getX() );
		float y = renderer.getCamera().getScreenToWorldY( e.getY() );
		Planet planet = sim.getPlanet(x, y);	
		
		if( shift_pressed )
		{
			if(  planet != null && last_clicked_planet != planet && last_clicked_planet != null )
			{
				if( planet.getReachablePlanets().contains( last_clicked_planet.getId() ) )
				{
					planet.getReachablePlanets().remove( last_clicked_planet.getId() );
					last_clicked_planet.getReachablePlanets().remove( planet.getId() );
				}
				else
				{
					planet.getReachablePlanets().add( last_clicked_planet.getId() );
					last_clicked_planet.getReachablePlanets().add( planet.getId() );
				}
				
				last_clicked_planet = null;
			}
			else
				last_clicked_planet = planet;
			return;
		}					
		
		if( planet == null )
		{
			clearSelection();
			button = e.getButton();
			return;
		}			
		
		if( planet != null && selected_planets.contains( planet ) == false )
		{								
			if( ctrl_pressed == false )
				clearSelection( );
			
			selected_planets.add( planet );
			createPlanetGui( planet );		
		}			
				
		button = e.getButton();
	}
	
	public void mouseReleased(MouseEvent e) 
	{	
		if( no_input ) 
			return;
		
		if( shift_pressed )
			return;
		
		if( dragged )
		{
			if( drag_end != null )
			{
				clearSelection( );
				float x = Math.min( drag_start.x, drag_end.x );
				float y = Math.min( drag_start.y, drag_end.y );
				float width = Math.abs( drag_end.x - drag_start.x );
				float height = Math.abs( drag_end.y - drag_start.y );
				
				for( Planet planet: sim.getPlanets() )
					if( planet.getPosition().x > x && planet.getPosition().x < x + width &&
						planet.getPosition().y > y && planet.getPosition().y < y + height )
						if( selected_planets.contains( planet ) == false )
						{
							selected_planets.add( planet );
							createPlanetGui( planet );	
						}										
			}
			
			drag_start = null;
			drag_end = null;
		}
		
		if( e.getButton() == MouseEvent.BUTTON2 )
		{
			clearSelection( );
			float x = renderer.getCamera().getScreenToWorldX( e.getX() );
			float y = renderer.getCamera().getScreenToWorldY( e.getY() );
			sim.addObject( new Planet( sim, new Vector2D( x, y ), 100, 1, 1, 1, 50 ) );
		}	
		
		button = 0;
	}

	public void mouseDragged(MouseEvent e) 
	{					
		if( no_input ) 
			return;
		
		mouse_pos.set( e.getX(), e.getY() );
		
		if( button == 0 )
			return;
		
		dragged = true;
		if( drag_start == null )
		{
			drag_start = new Vector2D( renderer.getCamera().getScreenToWorldX( e.getX() ),
									   renderer.getCamera().getScreenToWorldY( e.getY()) );
		}
		
		if( button == MouseEvent.BUTTON1 )
		{
			if( selected_planets.size() > 0 )
			{
				Vector2D displacement = new Vector2D( renderer.getCamera().getScreenToWorldX( e.getX() ),
						   						renderer.getCamera().getScreenToWorldY( e.getY()) );
				
				displacement.sub( drag_start );
				
				for( Planet planet: selected_planets )
					planet.getPosition().add( displacement );
				
				drag_start.set(  new Vector2D( renderer.getCamera().getScreenToWorldX( e.getX() ),
						   						renderer.getCamera().getScreenToWorldY( e.getY()) ) ); 
			}
			else
			{
				drag_end = new Vector2D( renderer.getCamera().getScreenToWorldX( e.getX() ),
										 renderer.getCamera().getScreenToWorldY( e.getY()) );
			}
		}				
	}

	public void mouseMoved(MouseEvent e) {
		mouse_pos.set( e.getX(), e.getY() );
		
	}

	public void keyPressed(KeyEvent e) {
		if( no_input ) 
			return;
		
		// TODO Auto-generated method stub
		if( e.getKeyCode() == KeyEvent.VK_CONTROL )
			ctrl_pressed = true;
		
		if( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			shift_pressed = true;
			for( WorldAlignementContainer cont: conts )
			{
				gui.remove( cont );				
			}
			conts.clear();
		}
		
		
		if( e.getKeyCode() == KeyEvent.VK_SPACE )
		{
			clearSelection( );
			float x = renderer.getCamera().getScreenToWorldX( renderer.getCamera().getMousePosition().x  );
			float y = renderer.getCamera().getScreenToWorldY( renderer.getCamera().getMousePosition().y  );
			sim.addObject( new Planet( sim, new Vector2D( x, y ), 100, 1, 1, 1, 50 ) );	
		}
	}

	public void keyReleased(KeyEvent e) 
	{	
		if( no_input ) 
			return;
		
		if( e.getKeyCode() == KeyEvent.VK_CONTROL )
			ctrl_pressed = false;
		
		if( e.getKeyCode() == KeyEvent.VK_DELETE )
		{
			for( Planet planet: selected_planets )
				sim.removeObject( planet );
			clearSelection( );						 	
		}
		
		if( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			for( Planet p: selected_planets )
			{
				createPlanetGui( p );
			}
			shift_pressed = false;
			last_clicked_planet = null;
		}
	}

	public void keyTyped(KeyEvent e) 
	{
			
	}
}
