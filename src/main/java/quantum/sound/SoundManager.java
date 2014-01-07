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
package quantum.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import quantum.utils.FileManager;
import quantum.utils.Log;

/**
 * a simple sound manager based on the vorbisspi by
 * javazoom. This thing is a fake singleton executing
 * asynchronously in a thread which is automatically
 * created and destroyed on program exit.
 * 
 * @author marzec
 *
 */
public class SoundManager 
{
	static Thread thread;
	static final ArrayList<SoundStream> streams = new ArrayList<SoundStream>();
	static boolean disposed = false;
	static SourceDataLine audio_line;
	static byte[] bytes;
	static HashMap<String, SoundBuffer> buffers = new HashMap<String, SoundBuffer>();
	static float buffer_volume = 0.75f;
	static float buffer_pan = 0;
	
	static final int BUFFER_SIZE = 4096 * 4;
	static
	{
		try
		{
			AudioFormat decodedFormat = new AudioFormat( 44100, 16,2, true, false );	
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);			
			audio_line = (SourceDataLine) AudioSystem.getLine(info);
			audio_line.open(decodedFormat, BUFFER_SIZE);
			audio_line.start();
			bytes = new byte[BUFFER_SIZE];
			Log.println( "[SoundManager] buffers size: " + bytes.length );

			thread = new Thread( new Runnable( ) 
			{
				public void run( )
				{
					while( !disposed )
					{
						update( );
						try {
							Thread.sleep( 3 );
						} catch (InterruptedException e) 
						{						
							e.printStackTrace();
						}
					}
				}
			});	

			thread.start();
			
			SoundManager.loadSoundBuffer( "sounds/tree.ogg", "tree" );
			SoundManager.loadSoundBuffer( "sounds/move.wav", "move" );
			SoundManager.loadSoundBuffer( "sounds/chain.wav", "chain" );
			SoundManager.loadSoundBuffer( "sounds/unchain.ogg", "unchain" );
		}
		catch( Exception ex )
		{
			Log.println( "[SoundManager] couldn't create audio line: " + Log.getStackTrace( ex ) );
		}
	}

	private SoundManager( )
	{

	}
		
	private static void update( )
	{			
		int read = 0;
		synchronized( streams )
		{									
			Thread.currentThread().setName( "SoundManager" );
			Iterator<SoundStream> iter = streams.iterator();		
			
			if( audio_line.available() == 0 )
				return;
			
			while( iter.hasNext() )
			{
				SoundStream stream = iter.next();
				stream.stream( audio_line.available() );									
									
				
				if( stream.getChannels() == 2 )
				{
					read = Math.max( read, stream.readBytes() );
					for( int i = 0; i < stream.readBytes(); i+=2 )
					{						
						byte low = stream.getBuffer()[i];
						byte high = stream.getBuffer()[i+1];
						
						int val = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff ); 							
						
						if( (i / 2) % 2 == 0 )
							val = (int)(val * stream.getRightChannelVolume() );
						else
							val = (int)(val * stream.getLeftChannelVolume() );
						
						low = bytes[i];
						high = bytes[i+1];
						int val2 = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff );
						
						val += val2;													
						
						if( val > Short.MAX_VALUE )
							val = Short.MAX_VALUE;
						if( val < Short.MIN_VALUE )
							val = Short.MIN_VALUE;
						
						low = (byte)(val & 0xff);
						high = (byte)((val & 0xff00) >> 8 );
						
						bytes[i] = low;
						bytes[i+1] = high;
					}
				}	
				else
				{
					read = Math.max( read, stream.readBytes() * 2 );
					int j = 0;
					for( int i = 0; i < stream.readBytes() * 2; i+=2, j+=2 )
					{						
						byte low = stream.getBuffer()[j];
						byte high = stream.getBuffer()[j + 1];
						
						int val = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff ); 							
						
						val = (int)( val * stream.getRightChannelVolume() );
						
						low = bytes[i];
						high = bytes[i+1];
						int val2 = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff );
						
						val += val2;													
						
						if( val > Short.MAX_VALUE )
							val = Short.MAX_VALUE;
						if( val < Short.MIN_VALUE )
							val = Short.MIN_VALUE;
						
						low = (byte)(val & 0xff);
						high = (byte)((val & 0xff00) >> 8 );																			
						
						bytes[i] = low;
						bytes[i+1] = high;
						
						i+=2;
						
						low = stream.getBuffer()[j];
						high = stream.getBuffer()[j + 1];
						
						val = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff ); 							
						
						val = (int)( val * stream.getLeftChannelVolume() );
						
						low = bytes[i];
						high = bytes[i+1];
						val2 = ( (  ( high & 0x80 ) == 0x80 ? 0xffff: 0 ) << 16 ) | ( high << 8 ) & 0xff00 | ( low & 0xff );
						
						val += val2;													
						
						if( val > Short.MAX_VALUE )
							val = Short.MAX_VALUE;
						if( val < Short.MIN_VALUE )
							val = Short.MIN_VALUE;
						
						low = (byte)(val & 0xff);
						high = (byte)((val & 0xff00) >> 8 );
						
						bytes[i] = low;
						bytes[i+1] = high;
					}
				}
				
				if( stream.finished() )
				{					
					iter.remove();
				}						
			}
		}
						
		if( read != 0 )
		{
			int written = audio_line.write( bytes, 0, read );
			while( written != read)
				written += audio_line.write( bytes, written, read - written );
		}
		else
		{
			audio_line.write( bytes, 0, bytes.length );
		}
		
