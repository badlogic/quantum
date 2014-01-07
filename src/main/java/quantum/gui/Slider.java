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

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

public class Slider extends Widget
{
	float value = 0;
	float min_value = 0;
	float max_value = 0;
	ValueChangedListener listener;
	
	public Slider(Gui gui, float minimum, float maximum, float value ) 
	{
		super(gui);	
		min_value = minimum;
		max_value = maximum;
		this.value = value;
		setMinimumValue( minimum );
		setMaximumValue( maximum );
		setValue( value );
	}

	public float getValue( )
	{
		return value;
	}
	
	public void setValue( float value )
	{
		if( value < min_value )
			value = min_value;
		else
			if( value > max_value )
				value = max_value;
			else
				this.value = value;
	}
	
	public float getMinimumValue( )
	{
		return min_value;
	}
	
	public void setMinimumValue( float value )
	{
		if( value > max_value )
			throw new RuntimeException( "maximum value > minimum value");
		min_value = value;
	}
	
	public float getMaximumValue( )
	{
		return max_value;
	}
	
	public void setMaximumValue( float value )
	{
		if( value < min_value )
			throw new RuntimeException( "maximum value > minimum value");
		max_value = value;
	}
	
	@Override
	public void dispose() 
	{	
		
	}

	@Override
	public boolean isFocusable() 
	{	
		return false;
	}

	@Override
	public void keyPressed(int key_code) 
	{	
		
	}

	@Override
	public void keyReleased(int key_code) 
	{	
		
	}

	@Override
	public void keyTyped(char character) 
	{	
		
	}

	@Override
	public void mouseDragged(float x, float y, int button) 
	{	
		float scale = x / width;
		setValue( scale * (max_value - min_value) + min_value );
		if( listener != null )
			listener.valueChanged( this );
	}

	@Override
	public void mouseExited() 
	{	
		
	}

	@Override
	public void mouseMoved(float x, float y) 
	{	
		
	}

	@Override
	public void mousePressed(float x, float y, int button) 
	{	
		float scale = x / width;
		setValue( scale * (max_value - min_value) + min_value );		
		if( listener != null )
			listener.valueChanged( this );
	}

	@Override
	public void mouseReleased(float x, float y, int button) 
	{	

	}

	@Override
	public void render(GLCanvas canvas) 
	{	
		GL gl = canvas.getGL();
		gl.glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad( pos.x, pos.y, width, height );
		gl.glColor4f( fg_col.getR(), fg_col.getG(), fg_col.getB(), fg_col.getA() );
		
		float scale = 0;
		if( max_value != min_value )
			scale = ( value - min_value )/ ( max_value - min_value );
		
		renderQuad( pos.x, pos.y, width * scale, height );
	}

	public void setValueChangedListener( ValueChangedListener listener ) 
	{	
		this.listener = listener;
	}

}
