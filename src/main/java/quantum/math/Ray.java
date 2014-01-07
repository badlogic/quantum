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
package quantum.math;

/**
 * simple ray class
 * 
 * @author mzechner@know-center.at
 *
 */
public class Ray 
{
	protected Vector start = new Vector();
	protected Vector dir = new Vector();	
	
	public Ray( Vector start, Vector dir )
	{
		this.start.set(start);
		this.dir.set(dir).nor();
	}
	
	public Ray cpy()
	{
		return new Ray( this.start, this.dir );
	}
	
	public Vector getEndPoint( float distance )
	{
		return new Vector( start ).add( dir.tmp().mul( distance ) );
	}
	
	public Vector getStartPoint( )
	{
		return start;
	}
	
	public Vector getDirection( )
	{
		return dir;
	}
	
	public Ray mul( Matrix matrix )
	{
		Vector tmp_end = new Vector( start ).add( dir.tmp() );
		tmp_end.mul( matrix );
		start.mul( matrix );
		dir.set( tmp_end.sub( start ) );
		return this;
	}
	
	public String toString()
	{
		return "ray [" + start + ":" + dir + "]";
	}
}
