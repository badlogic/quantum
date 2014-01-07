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

import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;

import quantum.game.Constants;
import quantum.game.Creature;
import quantum.game.GameInterface;
import quantum.game.Planet;
import quantum.game.Simulation;
import quantum.game.Tree;
import quantum.math.Bounds;
import quantum.math.Vector;
import quantum.math.Vector2D;
import quantum.math.WindowedMean;
import quantum.utils.FileManager;
import quantum.utils.Log;
import quantum.utils.Timer;

import com.sun.opengl.util.Screenshot;

public class Renderer 
{	
	FrameBufferObject offscreen_fbo;
	FrameBufferObject screen_fbo;
	FrameBufferObject fbo;
	FrameBufferObject fbo_tmp;	
	Shader vert_shader;
	Shader hort_shader;
	Shader instance_shader;
	boolean instancing = true;
	boolean glow = true;
	boolean render_all_paths = false;
	boolean render_is_start_planet = false;
	OrthoCamera cam = new OrthoCamera( 0, 0, 10 );
	
	boolean colors_set = false;
	int color_idx = 0;
	Color[] colors = { new Color( 1, 0, 0, 1 ),
					   new Color( 0, 1, 0, 1),
					   new Color( 0.2f, 0.2f, 1, 1),
					   new Color( 1, 1, 0, 1),
					   new Color( 0, 1, 1, 1),
					   new Color( 1, 0, 1, 1),
					   new Color( 0.5f, 0.5f, 0.5f, 1) };
					   		
	
	HashMap<Integer, Color> player_color = new HashMap<Integer, Color>();
	Planet selected_planet = null;
	HashMap<String, Texture> textures = new HashMap<String, Texture>();
	Timer timer = new Timer( );	
	Timer planet_timer = new Timer( );
	Timer tree_timer = new Timer( );
	Timer creature_timer = new Timer( );
	Timer glow_timer = new Timer( );
	Timer gui_timer = new Timer( );
	
	long current_time = System.nanoTime();
	
	WindowedMean elapsed_seconds = new WindowedMean( 10 );
	WindowedMean planet_render_time = new WindowedMean( 10 );
	WindowedMean tree_render_time = new WindowedMean( 10 );
	WindowedMean creature_render_time = new WindowedMean( 10 );
	WindowedMean glow_render_time = new WindowedMean( 10 );
	WindowedMean gui_render_time = new WindowedMean( 10 );
	int culled = 0;
	
	Timer fps_timer = new Timer();
	int frames = 0;
	float fps = 0;
	
	public Renderer( )
	{
		try {
			fps_timer.start();
			screen_fbo = new FrameBufferObject( 256, 256 );
			fbo = new FrameBufferObject( 512, 512 );
			fbo_tmp = new FrameBufferObject( 512, 512 );
			vert_shader = new Shader( FileManager.readFile( "shader/vertex.glsl" ), FileManager.readFile( "shader/verticalgaussian.glsl" ) );
			hort_shader = new Shader( FileManager.readFile( "shader/vertex.glsl" ), FileManager.readFile( "shader/horizontalgaussian.glsl" ) );
			instance_shader = new Shader( FileManager.readFile( "shader/instancer.glsl" ), null );
			Log.println( "[Renderer] shaders supported" );			
		} catch (Exception e) {
			Log.println( "[Renderer] disabling shader support: " + e.getMessage() );
			instancing = false;
			glow = false;
		}			
		
		try
		{
			textures.put( "smoke1", Texture.loadTexture( FileManager.readFile( "smoke1.bmp" ) ) );
			Log.println( "[Renderer] loaded textures" );
		}
		catch( Exception ex )
		{		
			Log.println( "[Renderer] couldn't load textures: " + ex.getMessage() );
		}			
	}
	
	public void centerCamera( GLCanvas canvas, Simulation sim )
	{
		Bounds b = new Bounds( );
		for( Planet planet: sim.getPlanets() )
		{
			float x = planet.getPosition().x;
			float y = planet.getPosition().y;
			float r = planet.getRadius();
			b.ext( new Vector( x + r, y, 0 ) );
			b.ext( new Vector( x, y + r, 0 ) );
			b.ext( new Vector( x - r, y, 0 ) );
			b.ext( new Vector( x, y - r, 0 ) );
		}
		
		float width = b.getDimensions().getX();
		float height = b.getDimensions().getY();
		
		float scale = Math.max( width / canvas.getWidth(), height / canvas.getHeight() );
		
		cam.setPosition( b.getCenter() );		
		cam.setScale( scale );
	}	
	
