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
package quantum.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import quantum.game.Constants;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class AutoUpdater 
{
	boolean update_available = false;
	String package_string = "";
	boolean update_done = false;
	boolean update_failed = false;
	boolean updating = false;
	long download_size = 0;
	long read_size = 0;
	
	public AutoUpdater( ) throws Exception
	{
		try {			
			URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/versions.php" );
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout( 2000 );
			connection.connect();
			BufferedReader in = new BufferedReader(	new InputStreamReader( connection.getInputStream() ));

			Pattern p = Pattern.compile("Quantum-[0-9]+\\.[0-9]+-pack.zip");			

			String line = null;
			ArrayList<String> packages = new ArrayList<String>();
			while( (line = in.readLine()) != null )
			{				
				Matcher matcher = p.matcher( line );
				if( matcher.find() )
					packages.add( line.substring( matcher.start(), matcher.end() ) );
			}			
			in.close();

			p = Pattern.compile( "[0-9]+\\.[0-9]+" );
			int version = Constants.VERSION;
			for( String pack: packages )
			{
				Matcher matcher = p.matcher( pack );
				matcher.find();
				int package_version = Integer.parseInt( pack.substring( matcher.start(), matcher.end() ).replace( ".", "" ) );
				if( package_version > version )
				{
					version = package_version;
					package_string = pack;
				}
			}

			if( version != Constants.VERSION )
				update_available = true;

		} catch (Exception e) {
			Log.println( "[AutoUpdater] couldn't load update site" );
			throw e;
		}			
	}

	public boolean updateAvailable( )
	{
		return update_available;
	}

	public boolean updateFailed( )
	{
		return update_failed;
	}

	public boolean updateDone( )
	{
		return update_done;
	}
	
	public boolean isUpdating( )
	{
		return updating;
	}

	public void update( ) throws Exception
	{
		if( package_string.equals( "" ) )
			throw new RuntimeException( "No update available" );

		update_done = false;
		update_failed = false;

		Thread t = new Thread( new Runnable( ) {

			public void run( )
			{
				File file = FileManager.newFile( "tmp" + System.nanoTime() + ".zip" );
				try
				{					
					BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( file ) );

					URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/" + package_string );
					URLConnection connection = url.openConnection(); 
					connection.setConnectTimeout( 2000 );
					connection.connect();
					InputStream in = connection.getInputStream();

					download_size = connection.getContentLength();
					read_size = 0;
					
					byte[] buf = new byte[4 * 1024]; 
					int bytesRead;
					while ((bytesRead = in.read(buf)) != -1) {
						out.write(buf, 0, bytesRead);
						read_size += bytesRead;
					}		

					out.close();
					in.close();				
					
				}
				catch( Exception ex )
				{					
					Log.println( "[AutoUpdater] update failed: " + Log.getStackTrace( ex ) );
					update_failed = true;
					file.delete();
				}
				
				update_done = true;
			}
		} );

		t.start();
		updating = true;
	}
	
	public static void unzip( File file ) throws Exception
	{
		final int BUFFER = 2048;		   
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(file);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
//				System.out.println("Extracting: " +entry);
				int count;
				byte data[] = new byte[BUFFER];

				if( entry.isDirectory() )
				{					
					String[] dirs = entry.getName().split( "/" );
					String dir = "";
					for( int i = 0; i < dirs.length; i++ )
					{						
						String d = dirs[i];
						if( d.equals( "quantum" ) )
							continue;	
						new File( dir + d ).mkdir();
						dir += d + "/" ;							
					}					
				}
				else
				{
					if( entry.getName().contains( "/" ) )
					{
						String[] dirs = entry.getName().split( "/" );
						String dir = "";
						for( int i = 0; i < dirs.length - 1; i++ )
						{
							String d = dirs[i];
							if( d.equals( "quantum" ) )
								continue;	
							new File( dir + d ).mkdir();
							dir += d + "/";							
						}
					}
					
					FileOutputStream fos = new 
					FileOutputStream(entry.getName().replace( "quantum/", "" ));
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) 
							!= -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				}
			}
			zis.close();
		} catch(Exception e) {
			throw e;
		}
	}
	
	public long totalSize( )
	{
		return download_size;
	}
	
	public long readSize( )
	{
		return read_size;
	}

	public static void main( String[] argv ) throws Exception
	{
		new AutoUpdater();
				
//		updater.unzip( new File( "tmp/tmp49013284280823.zip" ) );
		
//		if( updater.updateAvailable() )
//		{
//			updater.update();
//
//			System.out.println( "updating" );
//
//			while( updater.updateDone() == false )
//			{
//				Thread.sleep( 100 );
//				System.out.print( "." );
//			}
//
//			if( updater.updateFailed( ) )
//				System.out.println( "update failed!" );
//			else
//				System.out.println( "update successfull!" );
//		}
	}
}
