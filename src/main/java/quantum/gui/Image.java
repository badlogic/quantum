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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.gfx.Texture;

public class Image extends Widget
{	
	Texture texture;
	
	public Image( Gui gui, Texture texture )
	{
		super( gui );
		this.texture = texture;
		width = this.texture.getImageWidth();
		height = this.texture.getImageHeight();
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );
	}

	public Image( Gui gui, BufferedImage image )
	{
		super( gui );
		this.texture = Texture.loadTexture( image );
		width = this.texture.getImageWidth();
		height = this.texture.getImageHeight();
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );
	}
	
	public Image( Gui gui, InputStream in )
	{
		super( gui );
		this.texture = Texture.loadTexture( in );
		width = this.texture.getImageWidth();
		height = this.texture.getImageHeight();
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );
		try {
			in.close();
		} catch (IOException e) 
		{
		} 
	}
		
	public void setImage( BufferedImage img )
	{
		if( texture != null )
			texture.dispose();
		
		if( img != null )
			texture = Texture.loadTexture( img );
	}
	
	@Override
	public boolean isFocusable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void keyPressed(int key_code) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(int key_code) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(char character) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(float x, float y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(float x, float y, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(GLCanvas canvas) 
	{	
		gui.getGL().glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad(pos.x, pos.y, width, height);							
				
		GL gl = canvas.getGL();
				
		if( texture != null )
		{
			texture.bind(0);
					
			gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
			gl.glBegin( GL.GL_QUADS );
				gl.glTexCoord2f( 0, 0 );
				gl.glVertex2f( pos.x, pos.y );
				gl.glTexCoord2f( texture.getImageWidth() / texture.getWidth(), 0 );
				gl.glVertex2f( pos.x + width, pos.y );
				gl.glTexCoord2f( texture.getImageWidth() / texture.getWidth(), texture.getImageHeight() / texture.getHeight() );
				gl.glVertex2f( pos.x + width, pos.y - height );
				gl.glTexCoord2f( 0, texture.getImageHeight() / texture.getHeight() );
				gl.glVertex2f( pos.x, pos.y - height );
			gl.glEnd();
			
			texture.unbind();
		}
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );
	}

	@Override
	public void dispose() 
	{
		if( texture != null )
			texture.dispose();
	}
}
