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

import quantum.gfx.OrthoCamera;
import quantum.math.Vector2D;

public class WorldAlignementContainer extends Container 
{
	VerticalBoxContainer v_box;	
	OrthoCamera camera;
	Vector2D world_pos;
	
	public WorldAlignementContainer(Gui gui, OrthoCamera camera, Vector2D world_pos ) 
	{
		super(gui);
		this.camera = camera;
		this.world_pos = world_pos;
		v_box = new VerticalBoxContainer( gui );		
		getWidgets().add( v_box );
	}

	public void addWidget( Widget widget )
	{
		v_box.addWidget(widget);
	}
	
	public void addWidget( Widget widget, HorizontalAlignement alignement )
	{
		v_box.addWidget(widget, alignement);
	}	
	
	public void layout( )
	{
		super.layout();
		this.width = v_box.getWidth();
		this.height = v_box.getHeight();
		
		pos.x = camera.getWorldToScreenX( world_pos.x );
		pos.y = camera.getWorldToScreenY( world_pos.y );
		
		v_box.setY( height );	
		v_box.layout();
	}
	
	public void render( GLCanvas canvas )
	{			
		GL gl = canvas.getGL();
		gl.glColor4f( bg_col.getR(), bg_col.getG(), bg_col.getB(), bg_col.getA() );
		renderQuad( pos.x, pos.y, width, height );
		gl.glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );
		
		super.render( canvas );
	}
}
