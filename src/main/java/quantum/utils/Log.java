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

import java.io.PrintStream;

public class Log 
{
	static PrintStream out = null;
	
	static 
	{
		try
		{
			out = new PrintStream( FileManager.writeFile( "log.txt" ) );
		}
		catch( Exception ex )
		{
			System.out.println( "couldn't open log file!" );
		}
	}
	
	static StringBuffer buffer = new StringBuffer( );
	static boolean use_sys_out = true;
	static boolean log_to_file = true;
	
	public synchronized static void setUseSystemOut( boolean use_sys_out )
	{
		Log.use_sys_out = use_sys_out;
	}
	
	public synchronized static void setLogToFile( boolean log_to_file )
	{
		Log.log_to_file = log_to_file;
	}
	
	public static synchronized void println( String text )
	{
		if( use_sys_out )
			System.out.println( text );
		
		if( log_to_file && out != null )		
		{
			out.println( text );
			out.flush( );
		}
		
		buffer.append( text );
		buffer.append( "\n" );
	}
	
	public static synchronized void print( String text )
	{
		if( use_sys_out )
			System.out.print( text );
		
		if( log_to_file && out != null )		
		{
			out.print( text );
			out.flush( );
		}
		
		buffer.append( text );
	}
	
	public synchronized static String getLog( )
	{
		return buffer.toString();
	}	
	
	public synchronized static void close( )
	{
		if( out != null )
		{
			out = null;
			out.close();
		}
	}
	
	public synchronized static String getStackTrace( Exception e )
	{
		e.fillInStackTrace();
		StringBuffer buffer = new StringBuffer( );
		buffer.append( e.getMessage() + "\n" );
		for( StackTraceElement el: e.getStackTrace() )
			buffer.append( el.toString() + "\n" );
		
		return buffer.toString();
	}
}
