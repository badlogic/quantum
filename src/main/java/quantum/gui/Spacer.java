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

import javax.media.opengl.GLCanvas;


public class Spacer extends Widget
{
	boolean render = false;
	
	public Spacer(Gui gui, float width, float height ) 
	{
		super(gui);
		setWidth( width );
		setHeight( height );
		setForegroundColor( 0, 0, 0, 0 );
		setBackgroundColor( 0, 0, 0, 0 );
		setBorderColor( 0, 0, 0, 0 );
	}

	public void setRender( boolean render )
	{
		this.render = render;
	}

	public boolean isRendering( )
	{
		return render;
	}
	
	@Override
	public boolean isFocusable() 
	{	
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
		
		gui.getGL().glColor4f( border_col.getR(), border_col.getG(), border_col.getB(), border_col.getA() );
		renderOutlinedQuad( pos.x, pos.y, width, height );		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
