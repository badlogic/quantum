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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Bot;
import quantum.game.CommandTurnListener;
import quantum.game.GameLoop;
import quantum.game.Planet;
import quantum.game.Player;
import quantum.game.Simulation;
import quantum.gfx.Color;
import quantum.gui.Button;
import quantum.gui.CheckBox;
import quantum.gui.ClickedListener;
import quantum.gui.CustomDialog;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.Slider;
import quantum.gui.Spacer;
import quantum.gui.TextField;
import quantum.gui.ValueChangedListener;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.PlayerListMessage;
import quantum.sound.SoundManager;
import quantum.sound.SoundStream;
import quantum.utils.Log;

public class LocalGame implements KeyListener, DisplayListener,
		CommandTurnListener {
	Quantum quantum;
	Gui gui;
	GameLoop loop;
	List<Bot> bots;
	LocalGame self = this;
	ScreenAlignementContainer menu;
	CustomDialog game_menu;
	SoundStream music_stream;

	public LocalGame(final Quantum quantum, final Gui gui, Simulation sim,
			Client client, List<Bot> bots) {
		music_stream = SoundManager.playStream("sounds/bgsound.ogg");
		music_stream.setVolume(quantum.getConfig().getVolumeMusic());
		music_stream.setLooping(true);

		sim.addCommandTurnListener(this);
		this.quantum = quantum;
		this.gui = gui;
		this.bots = bots;
		gui.getCanvas().addKeyListener(this);
		quantum.addDisplayListener(this);

		List<Integer> ids = new ArrayList<Integer>();
		List<String> names = new ArrayList<String>();

		for (Player player : client.getPlayers()) {
			ids.add(player.getId());
			names.add(player.getName());
		}

		PlayerListMessage msg = new PlayerListMessage(ids, names);
		client.setPlayerList(msg);

		populateSimulation(sim, client.getPlayers());
		this.loop = new GameLoop(client, sim);
	}

	private void populateSimulation(Simulation sim, List<Player> players) {
		ArrayList<Planet> planets = new ArrayList<Planet>();
		for (Planet planet : sim.getPlanets())
			planets.add(planet);

		ArrayList<Player> pls = new ArrayList<Player>();
		pls.addAll(players);
		Collections.shuffle(pls);

		for (int i = 0; i < pls.size(); i++) {
			Planet planet = null;

			for (Planet p : planets) {
				if (p.isStartPlanet() && p.getOwner() < 0) {
					planet = p;
					break;
				}
			}

			if (planet == null) {
				for (Planet p : planets) {
					if (p.getOwner() < 0) {
						planet = p;
						break;
					}
				}
			}

			planet.setOwner(pls.get(i).getId());
			planet.setResources(100);
			planet.spawnTree();

			for (int j = 0; j < 50; j++)
				planet.spawnCreature();
		}
	}

	private void showGameOverMenu() {		

		Button save = new Button(gui, "Save Recording");
		Button back = new Button(gui, "Back");

		save.setSize(100, 25);
		back.setSize(75, 25);

		CustomDialog dialog = null;

		if (loop.getSimulation().getPlayerStats().get(
				loop.getClient().getPlayer().getId()) != null)
			dialog = new CustomDialog(
					gui,
					300,
					"Game Over",
					new Label(
							gui,
							"You have won! You can save the recording of the game for later playback.",
							300), save, back);
		else
			dialog = new CustomDialog(
					gui,
					300,
					"Game Over",
					new Label(
							gui,
							"You have lost! You can save the recording of the game for later playback.",
							300), save, back);

		final CustomDialog ref = dialog;

		save.setClickedListener(new ClickedListener() {

			public void clicked(Widget widget) {
				gui.remove(ref);
				showSaveRecordingDialog(false);
			}

		});

		back.setClickedListener(new ClickedListener() {

			public void clicked(Widget widget) {
				quantum.removeDisplayListener(self);
				gui.getCanvas().removeKeyListener(self);
				loop.dispose();
				if (menu != null)
					gui.remove(menu);
				gui.remove(ref);
				SoundManager.stopAll();
				new SinglePlayerMenu(quantum, gui);
				return;
			}

		});

		gui.add(dialog);
	}

	boolean save_menu_visible = false;

	private void showSaveRecordingDialog(final boolean in_game) {
		save_menu_visible = true;
		VerticalBoxContainer v_box = new VerticalBoxContainer(gui);

		Button save = new Button(gui, "Save");
		Button back = new Button(gui, "Cancel");

		save.setSize(75, 25);
		back.setSize(75, 25);

		v_box.addWidget(new Label(gui, "Enter Filename:"));
		v_box.addWidget(new Spacer(gui, 0, 10));

		final TextField file = new TextField(gui);
		file.setFocus(true);
		file.setText("game-"
				+ new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(Calendar
						.getInstance().getTime()) + ".rec");
		file.setSize(200, 25);

		v_box.addWidget(file);

		CustomDialog dialog = new CustomDialog(gui, 300, "Save Game Recording",
				v_box, save, back);
		final CustomDialog ref = dialog;

		save.setClickedListener(new ClickedListener() {

			public void clicked(Widget widget) {
				if (file.getText().equals("")) {
					gui.showConfirmDialog("Error",
							"You have to specify the filename!");
					return;
				}

				if (!in_game) {
					try {
						loop.saveRecording("dat/recordings/" + file.getText());
					} catch (Exception e) {
						Log
								.println("[LocalGame] couldn't save recording to '"
										+ file.getText()
										+ "': "
										+ Log.getStackTrace(e));
					}
					quantum.removeDisplayListener(self);
					gui.getCanvas().removeKeyListener(self);
					loop.dispose();
					if (menu != null)
						gui.remove(menu);
					gui.remove(ref);
					save_menu_visible = false;
					SoundManager.stopAll();
					new SinglePlayerMenu(quantum, gui);
				} else {
					try {
						loop.saveRecording("dat/recordings/" + file.getText());
					} catch (Exception e) {
						Log
								.println("[LocalGame] couldn't save recording to '"
										+ file.getText()
										+ "': "
										+ Log.getStackTrace(e));
					}
					gui.remove(ref);
					save_menu_visible = false;
					showMenu();
				}
				return;
			}

		});

		back.setClickedListener(new ClickedListener() {

			public void clicked(Widget widget) {
				if (!in_game) {
					quantum.removeDisplayListener(self);
					gui.getCanvas().removeKeyListener(self);
					loop.dispose();
					if (menu != null)
						gui.remove(menu);
					gui.remove(ref);
					save_menu_visible = false;
					SoundManager.stopAll();
					new SinglePlayerMenu(quantum, gui);
					return;
				} else {
					save_menu_visible = false;
					gui.remove(ref);
					showMenu();
				}
			}

		});

		gui.add(dialog);
	}

	boolean game_over_triggered = false;

	public void display(GLCanvas canvas) {
		Thread.currentThread().setName("Networked Game Thread");
		if (game_menu == null && save_menu_visible == false) {
			loop.update(canvas);
			for (Bot bot : bots)
				bot.update(loop.getSimulation());

			if (loop.getSimulation().getActivePlayers() == 1
					&& !game_over_triggered) {
				game_over_triggered = true;
				showGameOverMenu();
			}
		}
		loop.render(canvas);
		loop.getGameInterface().setHoverDelay(quantum.getDelay());

		// try {
		// Thread.sleep( 1 );
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	public void dispose() {

	}

	public void showMenu() {
		VerticalBoxContainer content = new VerticalBoxContainer(gui);

		Button leave = new Button(gui, "Quit Game");
		leave.setSize(200, 25);

		Button save = new Button(gui, "Save Recording");
		save.setSize(200, 25);

		content.addWidget(save, HorizontalAlignement.CENTER);
		content.addWidget(leave, HorizontalAlignement.CENTER);

		final CheckBox glow = new CheckBox(gui, "Glow Enabled");
		glow.setSize(200, 25);
		glow.setChecked(loop.getRenderer().isGlowOn());
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(glow, HorizontalAlignement.CENTER);
		glow.setClickedListener(new ClickedListener() {

			public void clicked(Widget widget) {
				loop.getRenderer().useGlow(glow.isChecked());
			}

		});

		final Label label = new Label(gui, "Popup Delay ["
				+ loop.getGameInterface().getHoverDelay() * 1000 + " ms]");
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(label, HorizontalAlignement.CENTER);

		final Slider delay = new Slider(gui, 0, 0.25f, loop.getGameInterface()
				.getHoverDelay());
		delay.setSize(100, 5);
		delay.setBackgroundColor(new Color(0.3f, 0.3f, 0.3f, 1));
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(delay, HorizontalAlignement.CENTER);
		delay.setValueChangedListener(new ValueChangedListener() {

			public void valueChanged(Widget widget) {
				loop.getGameInterface().setHoverDelay(delay.getValue());
				label.setText("Popup Delay ["
						+ loop.getGameInterface().getHoverDelay() * 1000
						+ " ms]");
				quantum.setDelay(delay.getValue());
			}
		});

		final Label label_music = new Label(gui, "Music Volume");
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(label_music, HorizontalAlignement.CENTER);

		final Slider music = new Slider(gui, 0, 1, quantum.getConfig()
				.getVolumeMusic());
		music.setSize(100, 5);
		music.setBackgroundColor(new Color(0.3f, 0.3f, 0.3f, 1));
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(music, HorizontalAlignement.CENTER);
		music.setValueChangedListener(new ValueChangedListener() {

			public void valueChanged(Widget widget) {
				quantum.getConfig().setVolumeMusic(music.getValue());
				music_stream.setVolume(music.getValue());
			}
		});

		final Label label_effect = new Label(gui, "Effects Volume");
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(label_effect, HorizontalAlignement.CENTER);

		final Slider effect = new Slider(gui, 0, 1, quantum.getConfig()
				.getVolumeSfx());
		effect.setSize(100, 5);
		effect.setBackgroundColor(new Color(0.3f, 0.3f, 0.3f, 1));
		content.addWidget(new Spacer(gui, 0, 10));
		content.addWidget(effect, HorizontalAlignement.CENTER);
		effect.setValueChangedListener(new ValueChangedListener() {

			public void valueChanged(Widget widget) {
				quantum.getConfig().setVolumeSfx(effect.getValue());
				SoundManager.setBufferVolume(effect.getValue());
			}
		});

		game_menu = new CustomDialog(gui, 210, "Game Menu", content);
		gui.add(game_menu);

		leave.setClickedListener(new ClickedListener() {
			public void clicked(Widget widget) {
				quantum.removeDisplayListener(self);
				gui.getCanvas().removeKeyListener(self);
				loop.dispose();
				hideMenu();
				SoundManager.stopAll();
				new SinglePlayerMenu(quantum, gui);
				return;
			}
		});

		save.setClickedListener(new ClickedListener() {
			public void clicked(Widget widget) {
				hideMenu();
				showSaveRecordingDialog(true);
			}
		});

	}

	public void hideMenu() {
		gui.remove(game_menu);
		game_menu = null;
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !save_menu_visible) {
			if (game_menu == null)
				showMenu();
			else
				hideMenu();

			// if( menu == null )
			// {
			// menu = new ScreenAlignementContainer( gui,
			// HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
			//								
			// Button leave_game = new Button( gui, "Quit Game" );
			// final CheckBox glow = new CheckBox( gui, "Enable Glow" );
			//				
			// leave_game.setSize( 200, 25 );
			// glow.setSize( 20, 20 );
			// glow.setChecked( loop.getRenderer().isGlowOn() );
			//												
			// leave_game.setBackgroundColor( 0, 0, 0, 1 );
			// glow.setBackgroundColor( 0, 0, 0, 1 );
			//				
			// glow.setClickedListener( new ClickedListener( ) {
			//					
			// public void clicked(Widget widget)
			// {
			// loop.getRenderer().useGlow( glow.isChecked() );
			// }
			//					
			// });
			//				
			// leave_game.setClickedListener( new ClickedListener( ) {
			//
			// public void clicked(Widget widget)
			// {
			// quantum.removeDisplayListener( self );
			// gui.getCanvas().removeKeyListener( self );
			// loop.dispose();
			// gui.remove( menu );
			// new SinglePlayerMenu(quantum, gui);
			// return;
			// }
			//					
			// });
			//								
			// menu.addWidget( leave_game );
			// menu.addWidget( new Spacer( gui, 0, 5 ) );
			// menu.addWidget( glow, HorizontalAlignement.CENTER );
			// menu.addWidget( new Spacer( gui, 0, 5 ) );
			//				
			// gui.add( menu );
			// }
			// else
			// {
			// gui.remove( menu );
			// menu = null;
			// }
		}
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	public void commandTurn(CommandBufferMessage msg) {
		// TODO Auto-generated method stub

	}
}
