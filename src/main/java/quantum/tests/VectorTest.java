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
package quantum.tests;

import quantum.math.Vector2D;

public class VectorTest 
{
	public static void main( String[] argv )
	{
		Vector2D a = new Vector2D( );
		Vector2D b = new Vector2D( 10, 10 );
		
		for( int i = 0; i < 100000; i++ )
		{
			a.dst(b);
			a.nor();
		}
		
		long start = System.nanoTime();
		for( int i = 0; i < 400000; i++ )
			a.dst(b);
		System.out.println( "400000 dsts: " + (System.nanoTime() - start) / 1000000.0 + "ms");
		
		
		start = System.nanoTime();
		for( int i = 0; i < 400000; i++ )
			a.nor();
		System.out.println( "400000 dsts: " + (System.nanoTime() - start) / 1000000.0 + "ms");
	}
}
