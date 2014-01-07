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
package quantum;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import quantum.utils.FileManager;

public class Config 
{
	String last_name = "";
	String last_ip = "";
	String last_port = "";
	int last_x;
	int last_y;
	int last_width;
	int last_height;
	float last_delay;
	float volume_music;
	float volume_sfx;
	
	public Config( )
	{
		read( );
	}
	
	private void read( )
	{
		this.last_name = "";
		this.last_ip = "";
		this.last_port = "";
		this.last_width = 921;
		this.last_height= 856;
		this.last_x = 0;
		this.last_y = 0;
		this.volume_music = 0.75f;
		this.volume_sfx = 0.75f;
		
		File file = FileManager.newFile( "config.dat" );
		if( file.exists() == false )
			return;
		
		try {
			BufferedReader reader = new BufferedReader( new InputStreamReader( FileManager.readFile( file ) ) );
			String name = reader.readLine();
			String ip = reader.readLine();
			String port = reader.readLine();
			this.last_name = name;
			this.last_ip = ip;
			this.last_port = port;
			this.last_width = Integer.parseInt( reader.readLine() );
			this.last_height = Integer.parseInt( reader.readLine() );
			this.last_x = Integer.parseInt( reader.readLine() );
			this.last_y = Integer.parseInt( reader.readLine() );
			this.last_delay = Float.parseFloat( reader.readLine() );
			this.volume_music = Float.parseFloat( reader.readLine() );
			this.volume_sfx = Float.parseFloat( reader.readLine() );
			reader.close();
			
		} catch (Exception e) 
		{		
			this.last_name = "";
			this.last_ip = "";
			this.last_port = "";
			this.last_width = 921;
			this.last_height= 856;
			this.last_x = 0;
			this.last_y = 0;
			this.last_delay = 0.2f;
		}		
	}
	
	protected void write( Rectangle window_bounds )
	{
		try {
			BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( FileManager.writeFile( "config.dat" ) ) );
			writer.write( last_name + "\n" );
			writer.write( last_ip + "\n" );
			writer.write( last_port + "\n" );
			Rectangle bounds = window_bounds;
			writer.write( bounds.width + "\n" );
			writer.write( bounds.height + "\n" );
			writer.write( bounds.getBounds().x + "\n" );
			writer.write( bounds.getBounds().y + "\n" );
			writer.write( last_delay + "\n" );
			writer.write( volume_music + "\n" );
			writer.write( volume_sfx + "\n" );
			writer.close();
			
		} catch (Exception e) 
		{		
		}		
	}	
	
	public String getName( )
	{
		return last_name;
	}
	
	public String getIp( )
	{
		return last_ip;
	}
	
	public String getPort( )
	{
		return last_port;
	}
	
	public int getWidth( )
	{
		return last_width;
	}
	
	public int getHeight( )
	{
		return last_height;
	}
	
	public int getX( )
	{
		return last_x;
	}
	
	public int getY( )
	{
		return last_y;
	}
	
	public float getDelay( )
	{
		return last_delay;
	}
	
	public void setName( String name )
	{
		last_name = name;				
	}
	
	public void setIp( String ip )
	{
		last_ip = ip;
	}
	
	public void setPort( String port )
	{
		last_port = port;
	}
	
	public void setDelay( float delay )
	{
		last_delay = delay;
	}
	
	public float getVolumeMusic( )
	{
		return volume_music;
	}
	
	public float getVolumeSfx( )
	{
		return volume_sfx;
	}
	
	public void setVolumeMusic( float music )
	{
		volume_music = music;
	}
	
	public void setVolumeSfx( float sfx )
	{
		volume_sfx = sfx;
	}
}
