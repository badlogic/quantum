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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import quantum.utils.FileManager;

public class SoundBuffer 
{	
	int channels;
	byte[] bytes;
	
	public SoundBuffer(String file) throws Exception 
	{
		AudioInputStream in= AudioSystem.getAudioInputStream( new BufferedInputStream( new FileInputStream( FileManager.newFile( file ) ), 512*512 ) );		
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

			AudioInputStream audio_in = AudioSystem.getAudioInputStream(decodedFormat, in);			
			
			byte[] bytes = new byte[2048];
			ByteArrayOutputStream buffer = new ByteArrayOutputStream( );
			int read = 0;
			while( ( read = audio_in.read(bytes) ) != -1 )
			{
				buffer.write( bytes, 0, read );
			}				
			
			audio_in.close();
			this.bytes = buffer.toByteArray();
		}
		else
		{
			bytes = new byte[0];
		}
	}	
	
	public SoundBuffer(InputStream file) throws Exception 
	{
		AudioInputStream in= AudioSystem.getAudioInputStream( new BufferedInputStream( file , 512*512 ) );		
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

			AudioInputStream audio_in = AudioSystem.getAudioInputStream(decodedFormat, in);			
			
			byte[] bytes = new byte[2048];
			ByteArrayOutputStream buffer = new ByteArrayOutputStream( );
			int read = 0;
			while( ( read = audio_in.read(bytes) ) != -1 )
			{
				buffer.write( bytes, 0, read );
			}				
			
			audio_in.close();
			this.bytes = buffer.toByteArray();
		}
		else
		{
			bytes = new byte[0];
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
}
