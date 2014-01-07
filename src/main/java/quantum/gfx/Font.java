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

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

/**
 * a simple font class for drawing system fonts as
 * textured quads.
 * 
 * @author mzechner@know-center.at
 *
 */
public class Font 
{
	public enum FontStyle
	{
		Plain,
		Bold,
		Italic,
		BoldItalic
	}
	
	protected final int size;
	protected final String face;
	protected FontStyle style;
	protected Texture texture;
	protected float height;
	protected float descent;
	protected HashMap<Character, Integer> advances = new HashMap<Character, Integer>();	
	protected HashMap<Character, TextureTile> tiles = new HashMap<Character, TextureTile>();
	
	/**
	 * creates a new font with the given face, size and style
	 * OpenGL has to be intialized already at this point
	 * @param face face of the font
	 * @param size size of the font
	 * @param style style of the font
	 */
	public Font( String face, int size, FontStyle style )
	{
		this.face = face;
		this.size = size;
		this.style = style;
		
		java.awt.Font font = null;
		
		try
		{
			java.awt.Font base_font = java.awt.Font.createFont( java.awt.Font.PLAIN, new FileInputStream( face ) );
			font = base_font.deriveFont( getJavaFontStyle(style), size );
		}
		catch( Exception ex )
		{
		
			font = new java.awt.Font( face, getJavaFontStyle(style), size );
		}
		
		BufferedImage image = getGlyphImage( font, 512, tiles, advances );
		texture = Texture.loadTexture( image );
		
	}
	
	public Font( InputStream face, int size, FontStyle style )
	{
		this.face = "";
		this.size = size;
		this.style = style;
		
		java.awt.Font font = null;
		
		try
		{
			java.awt.Font base_font = java.awt.Font.createFont( java.awt.Font.PLAIN, face );
			font = base_font.deriveFont( getJavaFontStyle(style), size );
		}
		catch( Exception ex )
		{		
			font = new java.awt.Font( "Arial", getJavaFontStyle(style), size );
		}
		
		BufferedImage image = getGlyphImage( font, 512, tiles, advances );
		texture = Texture.loadTexture( image );
		
	}
	
	public boolean containsGlyph( char c )
	{
		return tiles.containsKey( c );
	}
	
	protected int getJavaFontStyle( FontStyle style )
	{
		if( style == FontStyle.Plain )
			return java.awt.Font.PLAIN;
		if( style == FontStyle.Bold )
			return java.awt.Font.BOLD;
		if( style == FontStyle.Italic )
			return java.awt.Font.ITALIC;
		if( style == FontStyle.BoldItalic )
			return java.awt.Font.BOLD | java.awt.Font.ITALIC;
		
		return java.awt.Font.PLAIN;
	}
	
	protected BufferedImage getGlyphImage( java.awt.Font font, int img_size, HashMap<Character, TextureTile> tiles, HashMap<Character, Integer> advances )
	{
		BufferedImage image = new BufferedImage( img_size, img_size, BufferedImage.TYPE_4BYTE_ABGR );		
		
		Graphics2D g = image.createGraphics();		
		g.setFont( font );
		g.setColor( new java.awt.Color( 0, true ) );
		g.fillRect( 0, 0, img_size, img_size );
		g.setColor( new java.awt.Color( 0xffffffff, true ) );
		FontMetrics metrics = g.getFontMetrics();
		int height = (int)Math.ceil(metrics.getMaxCharBounds( g ).getHeight());
		int descent = metrics.getMaxDescent();
		
		int x = 2; 
		int y = height + descent + 2;
		
		for( char c = 0; c < 256; c++ )
		{
			if( Character.isISOControl( c ) )
				continue;
			
			if( x + metrics.charWidth(c) + 2 > img_size )
			{
				y += height + descent + 2;
				x = 2;
			}
			
			g.drawString( "" + c, x, y );
			advances.put( c, metrics.charWidth(c));
			tiles.put( c, new TextureTile( img_size, img_size, x, y - height, metrics.charWidth( c ), height + descent ) );
			x += advances.get( c ) + 2;
		}
		
		g.dispose();	
		this.height = height;
		this.descent = descent;
		
		return image;
	}
	
	/**
	 * renders the given text. texture unit 0 is used
	 * for the texture map and not reset to the previous
	 * binding. no additional care is taken for escape
	 * sequences or new lines and the like. alpha testing
	 * is enabled and reset, but not the alpha function 
	 * ( glAlphaFunc( GL_GREATER, 0.1f ) is used ). the 
	 * text is placed on the local x/y plane, were the
	 * current translation specifies the upper left corner
	 * of the line of text.
	 * 
	 * @param text text to be rendered with this font
	 */
	public void renderText( String text )
	{		
		texture.bind(0);
		GL gl = GLContext.getCurrent().getGL();
		gl.glEnable( GL.GL_ALPHA_TEST );
		gl.glAlphaFunc( GL.GL_GREATER, 0.1f );
		Mesh.beginQuads();
		float x = 0;
		float y = 0;
		for( int i = 0; i < text.length(); i++ )
		{
			Character c = text.charAt( i );
			if( !tiles.containsKey( c ) )
				continue;
						
			TextureTile tile = tiles.get( c );
			
			Mesh.texi( 0, tile.getLeft(), tile.getTop());
			Mesh.coordi( x, y );
			Mesh.texi( 0, tile.getLeft(), tile.getBottom() );
			Mesh.coordi( x, y - tile.getHeightPixels() );
			Mesh.texi( 0, tile.getRight(), tile.getBottom() );
			Mesh.coordi( x + tile.getWidthPixels(), y - tile.getHeightPixels() );
			Mesh.texi( 0, tile.getRight(), tile.getTop() );
			Mesh.coordi( x + tile.getWidthPixels(), y );

			x+= advances.get( c );
		}
		Mesh.end();
		gl.glDisable( GL.GL_ALPHA_TEST );
		texture.unbind(0);
	}
	
	/**
	 * pushes the modelview matrix, translates
	 * it by (x,y) and renders the given text
	 * 
	 * @param x
	 * @param y
	 * @param text
	 */
	public void renderText( float x, float y, String text )
	{
		GL gl = GLContext.getCurrent().getGL();
		gl.glPushMatrix();
		
		gl.glTranslatef( x, y, 0 );
		renderText( text );		
		gl.glPopMatrix();
	}
	
	/**
	 * renders the given text starting at x,y interpreting
	 * new line. y is decreased for each line.
	 * @param x
	 * @param y
	 * @param text
	 */
	public void renderTextNewLine( float x, float y, String text )
	{
		String[] lines = text.split( "\n" );
		
		for( String line: lines )
		{
			renderText( x, y, line );
			y-=getHeight();
		}
	}
	
	/**
	 * returns the height of a line of text
	 * @return
	 */
	public float getHeight( )
	{
		return height;
	}
	
	public float getDescent( )
	{
		return descent;
	}
	
	/**
	 * returns the width of the character in pixels
	 * or 0 if the character is not contained in the
	 * font 
	 * 
	 * @param c character
	 * @return width of character in pixels
	 */
	public float getWidth( char c )
	{
		if( advances.containsKey( c ) )
			return advances.get( c );
		else
			return 0;
	}
	
	public float getWidth( String text )
	{
		float w = 0;
		for( int i = 0; i < text.length(); i++ )		
			w += getWidth( text.charAt( i ) );	
		
		return w;
	}
	
	/**
	 * disposes the font
	 */
	public void dispose( )
	{
		if( texture != null )
			texture.dispose();
	}

}
