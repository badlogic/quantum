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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.glu.GLU;

import quantum.utils.Log;

/**
 * a simple texture class with image loading and
 * multitexturing capabilities
 * 
 * @author mzechner@know-center.at
 *
 */
public class Texture 
{            
    private static ColorModel glAlphaColorModel;           
    private static ColorModel glColorModel;
    private static int texture_count = 0;
    
    static
    {
        glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[] {8,8,8,8},
                true,
                false,
                ComponentColorModel.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
                
        glColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[] {8,8,8,0},
                false,
                false,
                ComponentColorModel.OPAQUE,
                DataBuffer.TYPE_BYTE);
    }
	
	/** opengl texture id **/
    private int textureID;       
    /** height of original image in pixels **/
    private int height;    
    /** width of original image in pixels **/
    private int width;        
    /** height in pixels of texture **/
    private int texHeight;
    /** width in pixels of texture **/
    private int texWidth;

    
    private int last_unit = 0;
    
    /**
     * Create a new texture
     *
     * @param textureID The GL texture ID
     */
    public Texture(int textureID) 
    {        
        this.textureID = textureID;
    }
    
    /**
     * returns 
     * @return
     */
    protected int getGLId()
    {
    	return textureID;
    }
    
    
    /**
     * activates the given texture unit, enables texturing
     * on that unit, binds the texture and sets the minification
     * filter to GL_LINEAR_MIPMAP_LINEAR and the magnification
     * filter to GL_LINEAR. texture wraps are set to repeat
     * @param unit texture unit to bind the texture to
     */
    public void bind( int unit ) 
    {
    	bind( unit, GL.GL_LINEAR_MIPMAP_LINEAR, GL.GL_LINEAR, GL.GL_REPEAT, GL.GL_REPEAT );
    }    
    
    /**
     * /**
     * activates the given texture unit, enables texturing
     * on that unit, binds the texture and sets the minification
     * filter to min_filter and the magnification filter to mag_filter.
     * texture wrap s is set to tex_wrap_s, texture wrap t is set to
     * tex_wrap_t.   
     * @param unit
     * 
     * @param unit the texture unit to bind the texture to.
     * @param min_filter minification filter ( e.g. GL_LINEAR_MIPMAP_LINEAR )
     * @param mag_filter magnification filter ( e.g. GL_LINEAR )
     * @param tex_wrap_s texutre wrap for s ( e.g. GL_REPEAT );
     * @param tex_wrap_s texutre wrap for t ( e.g. GL_REPEAT );
     */
    public void bind( int unit, int min_filter, int mag_filter, int tex_wrap_s, int tex_wrap_t )
    {
        last_unit = GL.GL_TEXTURE0 + unit;
        GL gl = GLContext.getCurrent().getGL();
        gl.glActiveTexture( last_unit );      
        gl.glEnable( GL.GL_TEXTURE_2D );
        gl.glBindTexture( GL.GL_TEXTURE_2D, textureID); 
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, min_filter );
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, mag_filter );
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, tex_wrap_s );
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, tex_wrap_t );
    }
    
    /**
     * unbinds the texture from the last unit it was bound to
     * and deactivates texturing for that unit
     */
    public void unbind()
    {
    	GL gl = GLContext.getCurrent().getGL();
    	gl.glActiveTexture( last_unit );
        gl.glDisable( GL.GL_TEXTURE_2D );
        gl.glBindTexture( GL.GL_TEXTURE_2D, 0);
    }
    
    /**
     * unbinds the texture from the specified unit and deactivates
     * texturing for that unit
     * @param unit
     */
    public void unbind( int unit )
    {
    	GL gl = GLContext.getCurrent().getGL();
    	gl.glActiveTexture( GL.GL_TEXTURE0 + unit );
        gl.glDisable( GL.GL_TEXTURE_2D );
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Get the height of the original image
     *
     * @return The height of the original image
     */
    public int getImageHeight() {
        return height;
    }
    
    /** 
     * Get the width of the original image
     *
     * @return The width of the original image
     */
    public int getImageWidth() {
        return width;
    }
    
    /**
     * Get the height of the physical texture
     *
     * @return The height of physical texture
     */
    public float getHeight() {
        return texHeight;
    }
    
    /**
     * Get the width of the physical texture
     *
     * @return The width of physical texture
     */
    public float getWidth() {
        return texWidth;
    }   
    
    /**
     * diposes the texture, does not unbind it
     */
    public void dispose( )
    {
    	IntBuffer buffer = IntBuffer.allocate(1);
    	buffer.put( textureID );
    	buffer.flip();
    	try
    	{
    		GLContext.getCurrent().getGL().glDeleteTextures( 1, buffer );
    		Log.println( "[Texture] disposed texture " + this.getWidth() + "x" + this.getHeight()  );
    		texture_count--;
    	}
    	catch( Exception ex )
    	{
    		Log.println( "[Texture] couldn't dispose texture: " + Log.getStackTrace( ex ) );
    	}    	
    }
    
    public static int getTextureCount( )
    {
    	return texture_count;
    }
           
    /**
     * creates a new empty texture with the given pixel format 
     * and width and height. in case width or height are not 
     * powers of 2 the next power of 2 is taken. Note that this
     * will bind this texture to the currently active texture unit
     * and does not restore the old binding.
     * 
     * @param width width of the texture
     * @param height height of the texture
     * @param dstPixelFormat pixel format of the texture ( e.g. GL_RGBA )
     * @return the new texture
     */
    public static Texture createTexture( int width, int height, int dstPixelFormat )
    {
    	GL gl = GLContext.getCurrent().getGL();
    	int texture_id = createTextureID();
    	Texture tex = new Texture( texture_id );
    	gl.glBindTexture( GL.GL_TEXTURE_2D, texture_id );
    	gl.glTexImage2D( GL.GL_TEXTURE_2D, 0, dstPixelFormat, get2Fold(width), get2Fold(height), 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, (ByteBuffer)null );
    	tex.texWidth = get2Fold( width );
    	tex.texHeight = get2Fold( height );  
    	gl.glBindTexture( GL.GL_TEXTURE_2D, 0 );
    	return tex;
    }
    
    /**
     * creates a new empty shadow texture with the given and width and height. 
     * in case width or height are not powers of 2 the next power of 2 is taken. 
     * Note that this will bind this texture to the currently active texture unit
     * and does not restore the old binding.
     * 
     * @param width width of the texture
     * @param height height of the texture
     * @return the new texture
     */
    public static Texture createDepthTexture( int width, int height )
    {
    	GL gl = GLContext.getCurrent().getGL();
    	int texture_id = createTextureID();
    	Texture tex = new Texture( texture_id );
    	gl.glBindTexture( GL.GL_TEXTURE_2D, texture_id );
    	gl.glTexImage2D( GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH_COMPONENT, get2Fold(width), get2Fold(height), 0, GL.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_BYTE, (ByteBuffer)null );
    	tex.texWidth = get2Fold( width );
    	tex.texHeight = get2Fold( height );    	
    	gl.glBindTexture( GL.GL_TEXTURE_2D, 0 );
    	return tex;
    }
    
    /**
     * loads an image from the given inputstream with GL_RGBA
     * as texture format. throws a runtime exception in 
     * case the image could not be loaded. in case the
     * image's dimensions are not a power of two the
     * texture dimensions will have the next power of two.
     * the image will not be scaled to fit the texture
     * size but remain at its original dimensions.     
     * 
     * @param in inputstream to be loaded
     * @return the new texture
     */
    public static Texture loadTexture( InputStream in )
    {
    	try
    	{
    		return loadTexture( loadImage( in ), GL.GL_RGBA );
    	}
    	catch( Exception ex )
    	{
    		throw new RuntimeException( "couldn't load image");
    	}
    }
    
    /**
     * loads a texture from the given image with GL_RGBA
     * as texture format. in case the
     * image's dimensions are not a power of two the
     * texture dimensions will have the next power of two.
     * the image will not be scaled to fit the texture
     * size but remain at its original dimensions.  
     * 
     * @param image image to be loaded
     * @return the new texture
     */
    public static Texture loadTexture( BufferedImage image )
    {
    	return loadTexture( image, GL.GL_RGBA );
    }
    
    /**
     * Load a texture into OpenGL from a image reference on
     * disk.
     *
     * @param resourceName The location of the resource to load
     * @param target The GL target to load the texture against
     * @param dstPixelFormat The pixel format of the screen
     * @param minFilter The minimising filter
     * @param magFilter The magnification filter
     * @return The loaded texture
     * @throws IOException Indicates a failure to access the resource
     */
    private static Texture loadTexture(BufferedImage bufferedImage, int dstPixelFormat ) 
    { 
    		GL gl = GLContext.getCurrent().getGL();
	        int srcPixelFormat = 0;	        
	        int textureID = createTextureID(); 
	        Texture texture = new Texture(textureID); 	        
	        gl.glBindTexture( GL.GL_TEXTURE_2D, textureID); 
	 
	        texture.width = bufferedImage.getWidth();
	        texture.height = bufferedImage.getHeight();
	        
	        if (bufferedImage.getColorModel().hasAlpha()) 
	            srcPixelFormat = GL.GL_RGBA;
	        else 
	            srcPixelFormat = GL.GL_RGB;
	        
	
	        ByteBuffer textureBuffer = convertImageData(bufferedImage,texture); 	        
	        new GLU().gluBuild2DMipmaps( GL.GL_TEXTURE_2D, dstPixelFormat, get2Fold( bufferedImage.getWidth()), get2Fold( bufferedImage.getHeight()), srcPixelFormat, GL.GL_UNSIGNED_BYTE, textureBuffer);
	        Log.println( "[Texture] created texture " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight() );
	        texture_count ++;
	        return texture; 
    } 
    
    /**
     * Get the closest greater power of 2 to the fold number
     * 
     * @param fold The target number
     * @return The power of 2
     */
    private static int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
    } 
    
    /**
     * Convert the buffered image to a texture
     *
     * @param bufferedImage The image to convert to a texture
     * @param texture The texture to store the data into
     * @return A buffer containing the data
     */
    @SuppressWarnings("unchecked")
	private static ByteBuffer convertImageData(BufferedImage bufferedImage,Texture texture) { 
        ByteBuffer imageBuffer = null; 
        WritableRaster raster;
        BufferedImage texImage;
        
        int texWidth = get2Fold( bufferedImage.getWidth() );
        int texHeight = get2Fold( bufferedImage.getHeight() );       
        
        texture.texHeight = texHeight;
        texture.texWidth = texWidth;
        
        if (bufferedImage.getColorModel().hasAlpha()) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,texWidth,texHeight,4,null);
            texImage = new BufferedImage(glAlphaColorModel,raster,false,new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,texWidth,texHeight,3,null);
            texImage = new BufferedImage(glColorModel,raster,false,new Hashtable());
        }
            
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0f,0f,0f,0f));
        //g.fillRect(0,0,texWidth,texHeight);
        g.drawImage(bufferedImage,0,0,null);        
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData(); 

        if( texImage.getColorModel().hasAlpha() )
        {
        	for( int i = 0; i < data.length; i+=4 )
        	{
        		if( data[i] == 0 && data[i+1] == 0 && data[i+2] == 0 )
        			data[i+3] = 0;        			
        	}
        }
        
        imageBuffer = ByteBuffer.allocateDirect(data.length); 
        imageBuffer.order(ByteOrder.nativeOrder()); 
        imageBuffer.put(data, 0, data.length); 
        imageBuffer.flip();
        
        return imageBuffer; 
    } 
    
    private static BufferedImage loadImage(InputStream in ) throws IOException 
    {        
        BufferedImage bufferedImage = ImageIO.read(new BufferedInputStream(in));  
        return bufferedImage;
    }  
    
    private static int createTextureID() 
    { 
       IntBuffer tmp = IntBuffer.allocate(1); 
       GLContext.getCurrent().getGL().glGenTextures(1, tmp); 
       return tmp.get(0);
    }    
}