	public void centerCamera( OrthoCamera cam, int img_width, int img_height, Simulation sim )
	{
		Bounds b = new Bounds( );
		for( Planet planet: sim.getPlanets() )
		{
			float x = planet.getPosition().x;
			float y = planet.getPosition().y;
			float r = planet.getRadius();
			b.ext( new Vector( x + r, y, 0 ) );
			b.ext( new Vector( x, y + r, 0 ) );
			b.ext( new Vector( x - r, y, 0 ) );
			b.ext( new Vector( x, y - r, 0 ) );
		}
		
		float width = b.getDimensions().getX();
		float height = b.getDimensions().getY();
		
		float scale = Math.max( width / img_width, height / img_height );
		
		cam.setPosition( b.getCenter() );		
		cam.setScale( scale );	
	}
	
	public BufferedImage takeCenteredScreenShot( GLCanvas canvas, Simulation sim )
	{
		canvas.getContext().makeCurrent();
		OrthoCamera cam = new OrthoCamera( 0, 0, 1 );
		centerCamera( cam, 256, 256, sim );
		
		GL gl = canvas.getGL();
		gl.glDisable( GL.GL_DEPTH_TEST );
		gl.glDepthMask( false );
		gl.glClear( GL.GL_COLOR_BUFFER_BIT );
		
		cam.update(256, 256);		
		
		if( glow )
		{
			fbo.bind();		
			gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );		
			renderPass(sim, null, canvas);
			fbo.unbind();
			instance_shader.unbind();
					
			fbo_tmp.bind();
			vert_shader.bind();
			fbo.renderFullScreenQuad();
			fbo_tmp.unbind();
			vert_shader.unbind();
			
			fbo.bind();
			hort_shader.bind();
			fbo_tmp.renderFullScreenQuad();		
			fbo.unbind();
			hort_shader.unbind();
								
			screen_fbo.bind();
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL.GL_COLOR_BUFFER_BIT );		
		
								
			renderPass(sim, null, canvas);
			gl.glColor4f( 1, 1, 1, 0.7f );	
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);			
			fbo.renderFullScreenQuad();
			gl.glDisable(GL.GL_BLEND);
			gl.glDepthMask(true);			
			BufferedImage img = Screenshot.readToBufferedImage(256, 256);
			screen_fbo.unbind();
			return img;
		}
		else
		{
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL.GL_COLOR_BUFFER_BIT );
			
			int[] old_dim = new int[4];
			GLContext.getCurrent().getGL().glGetIntegerv( GL.GL_VIEWPORT, old_dim, 0);
			GLContext.getCurrent().getGL().glViewport(0, 0, 256, 256);
			renderPass(sim, null, canvas);
			gl.glDepthMask(true);
			GLContext.getCurrent().getGL().glViewport( old_dim[0], old_dim[1], old_dim[2], old_dim[3] );			
			BufferedImage img = Screenshot.readToBufferedImage(256, 256);
			gl.glClear( GL.GL_COLOR_BUFFER_BIT );
			
			return img;
		}						
	}
	
	public void useInstancing( boolean value )
	{
		instancing = value;
		if( instance_shader == null )
			instancing = false;
	}
	
	public void useGlow( boolean value )
	{
		glow = value;
		if( fbo == null )
			glow = false;
	}
	
	public void render( Simulation sim, GLCanvas canvas )
	{
		render( sim, null, canvas );
	}
	
	public void render( Simulation sim, GameInterface gui, GLCanvas canvas )
	{
		current_time = System.nanoTime();
		timer.start();
		tree_timer.start(); tree_timer.pause();
		planet_timer.start(); planet_timer.pause();
		creature_timer.start(); creature_timer.pause();
		GL gl = canvas.getGL();
		gl.glDisable( GL.GL_DEPTH_TEST );
		gl.glDepthMask( false );
		gl.glClear( GL.GL_COLOR_BUFFER_BIT );
		
		cam.update(canvas, true);
		
		if( glow )
		{
			if( offscreen_fbo == null )
			{
				try {
					offscreen_fbo = new FrameBufferObject( canvas.getWidth(), canvas.getHeight() );
					Log.println( "[Renderer] created offscreen fbo" );
				} catch (Exception e) 
				{
					Log.println( "[Renderer] couldn't create offscreen fbo: " + Log.getStackTrace( e ) );
					glow = false;					
				}
			}
			else
			{
				if( offscreen_fbo.width != canvas.getWidth() || offscreen_fbo.height != canvas.getHeight() )
				{
					offscreen_fbo.dispose();
					try {
						offscreen_fbo = new FrameBufferObject( canvas.getWidth(), canvas.getHeight() );
						Log.println( "[Renderer] created offscreen fbo" );
					} catch (Exception e) 
					{
						Log.println( "[Renderer] couldn't create offscreen fbo: " + Log.getStackTrace( e ) );
						glow = false;					
					}
				}
			}
			offscreen_fbo.bind();
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );		
			renderPass(sim, gui, canvas);
			gl.glLineWidth( 1.5f );
			gui_timer.start();
			if( gui != null ) 
				gui.render( canvas );
			gui_timer.pause();
			gl.glLineWidth( 1f );
			offscreen_fbo.unbind();			
				
			fbo.bind();
			gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
			gl.glColor4f( 1, 1, 1, 1 );
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA );	
			offscreen_fbo.renderFullScreenQuadNearest();
			offscreen_fbo.renderFullScreenQuad( 3.5f / canvas.getWidth(), 3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( -3.5f / canvas.getWidth(), -3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( -3.5f / canvas.getWidth(), -3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( 3.5f / canvas.getWidth(), -3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( 0, -3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( 0, 3.5f / canvas.getHeight() );
			offscreen_fbo.renderFullScreenQuad( 3.5f / canvas.getWidth(), 0 );
			offscreen_fbo.renderFullScreenQuad( -3.5f / canvas.getWidth(), 0 );
			gl.glDisable( GL.GL_BLEND );
			
			gui_timer.start();
			if( gui != null ) 
				gui.render( canvas );
			gui_timer.pause();
				
			fbo.unbind();			
		
			glow_timer.start();
			fbo_tmp.bind();
			vert_shader.bind();
			fbo.renderFullScreenQuad();
			fbo_tmp.unbind();
			vert_shader.unbind();
			
			fbo.bind();
			hort_shader.bind();
			fbo_tmp.renderFullScreenQuad();		
			fbo.unbind();
			hort_shader.unbind();					
			
		
			glow_timer.pause();
				
			gl.glColor4f( 1, 1, 1, 0 );
			offscreen_fbo.renderFullScreenQuad();
			
			glow_timer.start();
			gl.glColor4f( 1, 1, 1, 0.7f );	
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);			
			fbo.renderFullScreenQuad();
			gl.glDisable(GL.GL_BLEND);
			glow_timer.pause();
		}
		else
		{
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL.GL_COLOR_BUFFER_BIT );
			renderPass(sim, gui, canvas);
			
			if( gui != null ) 
				gui.render( canvas );
		}			
		
		gl.glDepthMask(true);
		elapsed_seconds.addValue( timer.getElapsedSeconds() );
		tree_render_time.addValue( tree_timer.getElapsedSeconds() );
		planet_render_time.addValue( planet_timer.getElapsedSeconds() );
		creature_render_time.addValue( creature_timer.getElapsedSeconds() );
		glow_render_time.addValue( glow_timer.getElapsedSeconds() );
		gui_render_time.addValue( gui_timer.getElapsedSeconds() );
		gui_timer.stop();
		timer.stop();
		tree_timer.stop();
		creature_timer.stop();
		planet_timer.stop();
		glow_timer.stop();
		
		
		if( fps_timer.getElapsedSeconds() > 1 )
		{
			fps = frames;
			frames = 0;
			fps_timer.stop();
			fps_timer.start();			
		}
		
		frames++;
	}
	
	public void renderPass( Simulation sim, GameInterface gui, GLCanvas canvas )
	{		
		canvas.getGL().glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );		
		culled = 0;
		GL gl = canvas.getGL();
		
		if( !colors_set )
		{
			for( Planet planet: sim.getPlanets() )
			{				
				allocatePlayerColor( sim, planet.getOwner() );
			}
		}			
		
		for( Planet planet: sim.getPlanets() )
		{
			renderPlanetPathsLight( canvas, sim, planet );
		}
		
		canvas.getGL().glLineWidth( 1.5f );					
		
		tree_timer.start();
					
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
		textures.get( "smoke1" ).bind(0);
		gl.glEnable( GL.GL_BLEND );
		gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE );
		gl.glBegin( GL.GL_QUADS );			
		for( Planet planet: sim.getPlanets() )
		{
			for( Tree tree: planet.getTrees() )
			{			
				tree.setColor( getPlayerColor( planet.getOwner() ) );
				tree.renderHalo(canvas, this);
			}			
		}
		
		gl.glEnd( );
		gl.glEnable( GL.GL_BLEND );
		gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA );
		textures.get( "smoke1" ).unbind();
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		
		for( Planet planet: sim.getPlanets() )
		{
			for( Tree tree: planet.getTrees() )
			{			
				tree.render(canvas, this);				
			}
		}
		gl.glDisable( GL.GL_BLEND );
		tree_timer.pause();
		
		planet_timer.start();
		canvas.getGL().glLineWidth(1);
		canvas.getGL().glBegin(GL.GL_TRIANGLES);		
		for( Planet planet: sim.getPlanets() )
		{					
			Color col = getPlayerColor(planet.getOwner());
			if (col != null)
				canvas.getGL().glColor3f(col.getR(), col.getG(), col.getB());
			else
				canvas.getGL().glColor3f(0.7f, 0.7f, 1);
			
			planet.renderCreature( canvas, this);			
		}
		canvas.getGL().glEnd();
		canvas.getGL().glLineWidth(1.5f);
		planet_timer.pause();
		
		for( Planet planet: sim.getPlanets() )
		{			
			planet_timer.start();
			planet.render(canvas, this);			
			if( planet == selected_planet )
				renderPlanetPaths( canvas, sim, selected_planet );
			
			if( render_all_paths )
				renderPlanetPaths( canvas, sim, planet );					
			
			if( render_is_start_planet )
			{
				if( planet.isStartPlanet() )
					planet.renderMesh(canvas, 0.2f, 0, 1, 0 );
			}
			
			planet_timer.pause();						
			
			creature_timer.start();
			if( instancing )
			{
				if( cam.getScale() > 12 )
				{
					canvas.getGL().glPointSize( 2 );
					canvas.getGL().glBegin( GL.GL_POINTS );
					for( Creature creature: planet.getCreatures() )
					{										
						creature.setColor( getPlayerColor( creature.getOwner() ) );
						creature.render(canvas, true);				
					}
					canvas.getGL().glEnd();
					canvas.getGL().glPointSize( 1 );
				}				
				else
				{
					instance_shader.bind();
											
					canvas.getGL().glBegin( GL.GL_TRIANGLES );
					for( Creature creature: planet.getCreatures() )
					{							
						if( cam.visible( creature.getPosition(), Constants.BOID_SIZE ) )
						{
							creature.setColor( getPlayerColor( creature.getOwner() ) );
							creature.render(canvas, false);
						}
						else 
							culled++;
					}
					canvas.getGL().glEnd();
									
					instance_shader.unbind();
				}
			}
			else
			{			
				if( cam.getScale() > 6 )
				{
					canvas.getGL().glPointSize( 2 );
					canvas.getGL().glBegin( GL.GL_POINTS );
					for( Creature creature: planet.getCreatures() )
					{						
						creature.setColor( getPlayerColor( creature.getOwner() ) );
						creature.render(canvas, true);										
					}
					canvas.getGL().glEnd();
				}
				else
				{					
					for( Creature creature: planet.getCreatures() )
					{
						if( cam.visible( creature.getPosition(), Constants.BOID_SIZE ) )
						{
							gl.glPushMatrix();
							gl.glTranslatef( creature.getPosition().x, creature.getPosition().y, 0 );
							gl.glRotatef( creature.getAngle(), 0, 0, 1 );
							gl.glScalef( creature.getScale(), creature.getScale(), 0 );
							canvas.getGL().glBegin( GL.GL_TRIANGLES );
							creature.setColor( getPlayerColor( creature.getOwner() ) );
							creature.render(canvas, false);
							canvas.getGL().glEnd();
							gl.glPopMatrix();
						}
						else
							culled++;
					}					
				}
			}
			canvas.getGL().glDisable( GL.GL_BLEND );
			creature_timer.pause();
		}
		canvas.getGL().glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );			
			
		canvas.getGL().glLineWidth( 1 );		
		
		colors_set = true;
	}

	public boolean isGlowOn() 
	{	
		return glow;
	}
	
	public boolean isInstancingOn( )
	{
		return instancing;
	}
	
	public OrthoCamera getCamera( )
	{
		return cam;
	}	
	
	public Color allocatePlayerColor( Simulation sim, int id )
	{
		if( id == -1 )
		{
			player_color.put( -1, new Color( 1, 1, 1, 1 ) );
			return new Color( 1, 1, 1, 1 );
		}
		
		if( player_color.containsKey( id ) )
			return player_color.get( id );
		
		Color col = null;
		if( color_idx >= colors.length )
			col = new Color( (float)Math.random(), (float)Math.random(), (float)Math.random(), 1 );
		else
			col = colors[color_idx++];
		player_color.put( id, col );
		return col;
	}
	
	public Color getPlayerColor( int id )
	{
		return player_color.get(id);
	}
	
	public void setSelectedPlanet( Planet planet )
	{
		selected_planet = planet;
	}
	
	public void setRenderAllPaths( boolean value )
	{
		render_all_paths = value;
	}
	
	public void setRenderIsStartPlanet( boolean value )
	{
		render_is_start_planet = value;
	}
	
	Vector2D tmp = new Vector2D();
	
	public void renderPlanetPaths( GLCanvas canvas, Simulation sim, Planet planet )
	{
		if( planet == null )
			return;
		
		GL gl = GLContext.getCurrent().getGL();
		
		gl.glEnable( GL.GL_BLEND );
		gl.glBegin( GL.GL_LINES );	
		gl.glColor4f( 0.3f, 0.3f, 0.7f, 1 );
		for( int id: planet.getReachablePlanets() )
		{
			Planet p = sim.getPlanet( id );
			tmp.set(p.getPosition()).sub(planet.getPosition()).nor();
			gl.glVertex2f( planet.getPosition().x + tmp.x * planet.getRadius(), planet.getPosition().y + tmp.y * planet.getRadius() );
			gl.glVertex2f( p.getPosition().x + tmp.x * -p.getRadius(), p.getPosition().y + tmp.y * -p.getRadius() );
		}
		gl.glEnd();
		gl.glDisable( GL.GL_BLEND );	
		
		planet.renderMesh( canvas, 0.9f, 0.7f, 0.7f, 1 );
		
	}
	
	public void renderPlanetPathsLight( GLCanvas canvas, Simulation sim, Planet planet )
	{
		if( planet == null )
			return;
		
		GL gl = GLContext.getCurrent().getGL();
		
		gl.glEnable( GL.GL_BLEND );
		gl.glLineWidth( 1 );
		gl.glBegin( GL.GL_LINES );		
		gl.glColor4f( 0.1f, 0.1f, 0.1f, 1 );
		for( int id: planet.getReachablePlanets() )
		{
			Planet p = sim.getPlanet( id );
			tmp.set(p.getPosition()).sub(planet.getPosition()).nor();
			gl.glVertex2f( planet.getPosition().x + tmp.x * planet.getRadius(), planet.getPosition().y + tmp.y * planet.getRadius() );
			gl.glVertex2f( p.getPosition().x + tmp.x * -p.getRadius(), p.getPosition().y + tmp.y * -p.getRadius() );
		}		
		gl.glEnd();
		gl.glLineWidth( 1.5f );
		gl.glDisable( GL.GL_BLEND );				
		
	}
	
	
	public void setSimulation(Simulation sim) 
	{	
		player_color.clear();
		colors_set = false;
		color_idx = 0;
	}	
	
	public void dispose( )
	{
		cam.dispose();
		if( screen_fbo != null )
			screen_fbo.dispose();
		
		if( offscreen_fbo != null )
			offscreen_fbo.dispose();
		
		if( fbo != null )
			fbo.dispose();
				
		if( fbo_tmp != null )
			fbo_tmp.dispose();
		
		if( vert_shader != null )
			vert_shader.dispose();
		
		if( hort_shader != null )
			hort_shader.dispose();
		
		if( instance_shader != null )
			instance_shader.dispose();		
		
		for( Texture texture: textures.values() )
			texture.dispose();
		
		Log.println( "[Renderer] disposed" );
	}

	public Texture getTexture(String string) 
	{	
		return textures.get( string );
	}

	public double getRenderTime()
	{	
		return elapsed_seconds.getMean() * 1000;
	}
	
	public int getCulledObjects( )
	{
		return culled;
	}

	public float getFramesPerSecond() {
		return fps;
	}
	
	public double getTreeRenderTime( )
	{
		return tree_render_time.getMean() * 1000;
	}
	
	public double getPlanetRenderTime( )
	{
		return planet_render_time.getMean() * 1000;
	}
	
	public double getCreatureRenderTime( )
	{
		return creature_render_time.getMean() * 1000;
	}
	
	public double getGlowRenderTime( )
	{
		return glow_render_time.getMean() * 1000;
	}
	
	public double getGuiRenderTime( )
	{
		return gui_render_time.getMean() * 1000;
	}
	
	public long getSystemTime( )
	{
		return current_time;
	}
}
