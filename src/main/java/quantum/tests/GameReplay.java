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
package quantum.tests;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import quantum.game.Constants;
import quantum.game.Creature;
import quantum.game.GameLoop;
import quantum.game.GameObject;
import quantum.game.Planet;
import quantum.game.Simulation;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Label;
import quantum.gui.Slider;
import quantum.gui.Spacer;
import quantum.gui.ValueChangedListener;
import quantum.gui.Widget;
import quantum.math.Matrix;
import quantum.math.Vector2D;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.MessageDecoder;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.SimulationMessage;

import com.sun.opengl.util.Animator;

public class GameReplay extends JFrame implements GLEventListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -546682514908903776L;
	private Simulation sim;
	private GameLoop loop;
	private Client client;
	private Gui gui;
	private Label label;
	private GameObject selected = null;	
	private boolean paused = false;
	
	public GameReplay( String file ) throws Exception
	{
		Constants.TURN_TIME = 16;
		DataInputStream in = new DataInputStream( new GZIPInputStream( new FileInputStream( file ) ) );
		PlayerListMessage player_msg = (PlayerListMessage)MessageDecoder.decode( in );
		SimulationMessage sim_msg = (SimulationMessage)MessageDecoder.decode( in );
		client = new Client( "Replay" );
		client.setPlayerList( player_msg );
		sim = sim_msg.getSimulation();
		//sim.setLocalInput( true );
		loop = new GameLoop( client, sim );		
		
		try
		{
			while( in.available() > 0 )
			{			
				CommandBufferMessage msg = (CommandBufferMessage)MessageDecoder.decode(in);						
				sim.enqueueTurnCommandBufferMessage( msg );
			}
		}
		catch( EOFException ex )
		{
			
		}
		in.close();
		
		GLCapabilities caps = new GLCapabilities();
		caps.setRedBits(8);
    	caps.setGreenBits(8);
    	caps.setBlueBits(8);
    	caps.setAlphaBits(8);
    	caps.setDepthBits(16);
    	caps.setStencilBits(8);
    	caps.setDoubleBuffered(true);
        final GLCanvas canvas = new GLCanvas( caps );      
        canvas.addGLEventListener(this);

        canvas.addMouseListener( new MouseAdapter( ) 
        {
			public void mousePressed(MouseEvent e) 
			{							
				selected = null;
				
				Vector2D v = new Vector2D( e.getX(), e.getY() );
				
				for( GameObject object: sim.getGameObjects() )
				{					
					float w_x = loop.getRenderer().getCamera().getWorldToScreenX( object.getPosition().x );
					float w_y = canvas.getHeight() - loop.getRenderer().getCamera().getWorldToScreenY( object.getPosition().y );
					
					if( v.dst( w_x, w_y ) < 10 )
					{
						selected = object;
						break;
					}											
				}
			}		
        	
        });
        
        setSize(1024,1024);
        setTitle("Quantum Replay");        

        getContentPane().add(canvas,BorderLayout.CENTER);

        final Animator animator = new Animator( canvas );
        animator.setRunAsFastAsPossible( true );
        animator.start();
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
            	animator.stop();            	            	
                System.exit(0);
            }
        });
        
        this.setVisible( true );
	}

	Matrix ortho = new Matrix( );
	
	public void display(GLAutoDrawable canvas) {
		GL gl = canvas.getGL();
		gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
		
		try {			
			if( !paused )
				loop.update((GLCanvas)canvas);
			loop.render((GLCanvas)canvas);
			loop.getGameInterface().setIsReplay( true );
			if( selected != null )
			{
				if( selected instanceof Creature )
				{
					Creature creature = (Creature)selected;
					label.setVisible( true );
					float s_x = loop.getRenderer().getCamera().getWorldToScreenX( selected.getPosition().x );
					float s_y = loop.getRenderer().getCamera().getWorldToScreenY( selected.getPosition().y );
					label.setPosition( s_x + 10, s_y - 10 );				
					label.setText( selected.getId() + ": " + selected.getPosition().toString() + "\naction: " + creature.getActionAsString() + "\ntarget: " + sim.getObject( creature.getAttackTarget() ) + "\nvelocity: " + creature.getVelocity() );
				}
				
				if( selected instanceof Planet )
				{
					Planet planet = (Planet)selected;
					label.setVisible( true );
					float s_x = loop.getRenderer().getCamera().getWorldToScreenX( selected.getPosition().x );
					float s_y = loop.getRenderer().getCamera().getWorldToScreenY( selected.getPosition().y );
					label.setPosition( s_x + 10, s_y - 10 );				
					label.setText( selected.getId() + ": " + selected.getPosition().toString() + "\nresources: " + planet.getResources() + "/" + planet.getMaxResources() + "\nis regrowing: " + planet.isRegrowing() + "\n friendly: " + planet.getFriendlyCreatures( planet.getOwner() ) + "\norbiting: " + planet.getOrbitingCreatures() + "\n unborn: " + planet.getUnbornCreatures( ) + "\nenemy: " + planet.getEnemeyCreatures( planet.getOwner() ) );
				}
			}
			else
			{
				label.setVisible( false );
			}
			gui.render();
			
			if( selected != null )
			{
				ortho.setToOrtho2D( 0, 0, canvas.getWidth(), canvas.getHeight() );		
				gl.glLoadIdentity();
				gl.glLoadMatrixf( ortho.toFloatBuffer() );
				
				if( selected instanceof Creature )
				{
					GameObject obj = sim.getObject( ((Creature)selected).getAttackTarget() );
					if( obj != null )
					{
						float w_x = loop.getRenderer().getCamera().getWorldToScreenX( obj.getPosition().x );
						float w_y = loop.getRenderer().getCamera().getWorldToScreenY( obj.getPosition().y );
						
						gl.glColor4f( 1, 0, 0, 1 );
						Widget.renderOutlinedQuad( w_x - 10, w_y + 10, 20, 20 );
					}
				}
				
				float w_x = loop.getRenderer().getCamera().getWorldToScreenX( selected.getPosition().x );
				float w_y = loop.getRenderer().getCamera().getWorldToScreenY( selected.getPosition().y );
				
				gl.glColor4f( 0, 1, 0, 1 );
				Widget.renderOutlinedQuad( w_x - 10, w_y + 10, 20, 20 );
			}
			Thread.sleep( 0 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	public void init(GLAutoDrawable arg0) 
	{
		gui = new Gui( (GLCanvas)arg0 );
		label = new Label( gui, "");
		label.setAdaptTextToWidth( true );
		label.setSize( 200, 50 );
		label.setBorderColor( 1, 1, 1, 1 );
		label.setForegroundColor( 1, 1, 1, 1 );
		label.setBackgroundColor( 0, 0, 0, 0.4f );
		label.setPosition( 10, 100 );
		gui.add( label );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		Button pause = new Button( gui, "Pause" );
		pause.setSize( 75, 25 );		
		h_box.addWidget( pause );
		pause.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{			
				paused = !paused;
				if( !paused )
					((Button)widget).setCaption( "Paused" );
				else
					((Button)widget).setCaption( "Unpaused" );
			}			
		});
		
		Button step = new Button( gui, "Step" );
		step.setSize( 75, 25 );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( step );
		step.setClickedListener( new ClickedListener( ) 
		{
			public void clicked(Widget widget) 
			{
				try {
					sim.update();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}		
		});
		
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( new Label( gui, "Time Step: " ) );
		
		final Slider slider = new Slider( gui, 5, 100, Constants.TURN_TIME );
		slider.setSize( 100, 10 );
		slider.setValueChangedListener( new ValueChangedListener( ) {
			public void valueChanged(Widget widget) 
			{
				Constants.TURN_TIME = (int)slider.getValue();				
			}		
		});
		slider.setBackgroundColor( 1, 1, 1, 0.3f );
		h_box.addWidget( slider );
		h_box.setPosition( 10, 25 );
		gui.add( h_box );
	}

	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main( String[] argv ) throws Exception
	{
		new GameReplay( "faultygames/resource_problem.rec" );
	}
	
}
