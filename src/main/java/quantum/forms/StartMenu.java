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

package quantum.forms;

import java.io.DataInputStream;
import java.io.EOFException;
import java.util.zip.GZIPInputStream;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Constants;
import quantum.game.Simulation;
import quantum.gfx.FrameBufferObject;
import quantum.gfx.Renderer;
import quantum.gfx.Texture;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Spacer;
import quantum.gui.VerticalAlignement;
import quantum.gui.Widget;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.MessageDecoder;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.SimulationMessage;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class StartMenu implements DisplayListener {
	Quantum quantum;
	StartMenu self = this;
	Renderer renderer;
	Simulation sim;

	public StartMenu (final Quantum quantum, final Gui gui) {

		this.quantum = quantum;
		quantum.addDisplayListener(this);
		gui.getCanvas().getContext().makeCurrent();

		renderer = new Renderer();
		load("bggame.rec");

		final ScreenAlignementContainer cont = new ScreenAlignementContainer(gui, HorizontalAlignement.CENTER,
			VerticalAlignement.TOP);
		Image image;
		try {
			image = new Image(gui, FileManager.readFile("quantum.png"));
			cont.addWidget(new Spacer(gui, 0, 50));
			cont.addWidget(image, HorizontalAlignement.CENTER);
		} catch (Exception e) {
			Log.println("[StartMenu] couldn't load image 'quantum.png'");
			gui.showConfirmDialog("Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error");
		}

		final ScreenAlignementContainer cont3 = new ScreenAlignementContainer(gui, HorizontalAlignement.RIGHT,
			VerticalAlignement.BOTTOM);
		Label version = new Label(gui, "Version " + Constants.VERSION_STRING);
		cont3.addWidget(version);
		gui.add(cont3);

		Button single_game = new Button(gui, "Singleplayer");
		Button multi_game = new Button(gui, "Multiplayer");
		Button editor = new Button(gui, "Editor");
		Button exit = new Button(gui, "Exit");
		Button replay = new Button(gui, "Replay");
		Button tutorial = new Button(gui, "Tutorial");
		Button maps = new Button(gui, "Manage Maps");
		Button update = new Button(gui, "Check for Updates");
		Button garbage_collect = new Button(gui, "Collect Garbage");

		single_game.setSize(200, 40);
		single_game.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		multi_game.setSize(200, 40);
		multi_game.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		editor.setSize(200, 40);
		editor.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		exit.setSize(200, 40);
		exit.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		replay.setSize(200, 40);
		replay.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		tutorial.setSize(200, 40);
		tutorial.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		maps.setSize(200, 40);
		maps.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		update.setSize(200, 40);
		update.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);
		garbage_collect.setSize(200, 40);
		garbage_collect.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.7f);

		update.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new UpdateMenu(quantum, gui);
			}

		});

		tutorial.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new Tutorial(quantum, gui);
			}

		});

		editor.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new Editor(quantum, gui);
			}
		});

		exit.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				quantum.close();
			}
		});

		single_game.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new SinglePlayerMenu(quantum, gui);
			}

		});

		multi_game.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new LoginMenu(quantum, gui);
			}

		});

		replay.setClickedListener(new ClickedListener() {
			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new Replay(quantum, gui);
			}
		});

		maps.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				renderer.dispose();
				new MapMenu(quantum, gui);
			}

		});

		garbage_collect.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				for (int i = 0; i < 4; i++) {
					Runtime.getRuntime().gc();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		cont.addWidget(new Spacer(gui, 0, 80));
		cont.addWidget(single_game, HorizontalAlignement.CENTER);
		cont.addWidget(multi_game, HorizontalAlignement.CENTER);
		cont.addWidget(tutorial, HorizontalAlignement.CENTER);
		cont.addWidget(replay, HorizontalAlignement.CENTER);
		cont.addWidget(editor, HorizontalAlignement.CENTER);
		cont.addWidget(maps, HorizontalAlignement.CENTER);
		cont.addWidget(update, HorizontalAlignement.CENTER);
		cont.addWidget(exit, HorizontalAlignement.CENTER);
// cont.addWidget( garbage_collect, HorizontalAlignement.CENTER );

		gui.add(cont);

		Log.println("[StartMenu] Texture count: " + Texture.getTextureCount());
		Log.println("[StartMenu] FBO count: " + FrameBufferObject.getFBOCount());
	}

	public void display (GLCanvas canvas) {
		try {
			if (sim != null) {
				renderer.getCamera().setInputDisabled(true);
				renderer.centerCamera(canvas, sim);
				sim.update();
				renderer.render(sim, canvas);
			}

			Thread.sleep(3);
		} catch (Exception e) {
		}
	}

	public void load (String file) {
		try {
			DataInputStream in = new DataInputStream(new GZIPInputStream(FileManager.readFile(file)));
			PlayerListMessage player_msg = (PlayerListMessage)MessageDecoder.decode(in);
			SimulationMessage sim_msg = (SimulationMessage)MessageDecoder.decode(in);
			Client client = new Client("Replay");
			client.setPlayerList(player_msg);
			sim = sim_msg.getSimulation();

			try {
				while (in.available() > 0) {
					CommandBufferMessage msg = (CommandBufferMessage)MessageDecoder.decode(in);
					sim.enqueueTurnCommandBufferMessage(msg);
				}
			} catch (EOFException ex) {

			}
			in.close();
		} catch (Exception ex) {

		}
	}
}
