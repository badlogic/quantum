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



import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;


import quantum.gfx.Color;
import quantum.gfx.Font;
import quantum.gfx.Font.FontStyle;
import quantum.math.Matrix;
import quantum.math.Vector2D;

public class Gui implements MouseListener, MouseMotionListener, KeyListener 
{
	private GLCanvas canvas;
	private LinkedList<Widget> widgets = new LinkedList<Widget>( );
	private Matrix ortho = new Matrix( );
	private boolean attached = false;
	private Font default_font;
	private Color bg_col = new Color( 0, 0, 0, 0 );
	private Color fg_col = new Color( 1, 1, 1, 1 );
	private Color border_col = new Color( 1, 1, 1, 1 );
	private Widget key_focus = null;
	private Widget last_pressed_widget = null;
	private Widget last_hovered_widget = null;
	private boolean show_debug_bounds = false;
	private Vector2D translation = new Vector2D();	
	private ScreenAlignementContainer dialog = null;		
	
	public Gui( GLCanvas canvas )
	{
		this.canvas = canvas;
		default_font = new Font( "Arial", 12, FontStyle.Plain );
		attach();
	}	
	
	public GLCanvas getCanvas( )
	{
		return canvas;
	}
	
	public void setShowDebugBounds( boolean show )
	{
		show_debug_bounds = show;
	}
	
	public void showConfirmDialog( String text, String caption )
	{
		showConfirmDialog( text, caption, 400 );
	}
	
	public void showConfirmDialog( String text, String caption, ClickedListener listener )
	{
		showConfirmDialog( text, caption, 400, listener );
	}
	
	
	public void showConfirmDialog( String text, String caption, float width )
	{
		showConfirmDialog( text, caption, width, null );
	}
	
	public void showConfirmDialog( String text, String caption, float width, final ClickedListener listener )
	{
		final ScreenAlignementContainer cont = new ScreenAlignementContainer( this, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
		ConfirmDialog dialog = new ConfirmDialog( this, text, caption, new ClickedListener( ){

			public void clicked(Widget widget) 
			{
				if( listener != null )
					listener.clicked( widget );
				widget.gui.remove( cont );
				widget.gui.dialog = null;
			}			
		}, (int)width);				
		cont.addWidget( dialog );
		cont.setZOrder( Integer.MAX_VALUE );
		this.add( cont );
		this.dialog = cont;
	}
	
	public void showCustomDialog( CustomDialog dialog ) 
	{						
		dialog.setZOrder( Integer.MAX_VALUE );
		this.add( dialog );
		this.dialog = dialog;
	}
	
	public void removeCustomDialog( CustomDialog dialog )
	{
		remove( dialog );
		this.dialog = null;
	}
	
	public void render( )
	{
		Collections.sort( widgets, new Comparator<Widget>(){

			public int compare(Widget o1, Widget o2) 
			{			
				return o2.getZOrder() - o1.getZOrder();
			}			
		});
		
		GL gl = GLContext.getCurrent().getGL();
		ortho.setToOrtho2D( 0, 0, canvas.getWidth(), canvas.getHeight() );
		
		gl.glPushMatrix();
		gl.glLoadMatrixf( ortho.toFloatBuffer() );
		
		gl.glEnable( GL.GL_BLEND );
		gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA );
		
		for( int i = widgets.size() - 1; i >= 0 ; i-- )
		{
			Widget widget =widgets.get(i);
			if( widget.isVisible() )
				widget.render(canvas);
		}
		
		if( show_debug_bounds )
		{
			gl.glColor3f( 1, 0, 0 );
			for( Widget widget: widgets )
				Widget.renderOutlinedQuad( widget.getPosition().x, widget.getPosition().y, widget.getWidth(), widget.getHeight() );
		}
		
		gl.glDisable( GL.GL_BLEND );
		gl.glPopMatrix();		
	}
	
	public Widget getWidget( float x, float y )
	{
		for( Widget widget: widgets )
		{
			if( widget.isVisible() == false )
				continue;
			
			if( widget.intersects( x, canvas.getHeight() - y ) )
				return widget;
		}
		
		return null;
	}
	
	public void mouseClicked(MouseEvent e) 
	{	
	}

	public void mouseEntered(MouseEvent e) 
	{	
		
	}

	public void mouseExited(MouseEvent e) 
	{	
		
	}

	public void mousePressed(MouseEvent e) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			Widget widget = getWidget( e.getX(), e.getY() );
			
			if( dialog != null && widget != dialog )
				return;
			
			if( widget == null )
				return;
			
			if( widget.isFocusable( ) )
			{
				if( key_focus != null )
					key_focus.setFocus( false );
				key_focus = widget;
				widget.setFocus( true );
			}
			
