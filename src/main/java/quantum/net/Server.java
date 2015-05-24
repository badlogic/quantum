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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import quantum.game.Constants;
import quantum.game.Planet;
import quantum.game.Player;
import quantum.game.Simulation;
import quantum.game.commands.CommandBuffer;
import quantum.math.Vector2D;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.DisconnectedMessage;
import quantum.net.messages.GameOverMessage;
import quantum.net.messages.MapImageMessage;
import quantum.net.messages.MapListMessage;
import quantum.net.messages.Message;
import quantum.net.messages.PingMessage;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.PlayerMessage;
import quantum.net.messages.ReadyMessage;
import quantum.net.messages.SimulationMessage;
import quantum.net.messages.TextMessage;
import quantum.net.messages.VersionMessage;
import quantum.net.messages.VoteMessage;
import quantum.utils.FileManager;
import quantum.utils.Log;
import quantum.utils.Random;
import quantum.utils.Timer;

public strictfp class Server {
	static int id = 0;

	/** Handshake Thread. Waits for incoming Connections, negotiates the id and retrieves the name of the Client. If everything went
	 * ok the client is passed to the server.
	 * 
	 * @author marzec */
	strictfp class Login implements Runnable {
		ServerSocket socket;
		Thread thread;

		public Login (int port) throws Exception {
			socket = new ServerSocket(port);
			thread = new Thread(this);
			thread.start();
		}

		public void run () {
			Thread.currentThread().setName("Login Thread");

			while (true) {
				Socket s = null;

				try {
					s = socket.accept();
				} catch (SocketException e) {
					return;
				} catch (IOException e) {
					shutdown("listening socket error: " + Log.getStackTrace(e));
				}

				try {
					Client c = new Client(s);

					//
					// check versions
					//
					c.sendMessage(new VersionMessage());
					VersionMessage version_msg = null;
					while (version_msg == null)
						version_msg = (VersionMessage)c.readMessage();
					if (version_msg.getVersion() != Constants.VERSION)
						throw new RuntimeException("wrong version number " + version_msg.getVersion());

					//
					// get player message and give out id for that player
					//

					PlayerMessage player_msg = null;
					while (player_msg == null)
						player_msg = (PlayerMessage)c.readMessage();
					c.addPlayer(new Player(player_msg.getName(), id++));
					player_msg.setId(c.getPlayer().getId());
					c.sendMessage(player_msg);

					//
					// send a friendly text message
					//
					TextMessage msg = new TextMessage(-1, "Server", "Welcome " + c.getPlayer().getName() + "!");
					c.sendMessage(msg);

					//
					// send the server's map list
					//
					File dir = FileManager.newFile("dat/maps");
					String[] files = dir.list(new FilenameFilter() {

						public boolean accept (File dir, String name) {
							return name.endsWith(".map");
						}

					});

					ArrayList<String> filesList = new ArrayList<String>();
					ArrayList<String> nameList = new ArrayList<String>();

					int i = 0;
					for (String file : files) {
						Simulation sim = new Simulation(false);
						try {
							sim.load(FileManager.getPath() + "dat/maps/" + file);
							if (sim.getName().equals(""))
								nameList.add(file);
							else
								nameList.add(sim.getName() + " - " + sim.getAuthor());
							filesList.add(file);
						} catch (Throwable t) {
							Log.println("[Server] error while loading file '" + file + "'");
						}
					}

					MapListMessage map_msg = new MapListMessage(filesList.toArray(new String[0]), nameList.toArray(new String[0]));
					c.sendMessage(map_msg);

					//
					// add this client to the lobby
					//
					lobby.addClient(c);
				} catch (Exception e) {
					Log.println("[Server] client connection attempt failed: " + Log.getStackTrace(e));
				}
			}

		}

		public void stop () {
			try {
				socket.close();
			} catch (IOException e1) {
			}
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	/** the loby thread, checks for incoming messages and ready signals. kicks of the game thread in case all clients are ready.
	 * @author marzec */
	strictfp class Lobby implements Runnable {
		Thread thread;
		boolean running = true;
		Timer timer = new Timer();
		DatagramSocket broadcast_socket = null;

		public Lobby () {
			thread = new Thread(this);
			thread.start();
		}

		public void addClient (Client client) {
			synchronized (clients) {
				clients.add(client);
				broadcastPlayerList();
				Log.println("[Server] added client: " + client.getPlayer().getId() + ", " + client.getPlayer().getId());
			}
		}

		public void run () {
			Thread.currentThread().setName("Lobby Thread");

			ArrayList<Client> disconnected = new ArrayList<Client>();
			ArrayList<Client> local_clients = new ArrayList<Client>();
			try {
				broadcast_socket = new DatagramSocket();
			} catch (SocketException e1) {
				shutdown("couldn't create datagram socket: " + Log.getStackTrace(e1));
			}

			timer.start();

			File dir = FileManager.newFile("dat/maps");
			String[] files = dir.list(new FilenameFilter() {

				public boolean accept (File dir, String name) {
					return name.endsWith(".map");
				}

			});

			ArrayList<String> filesList = new ArrayList<String>();
			ArrayList<String> nameList = new ArrayList<String>();

			int i = 0;
			for (String file : files) {
				Simulation sim = new Simulation(false);
				try {
					sim.load(FileManager.getPath() + "dat/maps/" + file);
					if (sim.getName().equals(""))
						nameList.add(file);
					else
						nameList.add(sim.getName() + " - " + sim.getAuthor());
					filesList.add(file);
				} catch (Throwable t) {
					Log.println("[Server] error while loading file '" + file + "'");
				}
			}

			Log.println("[Server] finished loading files, sending list to client");
			MapListMessage map_msg = new MapListMessage(filesList.toArray(new String[0]), nameList.toArray(new String[0]));
			broadcastMessage(map_msg);

			votes.clear();

			publishServer();
			Timer ping_timer = new Timer();
			ping_timer.start();
			Timer udp_timer = new Timer();
			udp_timer.start();
			while (running) {
				boolean all_ready = true;

				synchronized (clients) {
					disconnected.clear();
					local_clients.clear();
					local_clients.addAll(clients);

					if (ping_timer.getElapsedSeconds() > 2) {
						broadcastMessage(new PingMessage());
						ping_timer.stop();
						ping_timer.start();
					}

					for (Client client : clients) {
						try {

							Message msg = client.readMessage();
							if (msg != null) {
								if (msg instanceof TextMessage) broadcastMessage(msg);
								if (msg instanceof ReadyMessage) {
									client.ready = true;
									broadcastMessage(msg);
									Log.println("[Server] client " + client.getPlayer().getId() + "(" + client.getPlayer().getId()
										+ ") is ready");
								}
								if (msg instanceof DisconnectedMessage) {
									disconnected.add(client);
								}
								if (msg instanceof MapImageMessage) {
									try {
										MapImageMessage image_msg = (MapImageMessage)msg;
										Simulation sim = new Simulation(false);
										DataInputStream in = new DataInputStream(new FileInputStream(FileManager.getPath() + "dat/maps/"
											+ image_msg.getName()));
										sim.readState(in);
										in.close();
										image_msg = new MapImageMessage(image_msg.getName(), sim);
										client.sendMessage(image_msg);
									} catch (Exception ex) {
										Log.println("[Server] couldn't send MapImageMessage: " + Log.getStackTrace(ex));
									}
								}
								if (msg instanceof VoteMessage) {
									broadcastMessage(msg);
									VoteMessage v_msg = (VoteMessage)msg;

									if (!votes.containsKey(v_msg.getName()))
										votes.put(v_msg.getName(), 1);
									else
										votes.put(v_msg.getName(), votes.get(v_msg.getName()) + 1);
								}
								if (msg instanceof MapListMessage) {
									MapListMessage map_msg2 = new MapListMessage(filesList.toArray(new String[0]),
										nameList.toArray(new String[0]));
									client.sendMessage(map_msg2);
								}
								if (msg instanceof PlayerListMessage) {
									sendPlayerList(client);
								}
								if (msg instanceof PlayerMessage) {
									PlayerMessage p_msg = (PlayerMessage)msg;
									if (p_msg.isRemove() == false) {
										client.addPlayer(new Player(p_msg.getName(), id++));
										p_msg.setId(id - 1);
										client.sendMessage(p_msg);
										broadcastPlayerList();
									} else {
										Iterator<Player> player = client.getPlayers().iterator();
										while (player.hasNext())
											if (player.next().getId() == p_msg.getId()) {
												player.remove();
												break;
											}
										broadcastPlayerList();
									}
								}
							}
						} catch (Exception e) {
							Log.println("[Server] disconnected client due to error: " + Log.getStackTrace(e));
							disconnected.add(client);
						}
						all_ready &= client.ready;
					}

					removeClients(disconnected);

					if (clients.size() >= 1 && all_ready) {
						login.stop();
						running = false;
						broadcast_socket.close();
						removeServer();
						game = new Game();
					}

					if (udp_timer.getElapsedSeconds() > 4) {
						try {
							InetAddress group = InetAddress.getByName("230.0.0.1");
							InetAddress addr = InetAddress.getLocalHost();
							String ip = (0x000000ff & ((int)addr.getAddress()[0])) + "." + (0x000000ff & ((int)addr.getAddress()[1]))
								+ "." + (0x000000ff & ((int)addr.getAddress()[2])) + "." + (0x000000ff & ((int)addr.getAddress()[3]));
							String identity = ip + ":" + port + ":" + name.replace(":", ":");

							DatagramPacket packet = new DatagramPacket(identity.getBytes(), identity.getBytes().length, group, 4446);
							broadcast_socket.send(packet);
						} catch (Exception e) {
							Log.println("[Server] couldn't broadcast server identity: " + Log.getStackTrace(e));
							shutdown("couldn't broadcast server identity!");
						}
						udp_timer.stop();
						udp_timer.start();

						updateServer();
					}
				}

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
				}
			}
		}

		@SuppressWarnings("deprecation")
		public void stop () {
			thread.stop();
		}
	}

	/** The game thread. handles turn command buffers and schedules them for broadcasting adequadly
	 * @author marzec */
	strictfp class Game implements Runnable {
		Thread thread;

		public Game () {
			Log.println("[Server] starting game");
			thread = new Thread(this);
			thread.setName("server game thread");
			thread.start();
		}

		public void run () {
			Thread.currentThread().setName("Server Game Thread");

			synchronized (clients) {
				//
				// start of by generating the game content
				// and sending the simulation to the clients
				//
				ReadyMessage ready_msg = new ReadyMessage(-1, "Server");
				broadcastMessage(ready_msg);

				Simulation sim = testing ? loadLevelTesting() : loadLevel();
				SimulationMessage sim_msg = new SimulationMessage(-1, sim);
				broadcastMessage(sim_msg);

				sim.resetRandom();

				HashMap<Client, Boolean> received = new HashMap<Client, Boolean>();
				HashMap<Client, Timer> last_received_client = new HashMap<Client, Timer>();
				ArrayList<Client> disconnected = new ArrayList<Client>();
				for (Client client : clients) {
					received.put(client, false);
					Timer t = new Timer();
					last_received_client.put(client, t);
					t.start();
				}
				CommandBuffer buffer = new CommandBuffer(Constants.COMMAND_TURNS * 2);

				int cmd_turn_inc = Constants.COMMAND_TURNS;

				Timer last_received = new Timer();
				last_received.start();

				while (true) {
					disconnected.clear();

					for (Client client : clients) {
						try {
							Message msg = client.readMessage();
							if (msg != null) {
								last_received.stop();
								last_received.start();
								last_received_client.get(client).stop();
								last_received_client.get(client).start();
								if (msg instanceof TextMessage) broadcastMessage(msg);

								if (msg instanceof DisconnectedMessage) {
									disconnected.add(client);
								}
								if (msg instanceof CommandBufferMessage) {
									CommandBufferMessage cmd_msg = (CommandBufferMessage)msg;
									if (cmd_msg.getCommandBuffer().getTurnId() != buffer.getTurnId()) {
										Log.println("[Server] received buffer for unexpected turn "
											+ cmd_msg.getCommandBuffer().getTurnId() + " from client " + client.getPlayer().getName());
										System.exit(0);
									}

									received.put(client, true);
									buffer.add(cmd_msg.getCommandBuffer());
								}

								if (msg instanceof PingMessage) {
									client.sendMessage(msg);

									if (((PingMessage)msg).getLastPing() != 0) {
										client.ping_mean.addValue(((PingMessage)msg).getLastPing());
									}
								}

								if (msg instanceof GameOverMessage) {
									GameOverMessage go_msg = new GameOverMessage();
									broadcastMessage(go_msg);

									for (Client c : clients)
										c.ready = false;

									try {
										login = new Login(port);
									} catch (Exception e) {
										Log.println("[Server] couldn't restart server: " + Log.getStackTrace(e));
										System.exit(0);
									}

									lobby = new Lobby();
									return;
								}

							} else {
								if (last_received_client.get(client).getElapsedSeconds() > 5) disconnected.add(client);
							}
						} catch (Exception e) {
							Log.println("[Server] removing client " + client.getPlayer().getName() + ": " + Log.getStackTrace(e));
							disconnected.add(client);
							received.remove(client);
							last_received_client.remove(client);
						}
					}
					removeClients(disconnected);

					boolean all_received = true;
					for (Client client : clients)
						all_received &= received.get(client);
					if (all_received) {
						double max_ping = 0;
						for (Client client : clients)
							max_ping = client.ping_mean.getMean() > max_ping ? client.ping_mean.getMean() : max_ping;

						int old_turn_inc = cmd_turn_inc;
						if (max_ping != 0) { //
							cmd_turn_inc = (int)max_ping / Constants.TURN_TIME + 5;
						}

						CommandBufferMessage cmd_msg = new CommandBufferMessage(-1, buffer);
						cmd_msg.setCommandTurns(cmd_turn_inc);
						broadcastMessage(cmd_msg);

						CommandBuffer tmp_buffer = new CommandBuffer(buffer.getTurnId());
						tmp_buffer.add(buffer);
						CommandBufferMessage tmp_msg = new CommandBufferMessage(-1, tmp_buffer);
						tmp_msg.setCommandTurns(cmd_msg.getNextCommandTurns());
						sim.enqueueTurnCommandBufferMessage(tmp_msg);

						for (Client client : clients)
							received.put(client, false);

						buffer.clear();
						buffer.setTurnId(buffer.getTurnId() + old_turn_inc);
					}

					try {
						Thread.sleep(3);
					} catch (InterruptedException e) {
					}

					if (clients.size() <= 0) {
						GameOverMessage msg = new GameOverMessage();
						broadcastMessage(msg);
						for (Client client : clients)
							client.ready = false;

						Log.println("[Server] restarting server");
						try {
							login = new Login(port);
						} catch (Exception e) {
							Log.println("[Server] couldn't restart server: " + Log.getStackTrace(e));
							System.exit(0);
						}
						lobby = new Lobby();
						return;
					}

					if (!testing && last_received.getElapsedSeconds() > 10) {
						GameOverMessage msg = new GameOverMessage();
						broadcastMessage(msg);
						for (Client client : clients)
							client.ready = false;

						Log.println("[Server] restarting server");
						try {
							login = new Login(port);
						} catch (Exception e) {
							Log.println("[Server] couldn't restart server: " + Log.getStackTrace(e));
							System.exit(0);
						}
						lobby = new Lobby();
						return;
					}
				}
			}
		}

		private Simulation loadLevel () {
			Simulation sim = new Simulation(false);

			if (votes.size() == 0) {
				File dir = FileManager.newFile("dat/maps/");
				String[] files = dir.list(new FilenameFilter() {

					public boolean accept (File dir, String name) {
						return name.endsWith(".map");
					}

				});

				Random rand = new Random(System.nanoTime());
				String map_file = files[(int)rand.rand(0, files.length - 1)];

				try {
					DataInputStream dataInputStream = new DataInputStream(new FileInputStream(FileManager.getPath() + "dat/maps/"
						+ map_file));
					sim.readState(dataInputStream);
					dataInputStream.close();
				} catch (Exception e) {
					Log.println("[Server] couldn't load '" + map_file + "': " + Log.getStackTrace(e));
					e.printStackTrace();
				}
			} else {
				int max = Integer.MIN_VALUE;
				String file = "";
				for (Entry<String, Integer> entry : votes.entrySet())
					if (entry.getValue() > max) {
						max = entry.getValue();
						file = entry.getKey();
					}

				try {
					sim.readState(new DataInputStream(new FileInputStream(FileManager.getPath() + "dat/maps/" + file)));
				} catch (Exception e) {
					Log.println("[Server] couldn't load '" + file + "': " + Log.getStackTrace(e));
					e.printStackTrace();
				}
			}

			ArrayList<Planet> planets = new ArrayList<Planet>();
			for (Planet planet : sim.getPlanets())
				planets.add(planet);

			ArrayList<Player> players = new ArrayList<Player>();
			for (Client client : clients)
				players.addAll(client.getPlayers());
			Collections.shuffle(players);

			for (int i = 0; i < players.size(); i++) {
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

				planet.setOwner(players.get(i).getId());
				planet.setResources(100);
				planet.spawnTree();

				for (int j = 0; j < 50; j++)
					planet.spawnCreature();
			}

			return sim;
		}

		private Simulation loadLevelTesting () {
// Simulation sim2 = loadLevel();
// if( true )
// return sim2;

			Simulation sim = new Simulation(false);

			Planet p1 = new Planet(sim, new Vector2D(0, 0), 200, 1f, 0.1f, 1f, 20);
			Planet p2 = new Planet(sim, new Vector2D(5000, 0), 200, 0.5f, 0.5f, 0.5f, 50);
			Planet p3 = new Planet(sim, new Vector2D(3000, 3000), 200, 0.5f, 0.5f, 0.5f, 30);
// Planet p4 = new Planet( sim, new Vector2D( -5000, 0 ), 200, 1f, 1f, 0.1f, 30 );
// Planet p5 = new Planet( sim, new Vector2D( -3000, 3000 ), 200, 0.5f, 0.5f, 0.5f, 30 );

// p5.setOwner( clients.get(0).getPlayers().get(0).getId() );
// p2.setOwner( clients.get(0).getPlayers().get(1).getId() );
// p4.setOwner( clients.get(0).getPlayers().get(2).getId() );

			sim.addObject(p1);
			sim.addObject(p2);
			sim.addObject(p3);
// sim.addObject( p4 );
// sim.addObject( p5 );

			p1.setOwner(clients.get(0).getPlayers().get(0).getId());
			for (int i = 0; i < 20; i++)
				p1.spawnCreature();

			p2.setOwner(clients.get(0).getPlayers().get(1).getId());
			for (int i = 0; i < 4; i++)
				p2.spawnCreature();

			p3.setOwner(clients.get(0).getPlayers().get(2).getId());
			for (int i = 0; i < 20; i++)
				p3.spawnCreature();

			sim.calculatePaths();

			return sim;
		}

		@SuppressWarnings("deprecation")
		public void stop () {
			thread.stop();
		}
	}

	Login login;
	Lobby lobby;
	Game game;
	int port;
	boolean testing;
	String last_map = "";
	ArrayList<Client> clients = new ArrayList<Client>();
	String name;
	String key = "";
	String external_ip = "";
	boolean log_enabled;
	HashMap<String, Integer> votes = new HashMap<String, Integer>();

	public Server (int port, String name, boolean testing, String external_ip) throws Exception {
		this.testing = testing;
		this.port = port;
		this.external_ip = external_ip;
		login = new Login(port);
		lobby = new Lobby();
		this.name = name;
		Log.println("[Server] quantum server started, port " + port);
	}

	public Server (int port, String name, String external_ip) throws Exception {
		this.port = port;
		this.external_ip = external_ip;
		login = new Login(port);
		lobby = new Lobby();
		this.name = name;
		Log.println("[Server] quantum server started, port " + port);
	}

	private void publishServer () {
// try {
// String cmd = "create&max_players=8&lobby_name=" + URLEncoder.encode( name, "UTF-8" ) + "&port=" + port;
// if( external_ip.equals( "" ) == false )
// cmd += "&ip=" + external_ip;
// URL url = new URL( "http://www.apistudios.com/quantum/list.php?" + cmd );
// URLConnection connection = url.openConnection();
// connection.setConnectTimeout( 2000 );
// connection.connect();
// BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ));
// key = in.readLine();
// in.close();
// } catch (Exception e) {
// Log.println( "[Server] couldn't load publish Server" );
// e.printStackTrace();
// }
	}

	private void updateServer () {
// try {
// String cmd = "update&id=" + URLEncoder.encode( key, "UTF-8" ) + "&players=" + clients.size();
// URL url = new URL( "http://www.apistudios.com/quantum/list.php?" + cmd );
// URLConnection connection = url.openConnection();
// connection.setConnectTimeout( 2000 );
// connection.connect();
// BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ));
// in.close();
// } catch (Exception e) {
// Log.println( "[Server] couldn't update Sever" );
// e.printStackTrace();
// }
	}

	private void removeServer () {
// try {
// String cmd = "remove&id=" + URLEncoder.encode( key, "UTF-8" );
// URL url = new URL( "http://www.apistudios.com/quantum/list.php?" + cmd );
// URLConnection connection = url.openConnection();
// connection.setConnectTimeout( 2000 );
// connection.connect();
// BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ));
// in.close();
// } catch (Exception e) {
// Log.println( "[Server] couldn't remove Server" );
// e.printStackTrace();
// }
	}

	public void broadcastPlayerList () {
		synchronized (clients) {
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ArrayList<String> names = new ArrayList<String>();

			for (Client client : clients) {
				for (Player player : client.getPlayers()) {
					ids.add(player.getId());
					names.add(player.getName());
				}
			}

			PlayerListMessage msg = new PlayerListMessage(ids, names);
			broadcastMessage(msg);
		}
	}

	public void sendPlayerList (Client c) {
		synchronized (clients) {
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ArrayList<String> names = new ArrayList<String>();

			for (Client client : clients) {
				for (Player player : client.getPlayers()) {
					ids.add(player.getId());
					names.add(player.getName());
				}
			}

			PlayerListMessage msg = new PlayerListMessage(ids, names);
			try {
				c.sendMessage(msg);
			} catch (Exception e) {
				Log.println("[Server] couldn't send PlayerListMessage to client " + c.getPlayer().getName() + ": "
					+ Log.getStackTrace(e));
				e.printStackTrace();
			}
		}
	}

	public void removeClients (List<Client> disconnected_clients) {
		synchronized (clients) {
			clients.removeAll(disconnected_clients);

			for (Client client : disconnected_clients) {
				Log.println("[Server] disconnected client: " + client.getPlayer().getName() + ", " + client.getPlayer().getId());
				client.dispose();
				for (Player player : client.getPlayers()) {
					DisconnectedMessage message = new DisconnectedMessage(player.getName());
					broadcastMessage(message);
				}
			}
		}
	}

	public void setLogging (boolean value) {
		log_enabled = value;
	}

	public void broadcastMessage (Message msg) {
		synchronized (clients) {
			ArrayList<Client> disconnected = new ArrayList<Client>();
			for (Client client : clients) {
				try {
					client.sendMessage(msg);
				} catch (Exception e) {
					disconnected.add(client);
					Log.println("[Server] disconnected client: " + client.getPlayer().getName() + ", " + client.getPlayer().getId());
				}
			}

			clients.removeAll(disconnected);

			while (disconnected.size() != 0) {
				ArrayList<Client> disconnected_new = new ArrayList<Client>();
				for (Client disconnected_client : disconnected) {
					for (Player player : disconnected_client.getPlayers()) {
						DisconnectedMessage message = new DisconnectedMessage(player.getName());
						for (Client client : clients) {
							if (client == disconnected_client) continue;

							try {
								client.sendMessage(message);
							} catch (Exception e) {
								disconnected_new.add(client);
								Log.println("[Server] disconnected player: " + client.getPlayer().getName() + ", "
									+ client.getPlayer().getId());
							}
						}
					}
				}
				disconnected = disconnected_new;
				clients.removeAll(disconnected);
			}
		}
	}

	public void shutdown (String message) {
		Log.println("[Server] SHUTTING DOWN: " + message);
		login.stop();
		lobby.stop();
		if (game != null) game.stop();
		removeServer();
	}

	public static void main (String argv[]) throws Exception {
		new Server(7776, "Test Server", true, "");
	}
}
