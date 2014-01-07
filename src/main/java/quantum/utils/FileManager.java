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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class FileManager 
{
	private final static String path;

	static
	{
		if( new File( "dat/" ).exists() )
		{			
			is_webstart = false;
			path = "";
		}
		else
		{						
			String user_dir = System.getProperty("user.home");
			path = user_dir + "/quantum/";
			Log.println( "[FileManager] path to quantum is '" + path + "'" );
			if( new File( user_dir + "/quantum/dat" ).exists() == false )
			{
				new File( user_dir + "/quantum" ).mkdir();
				new File( user_dir + "/quantum/dat" ).mkdir();
				new File( user_dir + "/quantum/dat/scripts" ).mkdir();
				new File( user_dir + "/quantum/dat/maps" ).mkdir();
				new File( user_dir + "/quantum/dat/recordings" ).mkdir();	
				loadMapsAndBots( );				
			}
			
			
			is_webstart = true;					
		}			
	}

	private static boolean is_webstart = false;

	public static boolean isWebstart( )
	{
		return is_webstart;
	}	

	private static void loadMapsAndBots( )
	{
		try
		{
			URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/maps/list.php" );
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout( 2000 );
			connection.connect();
			BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream(), "utf8") );

			String line = null;
			while( ( line = in.readLine() ) != null )	
			{
				if( line.startsWith( "<" ) || line.equals( "" ) || line.contains( ".map" ) == false )
					continue;

				line = line.replace( "</br>", "" );

				Log.println( "[FileManager] downloading map '" + line + "' for webstart installation" );
				download( "http://apiservers.com/hosted/marzec/quantum/maps/" + line, "dat/maps/" + line );				
			}

			in.close();
		}
		catch( Exception ex )
		{
			Log.println( "[FileManager] couldn't download maps: " + Log.getStackTrace( ex ) );			
		}

		try
		{
			URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/bots/list.php" );
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout( 2000 );
			connection.connect();
			BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream(), "utf8") );

			String line = null;
			while( ( line = in.readLine() ) != null )	
			{
				if( line.startsWith( "<" ) || line.equals( "" ) || line.contains( ".bsh" ) == false )
					continue;

				line = line.replace( "</br>", "" );

				Log.println( "[FileManager] downloading bot '" + line + "' for webstart installation" );
				download( "http://apiservers.com/hosted/marzec/quantum/bots/" + line, "dat/scripts/" + line );				
			}

			in.close();
		}
		catch( Exception ex )
		{
			Log.println( "[FileManager] couldn't download maps: " + Log.getStackTrace( ex ) );			
		}
	}
	
	public static String[] getMapList( ) throws Exception
	{
		try
		{
			URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/maps/list.php" );
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout( 2000 );
			connection.connect();
			BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream(), "utf8") );

			String line = null;
			ArrayList<String> maps = new ArrayList<String>( );
			while( ( line = in.readLine() ) != null )	
			{
				if( line.startsWith( "<" ) || line.equals( "" ) || line.contains( ".map" ) == false )
					continue;

				line = line.replace( "</br>", "" );
				
				maps.add( line );							
			}

			in.close();
			return maps.toArray( new String[0] );
		}
		catch( Exception ex )
		{
			Log.println( "[FileManager] couldn't download maps: " + Log.getStackTrace( ex ) );
			throw ex;
		}
	}

	public static void download( String uri, String local_file ) throws Exception
	{
		File file = FileManager.newFile( local_file );
		try
		{					
			BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( file ) );

			URL url = new URL( uri );
			URLConnection connection = url.openConnection(); 
			connection.setConnectTimeout( 2000 );
			connection.connect();
			InputStream in = connection.getInputStream();

			int read_size = 0;

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
			Log.println( "[FileManager] download from '" + uri + "' to '" + local_file + "' failed: " + Log.getStackTrace( ex ) );			
			file.delete();
		}
	}

	public static InputStream downloadMap( String file ) throws Exception
	{
		try
		{								
			URL url = new URL( "http://apiservers.com/hosted/marzec/quantum/maps/" + file );
			URLConnection connection = url.openConnection(); 
			connection.setConnectTimeout( 2000 );
			connection.connect();
			InputStream in = connection.getInputStream();

			return in;

		}
		catch( Exception ex )
		{					
			Log.println( "[FileManager] downloading map '" + file + "' failed: " + Log.getStackTrace( ex ) );
			throw ex;
		}
	}

	public static void uploadMap( String map, String remote_map ) throws Exception
	{
		try
		{
			String hostname = "www.apistudios.com";
			int port = 80;			
			Socket socket = new Socket();
			socket.connect( new InetSocketAddress( hostname, port ), 2000 );

			// Send header
			String path = "/hosted/marzec/quantum/maps/upload.php";

			// File To Upload
			File file = newFile( "dat/maps/" + map );

			DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream( file )));
			byte[] theData = new byte[(int) file.length( )];

			fis.readFully(theData);
			fis.close();

			DataOutputStream raw = new DataOutputStream(socket.getOutputStream());
			Writer wr = new OutputStreamWriter(raw);

			String command =
				"--dill\r\n"
				+ "Content-Disposition: form-data; name=\"uploadfile\"; filename=\""
				+ remote_map + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n"
				+ "\r\n";

			String trail = "\r\n--dill--\r\n";

			String header =
				"POST "+path+" HTTP/1.0\r\n"
				+ "Accept: */*\r\n"
				+ "Referer: http://localhost\r\n"
				+ "Accept-Language: de\r\n"
				+ "Content-Type: multipart/form-data; boundary=dill\r\n"
				+ "User_Agent: Quantum\r\n"
				+ "Host: localhost\r\n"
				+ "Content-Length: " + ((int) file.length() + command.length() +
						trail.length()) + "\r\n"
						+ "Connection: Keep-Alive\r\n"
						+ "Pragma: no-cache\r\n"
						+ "\r\n";

			wr.write(header);
			wr.write(command);

			wr.flush();
			raw.write(theData);
			raw.flush( );
			wr.write("\r\n--dill--\r\n");
			wr.flush( );

