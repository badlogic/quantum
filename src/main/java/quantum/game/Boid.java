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
package quantum.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.game.Constants;
import quantum.math.Vector2D;


public strictfp class Boid extends GameObject
{		
	public strictfp static class SteeringBehaviour
	{
		public static int None = 0;
		public static int Seek = 1;
		public static int Flee = 2;
		public static int Arrive = 3;		
		public static int Circle = 4;
		public static int Attack = 5;
	}
	
	Vector2D vel = new Vector2D( 1, 0);
	Vector2D force = new Vector2D( 0, 0 );
	Vector2D tmp = new Vector2D( );
	Vector2D tmp2 = new Vector2D( );
	int target;
	int behaviour = SteeringBehaviour.None;
	float angle = 0;
	float max_speed = Constants.BOID_MAX_SPEED;
	
	protected Boid( Simulation sim )
	{
		super( sim );		
	}
	
	public float getMaxSpeed( )
	{
		return max_speed;
	}
	
	public Boid( Simulation sim, float x, float y, float angle )
	{		
		super( sim, new Vector2D( x, y ) );
		vel.x = (float)Math.cos( Math.toRadians(angle) );
		vel.y = (float)Math.sin( Math.toRadians(angle) );
		vel.mul( 10 );
		this.angle = angle;
	}	
	
	public Vector2D getVelocity( )
	{
		return vel;
	}	
	
	public float getAngle( )
	{
		return angle;
	}
	
	public void render( GLCanvas canvas )
	{
		GL gl = canvas.getGL();					
		gl.glColor3f( 0, 0.8f, 0 );
		float angle_rad = (float)Math.toRadians( angle );		
		gl.glNormal3f( angle_rad, pos.x, pos.y );
		gl.glVertex2f( -1 * Constants.BOID_SIZE, 0.5f * Constants.BOID_SIZE );			
		gl.glVertex2f( -1 * Constants.BOID_SIZE, -0.5f * Constants.BOID_SIZE );			
		gl.glVertex2f( 1 * Constants.BOID_SIZE, 0 );					
	}
	
	public void steer( int behaviour, int target )
	{	
		this.behaviour = behaviour;
		this.target = target;
	}
	
	public void update( )
	{
		if( behaviour == SteeringBehaviour.Seek )
			updateSeek( );
		
		if( behaviour == SteeringBehaviour.Flee )
			updateFlee( );
		
		if( behaviour == SteeringBehaviour.Arrive )
			updateArrive( );
		
		if( behaviour == SteeringBehaviour.Circle )
			updateCircle( );
		
		if( behaviour == SteeringBehaviour.Attack )
			updateAttack( );
		
		if( behaviour == SteeringBehaviour.None )
			updateNone( );			
								
		if( force.len() > Constants.BOID_MAX_FORCE )
			force.nor().mul( Constants.BOID_MAX_FORCE );
		
		tmp.set(vel);
		
		vel.add(force);
		if( vel.len() > getMaxSpeed() )
			vel.nor().mul( getMaxSpeed() );
		
		pos.add(vel);
		angle = (float)(Math.toDegrees(Math.atan2( vel.y, vel.x)));
					
	}
	
	private void updateAttack() 
	{
		force.set( 0, 0 );
		GameObject obj = sim.getObject(target);
		if( obj == null )
		{
			return;
		}
			
		tmp.set(obj.getPosition());	
		if( obj instanceof Creature )
		{
			tmp.add( ((Creature)obj).vel.x, ((Creature)obj).vel.y );
		}
		
		tmp.sub(pos).nor().mul(getMaxSpeed());
		force.set(tmp).sub(vel);	
		
		
	}

	protected void updateSeek( )
	{
		force.set( 0, 0 );
		GameObject obj = sim.getObject(target);
		if( obj == null )
		{
			return;
		}
			
		tmp.set(obj.getPosition());
		
		tmp.sub(pos).mul(getMaxSpeed());
		force.set(tmp).sub(vel);
	}
	
	protected void updateFlee( )
	{
		tmp.set(pos).sub(sim.getObject(target).pos).mul(getMaxSpeed());
		force.set(tmp).sub(vel);
	}
	
	protected void updateArrive( )
	{
		if( pos.dst2(sim.getObject(target).pos) < 100 )
			behaviour = SteeringBehaviour.None;
		tmp.set(sim.getObject(target).pos).sub(pos).mul(getMaxSpeed());
		force.set(tmp).sub(vel);		
	}
	
//	protected void updateCircle( )
//	{
//		Planet planet = sim.getPlanet(target);		
//		tmp.set(planet.getPosition()).sub(pos);		
//		
//		if( tmp.len() < planet.getRadius() + Constants.BOID_MIN_ORBIT )
//		{							
//			force.set(tmp.y, -tmp.x).mul( handedness );
//			if( vel.len() > max_speed * 0.8 )
//				vel.mul(0.90f);
//		}
//		else
//		if( tmp.len() > planet.getRadius() + Constants.BOID_MAX_ORBIT )
//		{		
//			force.set(tmp.mul(1));			
//		}
//		else
//		{
//			force.set(vel);			
//		}
//	}
	
	protected void updateCircle( )
	{
		Planet planet = sim.getPlanet(target);		
		tmp.set(planet.getPosition()).sub(pos);
		float len = tmp.len();
		
		force.set( 0, 0 );
		
		float x = tmp2.set( vel.x, vel.y ).nor().dot( tmp );
		float y = tmp2.set( - vel.y, vel.x ).nor().dot( tmp );

		if( len > planet.getRadius() + Constants.BOID_MAX_ORBIT )
		{
			if( y < 0 )
				force.add( vel.y, -vel.x );
			else
				force.add( -vel.y, vel.x );
			return;
		}										
		
		if( x < 0 || x - planet.getRadius() > Constants.BOID_MIN_ORBIT )
		{		
			force.add( vel );
			return;
		}		
		
		tmp2.set(pos).add(vel.x * 20, vel.y * 20 );
		float len2 = tmp2.dst( planet.getPosition() ); 
		if(  len2 < planet.getRadius() + Constants.BOID_MIN_ORBIT )
		{
			if( len2 < planet.getRadius() + Constants.BOID_MIN_ORBIT )
				vel.mul( 0.93f );			
			
			if( y < 0 )
				force.add( -vel.y, vel.x );
			else
				force.add( vel.y, -vel.x );
		}					
	}
	
	protected void updateNone( )
	{
		if( vel.len() < Constants.BOID_MIN_SPEED )
			return;
		
		vel.mul( Constants.BOID_DECCELARATION );			
		force.set( 0, 0 );
	}
	
	public void read( DataInputStream in ) throws Exception
	{
		super.read(in);
		vel.x = in.readFloat();
		vel.y = in.readFloat();
		force.x = in.readFloat();
		force.y =in.readFloat();
		target = in.readInt();
		angle = in.readFloat();
		behaviour = in.readInt( );
	}
	
	public void write( DataOutputStream out ) throws Exception
	{
		super.write(out);
		out.writeFloat( vel.x);
		out.writeFloat( vel.y );
		out.writeFloat( force.x );
		out.writeFloat( force.y );
		out.writeInt( target );
		out.writeFloat( angle );
		out.writeInt( behaviour );
	}
}