//		Log.println( "available: " + audio_line.available() + "/" + audio_line.getBufferSize() + ", read: " + read );
		
		for( int i = 0; i < bytes.length; i++ )
			bytes[i] = 0;	
	}

	public static void stopAll( )
	{
		synchronized( streams )
		{
			for( SoundStream stream: streams )
				stream.dispose();
			
			streams.clear();
		}
	}
	
	public static void playStream( SoundStream stream )
	{
		synchronized( streams )
		{
			streams.add( stream );
		}
	}
	
	public static SoundStream playStream( String file )
	{
		if( audio_line == null )
			return null;
		
		SoundStream stream;
		try {
			stream = new SoundStream( file );
			synchronized( streams )
			{
				streams.add( stream );
			}
			return stream;
		} catch (Exception e) {
			Log.println( "[SoundManager] couldn't stream sound '" + file + "': " + Log.getStackTrace(e) );
		}
		return null;
	}
	
	
	public static void setBufferVolume( float volume )
	{
		buffer_volume = volume;
	}
	
	public static void setBufferPan( float pan )
	{
		buffer_pan = pan;
	}
	
	public static void playBuffer( String handle )
	{
		if( !buffers.containsKey( handle ) )
			return;
		
		synchronized( streams )
		{
			SoundStream stream = new SoundStream( buffers.get( handle ), BUFFER_SIZE );
			stream.setVolume( buffer_volume );
			stream.setPan( buffer_pan );
			streams.add( stream );
		}
	}
	
	public static void loadSoundBuffer( String file, String handle ) throws Exception
	{
		SoundBuffer buffer = new SoundBuffer( FileManager.readFile( file ) );
		buffers.put( handle, buffer );
	}

	public static int playingStreams( )
	{
		synchronized( streams )
		{
			return streams.size();
		}
	}
	
	public static void dispose( )
	{		
		if( thread != null )
		{
			disposed = true;
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public static void main( String[] argv ) throws Exception
	{		
		SoundStream stream = new SoundStream( "220.ogg" );		
		stream.setLooping( true );
		SoundManager.playStream( stream );
		Thread.sleep( 4000 );
		System.out.println( "finished" );
		SoundManager.dispose( );
	}
}