//			BufferedReader rd = new BufferedReader(new
//					InputStreamReader(socket.getInputStream()));
//			String line;
//			while ((line = rd.readLine()) != null) {
//				System.out.println(line);
//			}
			wr.close();
			raw.close();

			socket.close();
		} 
		catch (Exception e) 
		{ 
			Log.println( "[FileManager] couldn't upload file '" + map + "': " + Log.getStackTrace( e ) );
			throw e; 
		}

	}

	/**
	 * tries to open a file and returns it as a file input stream
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static InputStream readFile( String file ) throws Exception
	{			
		if( new File( path + file ).exists() )
			return new FileInputStream( file );
		else
		{			
			InputStream in = FileManager.class.getResourceAsStream( "/" + file );
			if( in != null )
				return in;
			in = ClassLoader.getSystemResourceAsStream( file );
			return in;
		}
	}

	public static InputStream readFile( File file ) throws Exception
	{					
		return new FileInputStream( file );		
	}

	/**
	 * tries to open a file for writting and returns it as an output stream
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static OutputStream writeFile( String file ) throws Exception
	{		
		return new FileOutputStream( path + file );							
	}

	/**
	 * returns a new file with the apropriate path
	 * @param file
	 * @return
	 */
	public static File newFile( String file ) 
	{
		Log.println( "[FileManager] path to quantum is '" + path + "', returning file '" + path + file +"'" );
		return new File( path + file );
	}	

	/**
	 * @returns wheter we are in webstart mode or node
	 */
	public static boolean isWebStart( )
	{
		return is_webstart;
	}

	public static void main( String[] argv ) throws Exception
	{
		//		FileManager.initialize();
//		FileManager.loadMapsAndBots();
		
	
	}

	/**
	 * returns the root path of quantum
	 * @return
	 */
	public static String getPath() 
	{	
		return path;
	}
}
