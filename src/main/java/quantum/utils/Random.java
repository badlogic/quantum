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

/**
 * a fast random number generator taken from
 * 
 * http://www.qbrundage.com/michaelb/pubs/essays/random_number_generation.html
 * 
 * faster then Math.random and java.util.Random
 * 
 * @author mzechner
 *
 */
public final strictfp class Random {
    private int mt_index;
    private int[] mt_buffer = new int[624];

    public Random(long seed) {
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < 624; i++)
            mt_buffer[i] = r.nextInt();
        mt_index = 0;
    }

    private int random() {
        if (mt_index == 624)
        {
            mt_index = 0;
            int i = 0;
            int s;
            for (; i < 624 - 397; i++) {
                s = (mt_buffer[i] & 0x80000000) | (mt_buffer[i+1] & 0x7FFFFFFF);
                mt_buffer[i] = mt_buffer[i + 397] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
            }
            for (; i < 623; i++) {
                s = (mt_buffer[i] & 0x80000000) | (mt_buffer[i+1] & 0x7FFFFFFF);
                mt_buffer[i] = mt_buffer[i - (624 - 397)] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
            }
        
            s = (mt_buffer[623] & 0x80000000) | (mt_buffer[0] & 0x7FFFFFFF);
            mt_buffer[623] = mt_buffer[396] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
        }
        return mt_buffer[mt_index++];
    }
    
    /**
     * @return a random number between 0 and 1
     */
    public double rand( )
    {    	
    	return ((long)random() + -(long)Integer.MIN_VALUE) / 4294967295.0;
    }
    
    /**
     * return a number between min and max
     * @param min minimum value
     * @param max maximum value
     * @return a value
     */
    public double rand( double min, double max )
    {
    	double r = rand();
    	r = min + ( max - min ) * r;
    	return r;
    }
    
    public static void main( String[] argv )
    {
    	Random rand = new Random(10);
    	
    	for( int i = 0; i < 10; i++ )
    		System.out.println( rand.rand() );
    	
    	long start = System.nanoTime(); 
    	for( int i = 0; i < 100000000; i++ )
    		Math.random();
    	System.out.println( "Math.random() took " + (System.nanoTime()-start)/ 1000000000.0 + " secs" );
    	
    	java.util.Random r = new java.util.Random();
    	for( int i = 0; i < 100000000; i++ )
    		r.nextDouble();
    	System.out.println( "java.util.Random.nextDouble() took " + (System.nanoTime()-start)/ 1000000000.0 + " secs" );
    	
    	start = System.nanoTime(); 
    	for( int i = 0; i < 100000000; i++ )
    		rand.random();
    	System.out.println( "Random.random() took " + (System.nanoTime()-start)/ 1000000000.0 + " secs" );        	    
    	
    	for( int i = 0; i < 10; i++ )
    		System.out.println( rand.random() );
    	
    	for( int i = 0; i < 10; i++ )
    		System.out.println( Math.random() );
    }
}


