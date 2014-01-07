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
package quantum.gfx;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;

import quantum.math.Bounds;
import quantum.math.Matrix;
import quantum.math.Ray;
import quantum.math.Vector;
import quantum.math.Vector2D;

import quantum.game.GameInterface;


/**
 * a simple ortho camera looking along negative z
 * having a scaling parameter for zooming
 * 
 * @author mzechner@know-center.at
 *
 */
public class OrthoCamera implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	protected final Vector pos = new Vector();
	protected float scale = 1.0f;	
	protected float scale_vel = 0;
	protected float scale_dir = 1;
	protected final Matrix proj = new Matrix();
	protected final Matrix model = new Matrix();
	protected final Matrix combined = new Matrix();
	protected final Bounds bounds = new Bounds();
	protected float width = 0;
	protected float height = 0;
	protected boolean interpolating = false;
	protected double interpolation_start_time = 0;
	protected double interpolation_duration = 0;
	protected final Vector start_pos = new Vector();
	protected float start_scale = 1;
	protected final Vector target_pos = new Vector();
	protected float target_scale = 1;	
	protected final Vector dir = new Vector();
	protected float near = -100;
	protected float far = 100;
	protected HashSet<Integer> keys = new HashSet<Integer>();
	boolean input_disabled = false;
	
	
	public OrthoCamera( float x, float y, float scale )
	{
		this.pos.set( pos );
		this.scale = scale;
	}
	
	public void setInputDisabled( boolean disabled )
	{
		input_disabled = disabled;
	}
	
	public void setScale( float scale )
	{
		this.scale = scale;
	}
	
	public float getScale( )
	{
		return scale;
	}
	
	public void setPosition( Vector pos )
	{
		this.pos.set( pos );
	}
	
	public Vector getPosition( )
	{
		return pos;
	}
	
	public void setNear( float near )
	{
		this.near = near;		
	}
	
	public float getNear( )
	{
		return this.near;
	}
	
	public void setFar( float far )
	{
		this.far = far;
	}
	
	public float getFar( )
	{
		return this.far;
	}
	
	public void update( int width, int height )
	{		
		this.width = width;
		this.height = height;
		
		proj.setToOrtho2D(0, 0, (width * scale), (float)(height * scale), near, far );
		model.idt();
		model.setToTranslation( new Vector( (float)(-pos.getX() + (width / 2) * scale), (float)(-pos.getY() + (height / 2) * scale), (float)(-pos.getZ()) ) );
		combined.set( proj );
		combined.mul( model );
					
		GL gl = GLContext.getCurrent().getGL();
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadIdentity();
		gl.glMatrixMode( GL.GL_MODELVIEW );
		gl.glLoadMatrixf(combined.toFloatBuffer());		
	}
	
	public void update( GLCanvas canvas )
	{
		width = canvas.getWidth();
		height = canvas.getHeight();
		
		proj.setToOrtho2D(0, 0, (canvas.getWidth() * scale), (float)(canvas.getHeight() * scale), near, far );
		model.idt();
		model.setToTranslation( new Vector( (float)(-pos.getX() + (canvas.getWidth() / 2) * scale), (float)(-pos.getY() + (canvas.getHeight() / 2) * scale), (float)(-pos.getZ()) ) );
		combined.set( proj );
		combined.mul( model );
					
		GL gl = canvas.getGL();
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadIdentity();
		gl.glMatrixMode( GL.GL_MODELVIEW );
		gl.glLoadMatrixf(combined.toFloatBuffer());					
	}
	
	float zoom_force = 0;
	float zoom_speed = 0;
	float t_x = 0;
	float t_y = 0;
	boolean installed = false;
	GLCanvas canvas;
	long start_time = System.nanoTime();
	
	public void dispose( )
	{
		if( canvas != null )
		{
			canvas.removeMouseListener( this );
			canvas.removeMouseMotionListener( this );
			canvas.removeMouseWheelListener( this );
			canvas.removeKeyListener( this );
		}
	}
	
	public void update( GLCanvas canvas, boolean update_matrices )
	{			
		float elapsed_seconds = ( System.nanoTime() - start_time ) / 1000000000.0f;
		width = canvas.getWidth();
		height = canvas.getHeight();
		
		if( !installed )
		{
			canvas.addMouseListener( this );
			canvas.addMouseMotionListener( this );
			canvas.addMouseWheelListener( this );
			canvas.addKeyListener( this );
			this.canvas = canvas;
			installed = true;
		}
		
		poll();
				
		if( !interpolating )
		{
			
			if( input_disabled == false )
			{
				if( isMouseButtonPressed( MouseEvent.BUTTON1 ) && GameInterface.getSelectedHandle() == null )
				{			
					pos.add( (float)(-getMouseDelta().x * scale), (float)(getMouseDelta().y * scale), 0 );			
				}
							
				zoom_force = 0;
				int w = getMouseWheel();
				if( w != 0 )
				{			
					scale_dir = w < 0? -1: 1; 
					scale_vel = 0.1f;
					zoom_force = (scale_vel * scale_dir);										
				}		
				else
				{
					if( keys.contains( KeyEvent.VK_UP ) )
					{
						scale_dir = -1;
						scale_vel = 0.03f;
						zoom_force = (scale_vel * scale_dir);
					}
					if( keys.contains( KeyEvent.VK_DOWN ))
					{					
						scale_dir = 1;
						scale_vel = 0.03f;
						zoom_force = (scale_vel * scale_dir);				
					}
						
				}
								
				
				zoom_speed += zoom_force * Math.log( scale + 2);
				zoom_speed = Math.min( zoom_speed, 0.1f * scale );
				
				float old_x = getScreenToWorldX( getMousePosition().x );
				float old_y = getScreenToWorldY( getMousePosition().y );
				
				scale += zoom_speed * elapsed_seconds * 40;
				if( scale < 1 )
					scale = 1f;
				
				if( zoom_speed != 0 )
				{
					float x = getScreenToWorldX( getMousePosition().x );
					float y = getScreenToWorldY( getMousePosition().y );
					
					if( zoom_speed < 0 )
						pos.add( old_x - x, old_y - y, 0 );
					else
						pos.add( x - old_x, y - old_y, 0 );
				}
				
				zoom_speed *= 0.93f;
				if( Math.abs(zoom_speed) < 0.00001  )			
					zoom_speed = 0;		
			}
											
		}
		else		
			updateInterpolation();		
		
		proj.setToOrtho2D(0, 0, (canvas.getWidth() * scale), (float)(canvas.getHeight() * scale), near, far );
		model.idt();
		model.setToTranslation( new Vector( (float)(-pos.getX() + (canvas.getWidth() / 2) * scale), (float)(-pos.getY() + (canvas.getHeight() / 2) * scale), (float)(-pos.getZ()) ) );
		combined.set( proj );
		combined.mul( model );
		
		
		if( update_matrices )
		{		
			GL gl = canvas.getGL();
			gl.glMatrixMode( GL.GL_PROJECTION );
			gl.glLoadIdentity();
			gl.glMatrixMode( GL.GL_MODELVIEW );
			gl.glLoadMatrixf(combined.toFloatBuffer());
		}
		
		calculateBounds();	
		start_time = System.nanoTime();
	}	
	
	private void updateInterpolation( )
	{
		double curr_time = System.nanoTime() / 1000000000.0;
		
		if( curr_time > interpolation_start_time + interpolation_duration )
		{
			interpolating = false;
			pos.set( target_pos );
			scale = target_scale;
			return;
		}
		
		float alpha = (float)(( curr_time - interpolation_start_time ) / interpolation_duration);
		
		scale = start_scale * ( 1 - alpha ) + ( alpha ) * target_scale;		
		pos.set( start_pos ).add( dir.tmp().mul( alpha ));
	}
	
	public void moveToTarget( float x, float y, float scale, float duration )
	{
		target_pos.setX(x);
		target_pos.setY(y);
		target_scale = scale;
		
		start_pos.set( pos );
		start_scale = this.scale;
		
		dir.set( target_pos ).sub( start_pos );
		
		interpolation_start_time = System.nanoTime() / 1000000000.0;
		interpolation_duration = duration;
		interpolating = true;
	}
	
	public Matrix getProjectionMatrix( )
	{
		return proj;
	}
	
	public Matrix getModelMatrix( )
	{
		return model;
	}
	
	public Matrix getCombinedMatrix( )
	{
		return combined;
	}	
	
	protected void calculateBounds()
	{
		bounds.inf();
		bounds.ext(pos.cpy().add( (float)(width * scale / 2), (float)(height * scale / 2), 0 ));
		bounds.ext(pos.cpy().add( (float)(width * scale / -2), (float)(height * scale / -2), 0 ) );
	}
	
	public Bounds getBounds( )
	{		
		return bounds;
	}
	
	public float getScreenToWorldX( float mouse_x )
	{
		return  ( mouse_x * scale ) - ( width * scale ) / 2 + pos.getX();
	}
	
	public int getWorldToScreenX( float world_x )
	{
		return (int)((world_x + ( width * scale ) / 2 - pos.getX()) / scale);
	}	
	
	public float getScreenToWorldY( float mouse_y )
	{
		return ( (height - mouse_y-1) * scale ) - ( height * scale ) / 2 + pos.getY();
	}
	
	public int getWorldToScreenY( float world_y )
	{
		return (int)(-( -world_y + (height * scale ) / 2 + pos.getY() - height * scale ) / scale); 
	}
	
	public boolean visible( Vector2D pos, float radius )
	{
		float x = getWorldToScreenX( pos.x );
		float r = getWorldToScreenX( pos.x + radius ) - x;
		float y = getWorldToScreenY( pos.y );
				
		if( x + r > 0 && x - r < canvas.getWidth() && y + r > 0 && y - r < canvas.getHeight() )
			return true;
		else
			return false;
	}
	
	public Ray getPickRay( int mouse_x, int mouse_y )
	{
		float x = getScreenToWorldX( mouse_x );
		float y = getScreenToWorldY( mouse_y );		
		return new Ray( new Vector( x, y, 10000 / 2 ), new Vector( 0, 0, -1 ) );
	}
	
	public float getWidth( )
	{
		return width;
	}
	
	public float getHeight( )
	{
		return height;
	}

	// define last mouse position
    private Point last_mouse_pos = new Point( 0, 0 );
    // define mouse delta
    private Point mouse_delta = new Point( 0, 0 );
    // define mouse wheel
    private int mouse_wheel = 0;
    // define mouse position
    private Point mouse_pos = new Point( 0, 0 );    
    // define mouse button
    private int mouse_buttons = 0;
	
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent arg0)
	{	
		mouse_buttons |= arg0.getButton();		
	}

	protected void poll( )
	{
        synchronized( mouse_pos )
        {
        	mouse_delta.x = mouse_pos.x - last_mouse_pos.x;
        	mouse_delta.y = mouse_pos.y - last_mouse_pos.y;
        	last_mouse_pos.setLocation(mouse_pos);           	
        }          
	}
	
	public void mouseReleased(MouseEvent arg0)
	{			
		mouse_buttons &= ~arg0.getButton();		
	}
	
	
	public void mouseDragged(MouseEvent arg0)
	{		
		synchronized( mouse_pos )
		{
			mouse_pos.setLocation( arg0.getPoint() );
		}
	}

	
	public void mouseMoved(MouseEvent arg0)
	{				
		synchronized( mouse_pos )
		{			
			mouse_pos.setLocation( arg0.getPoint() );			
		}
	}
	
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		mouse_wheel = arg0.getWheelRotation();		
	}	
	
	/**
	 * returns wheter the given button is pressed.
	 * use the MouseEvent.BUTTONX enumeration for
	 * specifying the button.
	 * @param button
	 * @return
	 */
	public boolean isMouseButtonPressed( int button )
	{
		synchronized( mouse_pos )
		{
			return (mouse_buttons & button) != 0;
		}
	}
	
	/**
	 * returns the mouse position relative to the 
	 * gl canvas
	 * @return
	 */
	public Point getMousePosition( )
	{
		synchronized( mouse_pos )
		{			
			return mouse_pos;
		}
	}
	
	public Point getMouseDelta( )
	{
		synchronized( mouse_pos )
		{
			return mouse_delta;
		}
	}
	
	public int getMouseWheel( )
	{
		int w = mouse_wheel;
		mouse_wheel = 0;
		return w;
	}

	public void keyPressed(KeyEvent e) 
	{
		keys.add( e.getKeyCode() );
		
	}

	public void keyReleased(KeyEvent e) 
	{
		keys.remove( e.getKeyCode() );
		
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
