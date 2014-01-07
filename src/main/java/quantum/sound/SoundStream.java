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

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import quantum.utils.FileManager;
import quantum.utils.Log;

/**
 * a sound stream for asynchronously playing Ogg/Vorbis
 * files. Allows looping, pausing and volume/pan control.
 * 
 * @author marzec
 *
 */
public class SoundStream 
{		
	byte[] bytes;
	String file;
	AudioInputStream audio_in;	
	int written = 0;
	int read = 0;
	int channels = 0;
	float pan = 0;
	float volume = 1;
	float r_vol = 1;
	float l_vol = 1;
	
	SoundBuffer buffer;
	int offset = 0;
	private boolean loop;
	
	public SoundStream( String file ) throws Exception
	{				
		this.file = file;
		load( );
	}	
	
	private void load( ) throws Exception
	{
		bytes = new byte[SoundManager.BUFFER_SIZE];
		AudioInputStream in= AudioSystem.getAudioInputStream( new BufferedInputStream( FileManager.readFile(file), 512*512 ) );				
		if (in != null)
		{
			AudioFormat baseFormat = in.getFormat();
			channels = baseFormat.getChannels();
			AudioFormat decodedFormat = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false );

			audio_in = AudioSystem.getAudioInputStream(decodedFormat, in);			
		}
		else
		{
			audio_in = null;			
		}
	}
	
	public SoundStream( SoundBuffer buffer, int buffer_size )
	{
		bytes = new byte[buffer_size];
		this.buffer = buffer;
		this.channels = buffer.getChannels();
	}
	
	public void setLooping( boolean loop )
	{
		this.loop = loop;
	}
	
	public boolean isLooping( )
	{
		return loop;
	}
	
	public void stream( int bytes_to_read )
	{
		synchronized( this )
		{
			if( audio_in == null && buffer == null )
				return;
			
			if( audio_in != null )
			{
	
				read = 0;
				try {
					int len = channels == 2?bytes_to_read: bytes_to_read / 2;
					while( true )
					{
						int ret = audio_in.read( bytes, read , len - read );
						
						if( ret == -1 )
						{
							if( loop )
							{							
														
								try {
									audio_in.close();
								} catch (IOException e) {
								}									
								load( );
								
								ret = 0;
							}
							else
							{
								dispose();
								break;
							}
						}
						
						read += ret;
						if( read == len )
							break;
												
					}
										
				} catch (Exception e) 
				{		
					e.printStackTrace();
					Log.println( "[SoundStream] couldn't stream: " + Log.getStackTrace( e ) );
					dispose();
					return;
				}					
			}
			
			if( buffer != null )
			{
				read = 0;
				int len = channels == 2?bytes_to_read: bytes_to_read / 2;
				for( int i = 0, j=0; i < len; i++, j++ )
				{					
					if( j + offset == buffer.getBuffer().length )
					{
						if( loop )
						{
							offset = 0;
							j = 0;
						}						
						else
						{
							dispose();
							break;
						}
					}
						
					bytes[i] = buffer.getBuffer()[j + offset];					
					read++;
				}
				offset += read;
				
			}
		}
	}

	public int readBytes( )
	{
		return read;
	}
	
	public void setPan( float pan )
	{
		if( pan < -1 )
			pan = -1;
		if( pan > 1 )
			pan = 1;
		this.pan = pan;
		calculateVolume( );
	}
	
	public float getPan( )
	{
		return pan;
	}
	
	public void setVolume( float volume )
	{
		if( volume > 1 )
			volume = 1;
		if( volume < 0 )
			volume = 0;
		this.volume = volume;
		calculateVolume( );
	}
	
	public float getVolume( )
	{
		return volume;
	}
	
	public float getLeftChannelVolume( )
	{
		return l_vol;
	}

	public float getRightChannelVolume( )
	{
		return r_vol;
	}
	
	private void calculateVolume( )
	{
		if( pan < 0 )
		{
			l_vol = volume; //Math.abs( volume );
			r_vol = (1 - Math.abs( pan )) * volume;
		}
		else
		{			
			l_vol = (1 - Math.abs( pan )) * volume;
			r_vol = volume; //Math.abs( volume );		
		}
	}
	
	public byte[] getBuffer( )
	{
		return bytes;
	}
	
	public int getChannels( )
	{
		return channels;
	}
	
	public boolean finished( )
	{
		return audio_in == null && buffer == null;
	}

	public void dispose( )
	{
		synchronized( this )
		{
			if( audio_in != null )
			{
				try {
					audio_in.close();
				} catch (IOException e) {
				}
			}
			buffer = null;
			audio_in = null;
		}
	}

	public static void main( String[] argv ) throws Exception
	{
		SoundStream stream = new SoundStream( "dat/sounds/demo3.ogg" );

		while(!stream.finished() )
		{
			stream.stream( 4096 );
			Thread.sleep( 5 );
		}

		stream.dispose();
	}
}
