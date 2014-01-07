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
 * a simple class keeping track
 * of the mean of a stream of values
 * within a certain window. the WindowedMean
 * will only return a value in case enough
 * data has been sampled. After enough
 * data has been sampled the oldest sample
 * will be replace by the newest in case
 * a new sample is added.
 * 
 * @author marzec
 *
 */
public strictfp class WindowedMean 
{
	double values[];
	int added_values = 0;
	int last_value;	
	double mean = 0;
	boolean dirty = true;
	
	/**
	 * constructor, window_size specifies
	 * the number of samples we will continuously
	 * get the mean and variance from. the
	 * class will only return meaning full values
	 * if at least window_size values have been
	 * added.
	 * 
	 * @param window_size size of the sample window
	 */
	public WindowedMean( int window_size )
	{
		values = new double[window_size];		
	}
	
	/**
	 * @return wheter the value returned will be meaningfull
	 */
	public boolean hasEnoughData()
	{
		return added_values >= values.length;
	}
	
	/**
	 * clears this WindowedMean. The class will
	 * only return meaningfull values after enough
	 * data has been added again.
	 */
	public void clear( )
	{
		added_values = 0;
		last_value = 0;
		for( int i = 0; i < values.length; i++ )
			values[i] = 0;
		dirty = true;
	}
	
	/**
	 * adds a new sample to this mean. in case the
	 * window is full the oldest value will be replaced
	 * by this new value.
	 * 
	 * @param value
	 */
	public void addValue( double value )
	{
		added_values++;
		values[last_value++] = value;
		if( last_value > values.length - 1 )
			last_value = 0;
		dirty = true;
	}
	
	/**
	 * returns the mean of the samples added
	 * to this instance. only returns meaningfull
	 * results when at least window_size samples
	 * as specified in the constructor have been
	 * added.
	 * @return
	 */
	public double getMean( )
	{
		if( hasEnoughData() )
		{
			if( dirty == true )
			{
				double mean = 0;			
				for( int i = 0; i < values.length; i++ )
					mean += values[i];
				
				this.mean = mean / values.length;				
				dirty = false;
			}
			return this.mean;
		}			
		else
			return 0;
	}
	
	/**
	 * returns the variance of the data. this will
	 * only return a meaningfull value when enough
	 * data has been added ( >= window_size as
	 * specified in the constructor ).
	 * @return
	 */
	public double getVariance( )
	{
		if( hasEnoughData() )
		{
			double mean = getMean();
			double variance = 0;
			for( int i = 0; i < values.length; i++ )
			{
				variance +=  ( values[i] - mean ) * ( values[i] - mean );
			}
			variance /= values.length;
			variance = Math.sqrt( variance );
			return variance;
		}
		else
			return 0;
	}
}
