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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import quantum.game.Creature;
import quantum.game.Planet;
import quantum.game.Simulation;
import quantum.gfx.Font;
import quantum.gfx.Renderer;
import quantum.gfx.Font.FontStyle;
import quantum.math.Matrix;
import quantum.math.Vector2D;

import com.sun.opengl.util.Animator;

public class LocalTest extends JFrame implements GLEventListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7583267323751807699L;
	Simulation sim;
	Creature c1;
	Creature c2;
	Renderer renderer;
	Font font;
	Matrix ortho = new Matrix( );
	
	public LocalTest( )
	{		
		sim = new Simulation( true );
		
		for( int i = 0; i < 10; i++ )
		{
			Planet planet = new Planet( sim, new Vector2D( (float)Math.random() * 20000, (float)Math.random() *20000 ), 100,  1, 1, 1, 200 );
			planet.setOwner( 1 );
			sim.addObject( planet );			
			planet.spawnTree();
			planet.spawnTree();
			for( int j = 0; j < 100; j++ )
				planet.spawnCreature();
		}						
		
		GLCapabilities caps = new GLCapabilities();
		caps.setRedBits(8);
    	caps.setGreenBits(8);
    	caps.setBlueBits(8);
    	caps.setAlphaBits(8);
    	caps.setDepthBits(16);
    	caps.setStencilBits(8);
    	caps.setNumSamples( 8 );    	
    	caps.setDoubleBuffered(true);
        GLCanvas canvas = new GLCanvas( caps );      
        canvas.addGLEventListener(this);

        setSize(1024,1024);
        setTitle("CAV-Projekt: JOGL - Beispielszene");        

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
	}
	
	public void display(GLAutoDrawable arg0) 
	{
		
//		arg0.getGL().glEnable( GL.GL_LINE_SMOOTH );
		
		try {
			sim.update();			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		renderer.render( sim, (GLCanvas)arg0 );		
		
		ortho.setToOrtho2D( 0, 0, arg0.getWidth(), arg0.getHeight() );
		GL gl = arg0.getGL();
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadMatrixf( ortho.toFloatBuffer() );
		gl.glMatrixMode( GL.GL_MODELVIEW );
		gl.glLoadIdentity();
		font.renderTextNewLine( 20, arg0.getHeight() - 10, 
						 "fps: " + String.format( "%.2f", renderer.getFramesPerSecond() ) + "\n" +
						 "simulation: " + String.format( "%.2f", sim.getSimulationUpdateTime() ) + " ms\n" +
						 "rendering: " + String.format( "%.2f", renderer.getRenderTime() ) + " ms\n" + 
						 "trees: " + String.format( "%.2f", renderer.getTreeRenderTime() ) + " ms\n" + 
						 "creatures: " + String.format( "%.2f", renderer.getCreatureRenderTime() ) + " ms\n" + 
						 "planets: " + String.format( "%.2f", renderer.getPlanetRenderTime() ) + " ms"	+ "\n\n" +
						 "#object: " + sim.getObjectCount() );		
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	public void init(GLAutoDrawable arg0) 
	{
		renderer = new Renderer( );
//		renderer.useGlow( false );
		font = new Font( "Arial", 16, FontStyle.Plain );		
	}

	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		// TODO Auto-generated method stub
		
	}

	public static void main( String[] argv )
	{
		new LocalTest( ).setVisible( true );
	}
}
