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
 * a simple timer class with start and stop
 * functionality as well as averaging n time
 * measurements.
 * 
 * @author mzechner@know-center.at
 *
 */
public class Timer
{
	// start time in nano seconds for the current measurement, -1 == not running	
	protected long start_time = -1;
	// n last stopped times in seconds
	protected double[] elapsed_time = null;
	// index to elapsed_time indicating the last measured time
	protected int curr_elapsed_time = 0;
	
	public Timer( )
	{
		elapsed_time = new double[1];		
	}
	
	/**
	 * constructor, avg_num defines how many measurements
	 * should be saved for averaging the timing, must be 
	 * >= 1
	 * 
	 * @param avg_num
	 */
	public Timer( int avg_num )
	{
		assert avg_num > 0 ;
		elapsed_time = new double[avg_num];
	}
	
	/**
	 * starts a new measurement or resumes a previously
	 * paused measurement
	 */
	public void start( )
	{
		start_time = System.nanoTime();
	}
	
	/**
	 * pauses the current measurement, use start to 
	 * resume it.
	 */
	public void pause( )
	{
		elapsed_time[curr_elapsed_time] += (System.nanoTime() - start_time ) / 1000000000.0;
		start_time = -1;
	}
	
	/**
	 * stops the current measurement and stores the
	 * final result. 
	 */
	public void stop( )
	{
		elapsed_time[curr_elapsed_time] += (System.nanoTime() - start_time ) / 1000000000.0;
		curr_elapsed_time++;
		curr_elapsed_time %= elapsed_time.length;
		elapsed_time[curr_elapsed_time] = 0;
		start_time = -1;
	}
	
	/**
	 * resets all measurements
	 */
	public void reset( )
	{
		for( int i = 0; i < elapsed_time.length; i++ )
			elapsed_time[i] = 0;
		
		curr_elapsed_time = 0;
		start_time = -1;
	}
	
	/**
	 * returns wheter the timer is running
	 * or stopped/paused
	 * @return
	 */
	public boolean isRunning( )
	{
		return start_time != -1;
	}
	
	/**
	 * returns the elapsed time of the current
	 * measurement in seconds
	 * @return
	 */
	public double getElapsedSeconds( )
	{
		if( isRunning() )		
			return elapsed_time[curr_elapsed_time] + ( System.nanoTime() - start_time ) / 1000000000.0;
		else
			return elapsed_time[curr_elapsed_time-1<0?elapsed_time.length-1:curr_elapsed_time-1];
	}
	
	/**
	 * returns the average over the last measured
	 * times
	 * @return
	 */
	public double getAverageElapsedSeconds( )
	{
		double time = 0;
		int n = 0;
		for( int i = 0; i < elapsed_time.length; i++ )
		{
			if( elapsed_time[i] != 0 )
			{
				time += elapsed_time[i];
				n++;
			}
		}				
		time /= n!=0?n:1;				
		return time;
	}
	
}
