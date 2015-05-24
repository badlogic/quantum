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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.media.opengl.awt.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.ConfirmDialog;
import quantum.gui.EnterListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.SelectedListener;
import quantum.gui.Spacer;
import quantum.gui.TextArea;
import quantum.gui.TextField;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.net.IRC;
import quantum.net.ServerDiscovery;
import quantum.net.ServerDiscovery.ServerEntry;
import quantum.utils.FileManager;
import quantum.utils.Log;
import quantum.utils.Timer;

public class JoinMenu implements DisplayListener {
	Quantum quantum;
	Timer timer = new Timer();
	JoinMenu self = this;
	quantum.gui.List lan_list = null;
	quantum.gui.List net_list = null;
	ServerDiscovery discovery;

	IRC irc;
	TextArea text_area;
	quantum.gui.List players;
	String channel = "#quantum";
	Button quant;
	Button idle;

	public JoinMenu (final Quantum quantum, final Gui gui) {
		try {
			discovery = new ServerDiscovery();
		} catch (Exception e1) {
			Log.println("[Quantum] Automatic Server discovery not working: " + Log.getStackTrace(e1));
			gui.showConfirmDialog("Automatic Server discovery not working", "Warning");
		}

// try
// {
// irc = new IRC( "apistudios.com", 6667, quantum.getLastName() );
// quantum.setLastName( irc.getNickName() );
// }
// catch( Exception ex )
// {
// Log.println( "[JoinMenu] couldn't connect to irc server: " + Log.getStackTrace( ex ) );
// }

		this.quantum = quantum;
		final ScreenAlignementContainer cont = new ScreenAlignementContainer(gui, HorizontalAlignement.CENTER,
			VerticalAlignement.CENTER);
		Image image;
		try {
			image = new Image(gui, FileManager.readFile("quantum.png"));
			cont.addWidget(image, HorizontalAlignement.CENTER);
			cont.addWidget(new Spacer(gui, 0, 10));
		} catch (Exception e) {
			Log.println("Couldn't load image 'quantum.png'");
			gui.showConfirmDialog("Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error");
		}

		final ScreenAlignementContainer cont3 = new ScreenAlignementContainer(gui, HorizontalAlignement.RIGHT,
			VerticalAlignement.BOTTOM);
		Button back = new Button(gui, "Back");
		back.setSize(65, 25);
		back.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				if (discovery != null) discovery.dispose();
				if (irc != null) irc.dispose();
				new StartMenu(quantum, gui);
			}
		});

		try {
			Image img = new Image(gui, FileManager.readFile("apistudios.png")) {
				public void mousePressed (float x, float y, int button) {
					try {
						java.awt.Desktop.getDesktop().browse(new URI("http://www.apistudios.com"));
					} catch (IOException e) {
					} catch (URISyntaxException e) {
					}
				}
			};
			cont3.addWidget(new Label(gui, "hosted by"));
			cont3.addWidget(img, HorizontalAlignement.LEFT);
			cont3.addWidget(new Spacer(gui, 100, 10));
		} catch (Exception e) {
			Log.println("[JoinMenu] couldn't load 'apistudios.png'");
			e.printStackTrace();
		}

		final TextField ip = new TextField(gui);
		final TextField port = new TextField(gui);
		port.setText("7777");

		ip.setSize(200, 25);
		ip.setText(quantum.getLastIp());
		port.setSize(200, 25);

		Button join = new Button(gui, "Join");
		join.setSize(64, 25);
		join.setClickedListener(new ClickedListener() {

			public void clicked (Widget widget) {
				if (ip.getText().length() == 0) {
					gui.showConfirmDialog("Please enter an ip", "Error");
					return;
				}

				if (port.getText().length() == 0) {
					gui.showConfirmDialog("Please enter a port number", "Error");
					return;
				}

				int port_number = 0;
				try {
					port_number = Integer.parseInt(port.getText());
				} catch (Exception ex) {
					gui.showConfirmDialog("Please enter a valid port number", "Error");
					return;
				}

				try {
					quantum.createClient(quantum.getLastName(), ip.getText(), port_number);
				} catch (Exception ex) {
					Log.println("[JoinMenu] couldn't connect to server at " + ip.getText() + ":" + port_number + " :"
						+ ex.getMessage());
					quantum.closeServerAndClient();
					gui.showConfirmDialog("Couldn't connect server at " + ip.getText() + ":" + port_number + "!\n" + ex.getMessage(),
						"Error");
					return;
				}

				quantum.setLastIp(ip.getText());
				quantum.setLastPort(port.getText());
				gui.remove(cont);

				gui.remove(cont3);
				quantum.removeDisplayListener(self);
				if (discovery != null) discovery.dispose();
				if (irc != null) irc.dispose();
				new LobbyMenu(quantum, gui, ip.getText(), false);
			}

		});

		Button create = new Button(gui, "Create");
		create.setSize(65, 25);
		create.setClickedListener(new ClickedListener() {
			public void clicked (Widget widget) {
				gui.remove(cont);
				gui.remove(cont3);
				if (discovery != null) discovery.dispose();
				if (irc != null) irc.dispose();
				quantum.removeDisplayListener(self);
				new CreateMenu(quantum, gui);
			}
		});

		HorizontalBoxContainer h_box = new HorizontalBoxContainer(gui);
		VerticalBoxContainer bv_box = new VerticalBoxContainer(gui);

		if (irc != null) {
			quant = new Button(gui, "#quantum");
			idle = new Button(gui, "#apistudios");
			quant.setSize(80, 25);
			quant.setForegroundColor(1, 0, 0, 1);
			idle.setSize(80, 25);

			text_area = new TextArea(gui);
			text_area = new TextArea(gui);
			text_area.setSize(500, 300);
			text_area.setText("Connection to irc.apistudios.com ... please wait!");

			final TextField text_field = new TextField(gui);
			text_field.setSize(645, 25);
			text_field.setEnterListener(new EnterListener() {

				public void pressedEnter (Widget widget) {
					if (text_field.getText().length() > 0) {
						try {
							irc.sendText(channel, quantum.getLastName(), text_field.getText());
							text_area.setText(text_area.getText() + "\n" + quantum.getLastName() + ": " + text_field.getText());
						} catch (Exception e) {
							Log.println("[LobbyMenu] couldn't send TextMessage: " + e.getMessage());
							ConfirmDialog dialog = new ConfirmDialog(gui, "Disconnected!", "Error", new ClickedListener() {

								public void clicked (Widget widget) {
									gui.remove(cont);

									gui.remove(cont3);
									if (discovery != null) discovery.dispose();
									if (irc != null) irc.dispose();
									quantum.removeDisplayListener(self);
									new CreateMenu(quantum, gui);
									gui.remove(widget);
								}
							});

							gui.add(dialog);
						}
					}

					text_field.setText("");
				}

			});

			HorizontalBoxContainer irc_h_box = new HorizontalBoxContainer(gui);
			irc_h_box.addWidget(quant);
			irc_h_box.addWidget(idle);
			cont.addWidget(irc_h_box);

			irc_h_box = new HorizontalBoxContainer(gui);
			irc_h_box.addWidget(text_area);
			players = new quantum.gui.List(gui);
			players.setSize(145, 300);
			irc_h_box.addWidget(players);

			cont.addWidget(irc_h_box);
			cont.addWidget(text_field);
			cont.addWidget(new Spacer(gui, 5, 10));

			quant.setClickedListener(new ClickedListener() {
				public void clicked (Widget widget) {
					quant.setForegroundColor(1, 0, 0, 1);
					idle.setForegroundColor(1, 1, 1, 1);
					channel = "#quantum";
					text_area.setText("");
					for (String message : irc.getMessages(channel))
						text_area.setText(text_area.getText() + "\n" + message);
					irc.getNewMessages(channel);

					players.removeAll();
					List<String> users = irc.getUsers(channel);
					if (users.size() != 0) {
						players.removeAll();
						for (String user : users)
							players.addItem(user);
					}
				}
			});

			idle.setClickedListener(new ClickedListener() {
				public void clicked (Widget widget) {
					channel = "#idle";
					quant.setForegroundColor(1, 1, 1, 1);
					idle.setForegroundColor(1, 0, 0, 1);
					text_area.setText("");
					for (String message : irc.getMessages(channel))
						text_area.setText(text_area.getText() + "\n" + message);
					irc.getNewMessages(channel);

					players.removeAll();
					List<String> users = irc.getUsers(channel);
					if (users.size() != 0) {
						players.removeAll();
						for (String user : users)
							players.addItem(user);
					}
				}
			});
		}

		bv_box.addWidget(new Label(gui, "IP Address"));
		bv_box.addWidget(new Spacer(gui, 5, 5));
		bv_box.addWidget(ip);
		bv_box.addWidget(new Spacer(gui, 5, 5));
		bv_box.addWidget(new Label(gui, "Port"));
		bv_box.addWidget(new Spacer(gui, 5, 5));
		bv_box.addWidget(port);

		bv_box.addWidget(new Spacer(gui, 0, 10));

		HorizontalBoxContainer bh_box = new HorizontalBoxContainer(gui);
		bh_box.addWidget(create);
		bh_box.addWidget(new Spacer(gui, 5, 5));
		bh_box.addWidget(join);
		bh_box.addWidget(new Spacer(gui, 5, 5));
		bh_box.addWidget(back);
		bv_box.addWidget(bh_box, HorizontalAlignement.CENTER);

		lan_list = new quantum.gui.List(gui);
		lan_list.setBackgroundColor(0, 0, 0, 1);
		lan_list.setSize(200, 150);
		lan_list.setSelectedListener(new SelectedListener() {
			public void selected (Widget widget, Object selection) {
				if (selection != null) {
					ServerEntry entry = (ServerEntry)selection;
					ip.setText(entry.ip);
					port.setText("" + entry.port);
					net_list.unselect();
				}

			}

		});

		net_list = new quantum.gui.List(gui);
		net_list.setBackgroundColor(0, 0, 0, 1);
		net_list.setSize(200, 150);
		net_list.setSelectedListener(new SelectedListener() {
			public void selected (Widget widget, Object selection) {
				if (selection != null) {
					ServerEntry entry = (ServerEntry)selection;
					ip.setText(entry.ip);
					port.setText("" + entry.port);
					lan_list.unselect();
				}

			}

		});

		VerticalBoxContainer lh_box = new VerticalBoxContainer(gui);
		lh_box.addWidget(new Label(gui, "Lan Servers"));
		lh_box.addWidget(new Spacer(gui, 0, 5));
		lh_box.addWidget(lan_list);

		VerticalBoxContainer nh_box = new VerticalBoxContainer(gui);
		nh_box.addWidget(new Label(gui, "INet Servers"));
		nh_box.addWidget(new Spacer(gui, 0, 5));
		nh_box.addWidget(net_list);

		h_box.addWidget(bv_box, VerticalAlignement.CENTER);
		h_box.addWidget(new Spacer(gui, 20, 20));
		h_box.addWidget(lh_box);
		h_box.addWidget(new Spacer(gui, 20, 20));
		h_box.addWidget(nh_box);

		cont.addWidget(h_box);
		gui.add(cont);
		quantum.addDisplayListener(this);

		gui.add(cont3);

		timer.start();
	}

	public void display (GLCanvas canvas) {
		if (timer.getElapsedSeconds() > 3) {
			try {
				if (discovery != null) {
					List<ServerEntry> servers = discovery.getServers();
					lan_list.removeAll();
					net_list.removeAll();
					for (ServerEntry server : servers)
						if (server.isLan())
							lan_list.addItem(server);
						else
							net_list.addItem(server);
					timer.stop();
					timer.start();
				}

				if (irc != null) {
					if (irc.hasNewMessages("#idle")) if (!channel.equals("#idle")) idle.setForegroundColor(0, 1, 0, 1);

					if (irc.hasNewMessages("#quantum")) if (!channel.equals("#quantum")) quant.setForegroundColor(0, 1, 0, 1);

					List<String> users = irc.getUsers(channel);
					if (users.size() != 0) {
						players.removeAll();
						for (String user : users)
							players.addItem(user);
					}

					List<String> messages = irc.getNewMessages(channel);
					if (messages.size() != 0) {
						for (String message : messages)
							text_area.setText(text_area.getText() + "\n" + message);
					}
				}
			} catch (Exception ex) {
				Log.println("[JoinMenu] couldn't retrieve servers: " + ex.getMessage());
				ex.printStackTrace();
			}
		}

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}
	}
}
