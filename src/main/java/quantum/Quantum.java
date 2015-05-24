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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import quantum.forms.StartMenu;
import quantum.gui.Gui;
import quantum.net.Client;
import quantum.net.Server;
import quantum.sound.SoundManager;
import quantum.utils.FileManager;
import quantum.utils.Log;

import com.jogamp.opengl.util.Animator;

@SuppressWarnings("serial")
public strictfp class Quantum extends JFrame implements GLEventListener {
	public interface DisplayListener {
		public void display (GLCanvas canvas);
	}

	Gui gui;
	StartMenu menu;
	Server server;
	Client client;
	String user_name;
	ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
	GLCanvas canvas;
	Animator animator;
	Config config = new Config();

	public Quantum () {		
		GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
		caps.setRedBits(8);
		caps.setGreenBits(8);
		caps.setBlueBits(8);
		caps.setAlphaBits(8);
		caps.setDepthBits(16);
		caps.setStencilBits(8);
		caps.setDoubleBuffered(true);
		canvas = new GLCanvas(caps);
		canvas.addGLEventListener(this);

		SoundManager.setBufferVolume(config.getVolumeSfx());
		setBounds(config.getX(), config.getY(), config.getWidth(), config.getHeight());
		setTitle("QUANTUM");
		try {
			this.setIconImage(ImageIO.read(FileManager.readFile("icon.png")));
		} catch (Exception e1) {
			Log.println("[Quantum] couldn't 'load icon.png'");
		}

		add(canvas);

		animator = new Animator(canvas);
		animator.setRunAsFastAsPossible(true);
		animator.start();

		addWindowListener(new WindowAdapter() {
			public void windowClosing (WindowEvent e) {
				remove(canvas);
				animator.stop();
				closing();
				System.exit(0);
			}
		});
	}

	public void close () {
		remove(canvas);
		animator.stop();
		closing();
		System.exit(0);
	}

	public static void main (String[] argv) throws Exception {
// String[] files = FileManager.newFile( "./" ).list();
// for( String file: files )
// {
// if( file.contains( ".zip" ) && file.startsWith( "tmp" ) )
// {
// try {
// AutoUpdater.unzip( FileManager.newFile( file ) );
// } catch (Exception e) {
// Log.println( "[Quantum] unzipping update failed!" + Log.getStackTrace( e ));
// }
// FileManager.newFile( file ).delete();
// if( FileManager.newFile( "jre" ).exists() )
// new ProcessBuilder( "jre/bin/javaw", "-server", "-jar", "quantum.jar" ).start();
// else
// new ProcessBuilder( "javaw", "-jar", "quantum.jar" ).start();
// System.exit(0);
// }
// }		

		SwingUtilities.invokeLater(new Runnable() {
			public void run () {
				Quantum app = new Quantum();
				app.setVisible(true);
			}
		});
	}

	public void addDisplayListener (DisplayListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeDisplayListener (DisplayListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void setServer (Server server) {
		this.server = server;
	}

	public Server getServer () {
		return server;
	}

	public void setClient (Client client) {
		this.client = client;
	}

	public Client getClient () {
		return client;
	}

	public void closing () {
		FileManager.newFile("./").list(new FilenameFilter() {
			public boolean accept (File dir, String name) {
				if (name.startsWith("tmp") && name.endsWith(".rec")) FileManager.newFile(name).delete();
				return false;
			}
		});

		closeServerAndClient();
		config.write(this.getBounds());
	}

	public String getLastName () {
		return config.getName();
	}

	public String getLastPort () {
		return config.getPort();
	}

	public String getLastIp () {
		return config.getIp();
	}

	public void setLastName (String name) {
		config.setName(name);
	}

	public void setLastPort (String port) {
		config.setPort(port);
	}

	public void setLastIp (String ip) {
		config.setIp(ip);
	}

	public void setDelay (float delay) {
		config.setDelay(delay);
	}

	public Config getConfig () {
		return config;
	}

	public void createServer (int port_number, String name, String ip) throws Exception {
		if (server != null) throw new RuntimeException("oh no's! Server already running!");

		server = new Server(port_number, name, ip);
	}

	public void createClient (String user_name, String ip, int port) throws Exception {
		client = new Client(user_name, ip, port);
	}

	public void closeServerAndClient () {
		if (client != null) client.dispose();

		if (server != null) server.shutdown("");

		server = null;
		client = null;
	}

	public void display (GLAutoDrawable drawable) {
		synchronized (drawable) {
			drawable.getGL().glViewport(0,  0, drawable.getWidth(), drawable.getHeight());
			drawable.getGL().glClearColor(1, 0, 0, 1);
			drawable.getGL().glClear(GL.GL_COLOR_BUFFER_BIT);

			synchronized (listeners) {
				for (DisplayListener listener : listeners)
					listener.display((GLCanvas)drawable);
			}

			gui.render();
		}
	}

	public void displayChanged (GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// TODO Auto-generated method stub

	}

	public void init (GLAutoDrawable drawable) {
		synchronized (drawable) {
			if(gui == null) {
				gui = new Gui((GLCanvas)drawable);
				// gui.setDefaultFont( "dat/matchworks.ttf", 16, FontStyle.Plain );
				menu = new StartMenu(this, gui);
				drawable.getGL().setSwapInterval(-1);
				drawable.getGL().glEnable(GL.GL_MULTISAMPLE);
			}
		}
	}

	public void reshape (GLAutoDrawable drawable, int x, int y, int width, int height) {

	}

	public float getDelay () {
		return config.getDelay();
	}

	public void dispose (GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
}
