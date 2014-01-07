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
package quantum.utils;


public strictfp class RandomSequence 
{
	int sequence[];
	int count = 0;
	
	public RandomSequence( int n )
	{
		sequence = new int[n];
		for( int i = 0; i < n; i++ )
			sequence[i] = i;
		
		Random rand = new Random( System.nanoTime() );
		for( int i = 0; i < n; i++ )
		{
			int idx =(int)rand.rand(0, n-1);
			int tmp = sequence[idx];
			sequence[idx] = sequence[i];
			sequence[i] = tmp;
		}
	}
	
	public int getNext( )
	{
		count++;
		if( count > sequence.length -1 )
			count = 0;
		return sequence[count];
	}
}