			widget.mousePressed( e.getX() - widget.getPosition().x, canvas.getHeight() - e.getY() - widget.getPosition().y, e.getButton() );
			last_pressed_widget = widget;
		}
	}

	public void mouseReleased(MouseEvent e) 
	{			
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( last_pressed_widget == null )
				return;		
			
			last_pressed_widget.mouseReleased( e.getX() - last_pressed_widget.getPosition().x, canvas.getHeight() - e.getY() - last_pressed_widget.getPosition().y, e.getButton() );
			last_pressed_widget = null;
		}
	}

	public void mouseDragged(MouseEvent e) 
	{				
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( last_pressed_widget == null )
				return;
			
			last_pressed_widget.mouseDragged( e.getX() - last_pressed_widget.getPosition().x, canvas.getHeight() - e.getY() - last_pressed_widget.getPosition().y, e.getButton() );
		}
	}

	public void mouseMoved(MouseEvent e) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			Widget widget = getWidget( e.getX(), e.getY() );
			
			if( dialog != null && widget != dialog )
				return;
			
			if( last_hovered_widget != widget )
			{
				if( last_hovered_widget != null )
					last_hovered_widget.mouseExited();
				last_hovered_widget = widget;
			}
			
			if( widget == null )
				return;			
			
			widget.mouseMoved( e.getX() - widget.getPosition().x, canvas.getHeight() - e.getY() - widget.getPosition().y );		
		}
	}

	public void keyPressed(KeyEvent e) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( dialog != null && key_focus != dialog )
				return;
			
			if( key_focus == null )
				return;
			
			key_focus.keyPressed( e.getKeyCode() );
		}
	}

	public void keyReleased(KeyEvent e) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( dialog != null && key_focus != dialog )
				return;
			
			if( key_focus == null )
				return;
			
			key_focus.keyReleased( e.getKeyCode() );
		}
	}

	public void keyTyped(KeyEvent e) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( dialog != null && key_focus != dialog )
				return;
			
			if( key_focus == null )
				return;
			
			key_focus.keyTyped( e.getKeyChar() );
		}
	}
	
	public void attach( )
	{
		if( attached )
			return;
		
		canvas.addMouseMotionListener( this );
		canvas.addMouseListener( this );
		canvas.addKeyListener( this );
		attached = true;
	}

	public void detach() 
	{	
		if( !attached )
			return;
		
		canvas.removeMouseListener( this );
		canvas.removeMouseMotionListener( this );
		canvas.removeKeyListener( this );
		
		attached = false;
	}
	
	public void dispose( )
	{
		detach( );
		if( default_font != null )
			default_font.dispose();
	}

	public void add(Widget widget) 
	{
		getCanvas().getContext().makeCurrent();
		widgets.add( widget );		
		if( key_focus == null )
			key_focus = widget;
	}
	
	public void remove( Widget widget )
	{
		if( widget == null )
			return;
		
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			widgets.remove( widget );
			widget.dispose();
			if( key_focus == widget )
			{
				if( widgets.size() != 0 )
					key_focus = widgets.get(0);
				else
					key_focus = null; 
			}
		}
	}

	public void setDefaultFont( String face, int size, FontStyle style ) 
	{	
		synchronized( canvas )
		{
			getCanvas().getContext().makeCurrent();
			if( default_font != null )
				default_font.dispose();
			default_font = new Font( face, size, style );
		}
	}
	
	public Font getDefaultFont( )
	{
		return default_font;
	}

	public Color getDefaultBackgroundColor() 
	{	
		return bg_col;
	}

	public Color getDefaultForegroundColor() 
	{			
		return fg_col;
	}

	public Color getDefaultBorderColor( )
	{
		return border_col;
	}
	
	public void setDefaultBackgroundColor( float r, float g, float b, float a )
	{
		bg_col.set(r, g, b, a);
	}
	
	public void setDefaultForegroundColor( float r, float g, float b, float a )
	{
		fg_col.set(r, g, b, a);
	}
	
	public void setDefaultBorderColor( float r, float g, float b, float a )
	{
		border_col.set(r, g, b, a);
	}
	
	protected void pushTranslation( float x, float y )
	{
		translation.x += (int)x;
		translation.y += (int)y;		
	}	
	
	protected void enableScissor( float x, float y, float width, float height )
	{
		canvas.getGL().glScissor( (int)(x + translation.x), (int)(y + translation.y), (int)width, (int)height );
		canvas.getGL().glEnable( GL.GL_SCISSOR_TEST );
	}
	
	protected void disableScissor( )
	{
		canvas.getGL().glDisable( GL.GL_SCISSOR_TEST );
		
	}
	
	public GL getGL() 
	{	
		return canvas.getGL();
	}
}
