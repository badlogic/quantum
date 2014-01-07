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

/**
 * simple class for specifying tiles in a texture
 * by pixels. returns texture coordinates for pixel
 * coordinates. usefull for glyphs etc.
 * 
 * @author mzechner@know-center.at
 *
 */
public class TextureTile 
{	
	protected final float  u, v;
	protected final float w, h;
	protected final float o_w, o_h;
	
	/**
	 * calculates the texture coordinates and width
	 * of the specified tile in the texture coorindate
	 * space. uses the dimensions of tex for the
	 * basis of calculation
	 * 
	 * @param tex texture to get the pixel dimensions from
	 * @param x x position of top left corner of the tile in pixel coordinates
	 * @param y y position of top left corner of the tile in pixel coordinates
	 * @param width width of tile in pixels
	 * @param height height of tile in pixels
	 */
	public TextureTile( Texture tex, int x, int y, int width, int height )
	{
		u = x / (float)tex.getWidth();
		v = y / (float)tex.getHeight();
		
		w = width / (float)tex.getWidth();
		h = height / (float)tex.getHeight();
		o_w = width;
		o_h = height;
	}
	
	/**
	 * calculates the texture coordinates and width
	 * of the specified tile in the texture coorindate
	 * space. uses the dimensions of tex for the
	 * basis of calculation
	 * 
	 * @param img_width width of image/texture in pixels
	 * @param img_height height of image/texture in pixels
	 * @param x x position of top left corner of the tile in pixel coordinates
	 * @param y y position of top left corner of the tile in pixel coordinates
	 * @param width width of tile in pixels
	 * @param height height of tile in pixels
	 */
	public TextureTile( int img_width, int img_height, int x, int y, int width, int height )
	{
		u = x / (float)img_width;
		v = y / (float)img_height;
		
		w = width / (float)img_width;
		h = height / (float)img_height;
		
		o_w = width;
		o_h = height;
	}
	
	/**
	 * @return the u coordinate of this tile's top left corner in texture space
	 */
	public float getLeft( )
	{
		return u;
	}
	
	/**
	 * @return the v coordinate of this tile's top left corner in texture space
	 */
	public float getTop( )
	{
		return v;
	}	
	
	/**
	 * @return the u coordinate of the bottom right corner of this tile in texture space
	 */
	public float getRight( )
	{
		return u + w;
	}
	
	/**
	 * @return the v coordinate of the bottom right corner of this tile in texture space
	 */
	public float getBottom( )
	{
		return v + h;
	}
	
	/**
	 * @return the tiles width in pixels
	 */
	public float getWidthPixels( )
	{
		return o_w;
	}
	
	/**
	 * @return the tiles height in pixels
	 */
	public float getHeightPixels( )
	{
		return o_h;
	}
	
	/**
	 * @return the width of this tile in texture space
	 */
	public float getWidth( )
	{
		return w;
	}
	
	/**
	 * @return the height of this tile in texture space
	 */
	public float getHeight( )
	{
		return h;
	}
}
