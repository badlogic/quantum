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

package quantum.game;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLCanvas;

import quantum.gfx.Color;
import quantum.gfx.Font;
import quantum.gfx.Font.FontStyle;
import quantum.gfx.OrthoCamera;
import quantum.gfx.Renderer;
import quantum.gfx.Texture;
import quantum.gui.Widget;
import quantum.math.Matrix;
import quantum.math.Vector2D;
import quantum.net.messages.PlayerListMessage;
import quantum.sound.SoundManager;
import quantum.utils.FileManager;
import quantum.utils.Log;
import quantum.utils.Timer;

public strictfp class GameInterface implements MouseListener, MouseMotionListener, KeyListener {
	Simulation sim;
	OrthoCamera cam;
	Matrix mat = new Matrix();
	GameLoop loop;
	Font font;
	Planet selected_planet;
	Renderer renderer;
	GLCanvas canvas;

	ArrayList<Handle> move_handles = new ArrayList<Handle>();
	static Handle selected_handle = null;
	Button tree_button = null;
	Texture mark = null;
	boolean show_stats = false;
	boolean listener_installed = false;
	boolean ctrl_pressed = false;
	boolean is_replay = false;

	Timer hover_timer = new Timer();
	Planet last_hovered = null;
	float delay = 0.20f;

	class Button {
		float x, y;
		Texture tree_button_texture;
		boolean mouse_over = false;

		public void render (float x, float y) {
			this.x = x;
			this.y = y;
			if (tree_button_texture == null) try {
				tree_button_texture = Texture.loadTexture(FileManager.readFile("treebutton.png"));
			} catch (Exception e) {
				Log.println("[GameInterface] couldn't load 'treebutton.png'");
			}

			tree_button_texture.bind(0);
			GL2 gl = GLContext.getCurrent().getGL().getGL2();
			if (mouse_over)
				gl.glColor4f(1, 1, 1, 0);
			else
				gl.glColor4f(0.7f, 0.7f, 0.7f, 0);
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0, 0);
			gl.glVertex2f(x, y);
			gl.glTexCoord2f(1, 0);
			gl.glVertex2f(x + 32, y);
			gl.glTexCoord2f(1, 1);
			gl.glVertex2f(x + 32, y - 32);
			gl.glTexCoord2f(0, 1);
			gl.glVertex2f(x, y - 32);
			gl.glEnd();
			tree_button_texture.unbind(0);
		}

		public boolean intersect (float x, float y) {
			y = cam.getHeight() - y;

			if (this.x <= x && this.x + 32 >= x && this.y >= y && this.y - 32 <= y) {
				mouse_over = true;
				return true;
			} else {
				mouse_over = false;
				return false;
			}
		}

		public void dispose () {
			tree_button_texture.dispose();
		}
	}

	class Handle {
		Vector2D o_pos = new Vector2D();
		Vector2D pos = new Vector2D();
		float WIDTH = 10;
		Vector2D dir = new Vector2D();
		int units;
		Planet src;
		Planet dst;
		float dist = 0;
		boolean dragged = false;

		public Handle (Planet src_planet, Planet dst_planet) {
			dir.set(dst_planet.getPosition()).sub(src_planet.getPosition());
			o_pos.set(src_planet.getPosition()).add(dir.x / 2, dir.y / 2);
			pos.set(o_pos);
			dir.nor();
			units = src_planet.getFriendlyCreatures(loop.getClient().getPlayer().getId());
			src = src_planet;
			dst = dst_planet;
			dist = o_pos.dst(dst_planet.getPosition());
		}

		public boolean intersects (float x, float y) {
			if (src.getFriendlyCreatures(loop.getClient().getPlayer().getId()) == 0) return false;

			float p_x = cam.getWorldToScreenX(pos.x);
			float p_y = cam.getWorldToScreenY(pos.y);
			y = cam.getHeight() - y;

			if (p_x - WIDTH <= x && p_x + WIDTH >= x && p_y - WIDTH <= y && p_y + WIDTH >= y)
				return true;
			else
				return false;
		}

		public void setDragged () {
			dragged = true;
		}

		public void render (int total_creatures) {
			if (total_creatures == 0) return;

			float x = cam.getWorldToScreenX(pos.x);
			float y = cam.getWorldToScreenY(pos.y);
			GL2 gl = GLContext.getCurrent().getGL().getGL2();
			gl.glColor4f(0.7f, 0.7f, 1, 1);
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
			gl.glBegin(GL.GL_TRIANGLES);
			gl.glVertex2f(x - dir.y * WIDTH, y + dir.x * WIDTH);
			gl.glVertex2f(x + dir.y * WIDTH, y - dir.x * WIDTH);
			gl.glVertex2f(x + dir.x * WIDTH, y + dir.y * WIDTH);
			gl.glEnd();
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);

			units = (int)(total_creatures * (1 - pos.dst(dst.getPosition()) / dist));

			if (dragged && units != 0) {
				gl.glColor3f(1, 1, 1);
				if (dir.x < 0 && dir.y < 0)
					font.renderText((int)(x - dir.y * WIDTH * 1.5f), (int)(y + dir.x * WIDTH * 1.5f), units + "/" + total_creatures);
				else
					font.renderText((int)(x + dir.y * WIDTH * 1.5f), (int)(y - dir.x * WIDTH * 1.5f), units + "/" + total_creatures);

				float src_strength = src.getCreatureStrength(loop.getClient().getPlayer().getId(), units);
				float dst_strength = dst.getCreatureStrengthExclude(loop.getClient().getPlayer().getId());
				float total_strength = src_strength + dst_strength;

				if (total_strength != 0 && dst_strength != 0) {
					src_strength /= total_strength;
					dst_strength /= total_strength;

					Color src = renderer.getPlayerColor(loop.getClient().getPlayer().getId());
					Color dst = renderer.getPlayerColor(this.dst.getOwner());

					if (dir.x < 0 && dir.y < 0) {
						gl.glBegin(GL.GL_LINES);
						gl.glColor3f(src.getR(), src.getG(), src.getB());
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f), (int)(y + dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100 * src_strength,
							(int)(y + dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glColor3f(dst.getR(), dst.getG(), dst.getB());
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100 * src_strength,
							(int)(y + dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100, (int)(y + dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f
							- 3);
						gl.glEnd();
					} else {
						gl.glBegin(GL.GL_LINES);
						gl.glColor3f(src.getR(), src.getG(), src.getB());
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f), (int)(y - dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100 * src_strength,
							(int)(y - dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glColor3f(dst.getR(), dst.getG(), dst.getB());
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100 * src_strength,
							(int)(y - dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f - 3);
						gl.glVertex2f((int)(x + dir.y * WIDTH * 1.5f) + 100, (int)(y - dir.x * WIDTH * 1.5f) - font.getHeight() * 1.5f
							- 3);
						gl.glEnd();
					}
				}
			}
		}

		public void move (float x, float y) {
			float w_x = cam.getScreenToWorldX(x) - o_pos.x;
			float w_y = cam.getScreenToWorldY(y) - o_pos.y;

			float pos = w_x * dir.x + w_y * dir.y;
			if (pos < 0) {
				this.pos.set(o_pos);
				return;
			}

			if (pos > dist) {
				this.pos.set(dst.getPosition());
			} else
				this.pos.set(o_pos.x + dir.x * pos, o_pos.y + dir.y * pos);
		}

		public void reset () {
			pos.set(o_pos);
			dragged = false;
		}
	}

	public GameInterface (GameLoop loop, GLCanvas canvas, Simulation sim, Renderer renderer) {
		this.canvas = canvas;
		this.sim = sim;
		this.cam = renderer.getCamera();
		this.loop = loop;
		this.renderer = renderer;

		for (Planet planet : sim.getPlanets()) {
			if (planet.getOwner() == loop.getClient().getPlayer().getId()) {
				this.cam.setScale(50);
				this.cam.moveToTarget(planet.getPosition().x, planet.getPosition().y, 15, 2);
				selected_planet = planet;
				renderer.setSelectedPlanet(planet);
				break;
			}
		}

	}

	public void createTextures () {
		if (mark == null) {
			try {
				mark = Texture.loadTexture(FileManager.readFile("mark.png"));
			} catch (Exception e) {
				Log.println("[GameInterface] couldn't load mark.png'");
				e.printStackTrace();
			}
		}
	}

	public void render (GLCanvas canvas) {
		if (listener_installed == false) {
			if (!is_replay) {
				canvas.addKeyListener(this);
				canvas.addMouseListener(this);
				canvas.addMouseMotionListener(this);
				listener_installed = true;
			}
		}

		createTextures();

		renderDebugStats();

		if (selected_planet != null && !is_replay) {
			int total_creatures = selected_planet.getMoveableCreatures(loop.getClient().getPlayer().getId());
			for (Handle handle : move_handles)
				handle.render(total_creatures);
		}

		renderChains(canvas);
		renderTreeButton(canvas, selected_planet);
		renderHoverPlanetStats(canvas);
		renderPlayerStats(canvas);

		renderMarks(canvas);

		canvas.getGL().getGL2().glPopMatrix();
	}

	private void renderDebugStats () {
		mat.setToOrtho2D(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.getGL().getGL2().glPushMatrix();
		canvas.getGL().getGL2().glLoadMatrixf(mat.toFloatBuffer());

		canvas.getGL().getGL2().glColor3f(1, 1, 1);
		if (font == null) {
			try {
				font = new Font(FileManager.readFile("matchworks.ttf"), 18, FontStyle.Plain);
			} catch (Exception ex) {
				Log.println("[GameInterface] couldn't open 'matchworks.ttf': " + Log.getStackTrace(ex));
			}
		}

		if (show_stats) {
			font.renderTextNewLine(
				10,
				canvas.getHeight(),
				"\n\n\n\n\n\n\n\n\nturn: " + sim.getTurn() + "\ncommand turn length: " + sim.getCommandTurnLength() + "\nping: "
					+ loop.ping.getLastPing() + " ms\nfps: " + renderer.getFramesPerSecond() + "\nobjects: " + sim.getObjectCount()
					+ "\n" + "zoom: " + cam.getScale() + "\nsimulation: " + String.format("%.2f", sim.getSimulationUpdateTime())
					+ "ms\n" + "rendering: " + String.format("%.2f", renderer.getRenderTime()) + "ms" + "\n" + "planet rendering: "
					+ String.format("%.2f", renderer.getPlanetRenderTime()) + "ms" + "\n" + "tree rendering: "
					+ String.format("%.2f", renderer.getTreeRenderTime()) + "ms" + "\n" + "creature rendering: "
					+ String.format("%.2f", renderer.getCreatureRenderTime()) + "ms" + "\n" + "glow rendering: "
					+ String.format("%.5f", renderer.getGlowRenderTime()) + "ms" + "\n" + "gui rendering: "
					+ String.format("%.5f", renderer.getGuiRenderTime()) + "ms" + "\n" + "culled: " + renderer.getCulledObjects()
					+ " objects" + "\n" + "mem: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024
					+ "/" + Runtime.getRuntime().totalMemory() / 1024 + " kb");
		}
	}

	private void renderTreeButton (GLCanvas canvas, Planet planet) {
		if (planet == null) return;

		if (is_replay) return;

		tmp.set(1, 1).nor().mul(selected_planet.getRadius() * 1.2f);

		float a_x = cam.getWorldToScreenX(selected_planet.getPosition().x + tmp.x + tmp.x / 3);
		float a_y = cam.getWorldToScreenY(selected_planet.getPosition().y + tmp.y + tmp.y / 3);
		float p_x = cam.getWorldToScreenX(selected_planet.getPosition().x + tmp.x);
		float p_y = cam.getWorldToScreenY(selected_planet.getPosition().y + tmp.y);

		GL2 gl = canvas.getGL().getGL2();

		if (selected_planet.owner == loop.getClient().getPlayer().getId()) {
			if (selected_planet.getMoveableCreatures(loop.getClient().getPlayer().getId()) >= Constants.TREE_COST) {
				gl.glColor3f(0.7f, 0.7f, 1);
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2f(p_x, p_y);
				gl.glVertex2f(a_x, a_y);
				gl.glVertex2f(a_x, a_y);
				gl.glVertex2f(a_x + 20, a_y);
				gl.glEnd();

				a_x += 25;
				a_y -= font.getHeight();

				if (tree_button == null) tree_button = new Button();

				tree_button.render(a_x, a_y + 32);
			}
		}
	}

	Vector2D tmp2 = new Vector2D();

	private void renderMarks (GLCanvas canvas) {
		for (Planet planet : sim.getPlanets()) {
			if (planet.getResources() == 0 && planet.getOwner() == loop.getClient().getPlayer().getId()) {
				tmp.set(-1, 1).nor().mul(planet.getRadius() * 1.4f);
				float a_x = cam.getWorldToScreenX(planet.getPosition().x + tmp.x);
				float a_y = cam.getWorldToScreenY(planet.getPosition().y + tmp.y);

				GL2 gl = canvas.getGL().getGL2();
				gl.glColor3f(1, 1, 1);
				mark.bind(0);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0, 0);
				gl.glVertex2f(a_x, a_y);
				gl.glTexCoord2f(1, 0);
				gl.glVertex2f(a_x - 16, a_y);
				gl.glTexCoord2f(1, 1);
				gl.glVertex2f(a_x - 16, a_y - 16);
				gl.glTexCoord2f(0, 1);
				gl.glVertex2f(a_x, a_y - 16);
				gl.glEnd();
				mark.unbind();
			}
		}
	}

	private void renderChains (GLCanvas canvas) {
		for (Planet planet : sim.getPlanets()) {
			if (planet.getOwner() == loop.getClient().getPlayer().getId() && planet.getChainedPlanet() != -1) {
				renderChain(planet, sim.getPlanet(planet.getChainedPlanet()));
			}
		}
	}

	private void renderChain (Planet source, Planet target) {
		float WIDTH = 6;
		Planet planet = source;
		Planet p = target;

		tmp2.set(p.getPosition()).sub(planet.getPosition());
		tmp2.nor();
		tmp.set(planet.getPosition()).add(tmp2.x * (planet.getRadius() + Constants.BOID_MAX_ORBIT),
			tmp2.y * (planet.getRadius() + Constants.BOID_MAX_ORBIT));

		float x = cam.getWorldToScreenX(tmp.x);
		float y = cam.getWorldToScreenY(tmp.y);

		GL2 gl = canvas.getGL().getGL2();
		gl.glEnable(GL.GL_BLEND);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
		Color col = renderer.getPlayerColor(source.getOwner());
		if (col != null)
			gl.glColor4f(col.getR(), col.getG(), col.getB(), 1);
		else
			gl.glColor4f(1, 216f / 255f, 0, 0.7f);
// gl.glLineWidth( 1 );
		gl.glBegin(GL.GL_LINES);
		for (int i = 0; i < 5; i++) {
			gl.glVertex2f(x + tmp2.x * WIDTH, y + tmp2.y * WIDTH);
			gl.glVertex2f(x - tmp2.y * WIDTH, y + tmp2.x * WIDTH);
			gl.glVertex2f(x + tmp2.x * WIDTH, y + tmp2.y * WIDTH);
			gl.glVertex2f(x + tmp2.y * WIDTH, y - tmp2.x * WIDTH);
			x += tmp2.x * 4;
			y += tmp2.y * 4;
		}
		gl.glEnd();
		gl.glLineWidth(1.5f);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glDisable(GL.GL_BLEND);
	}

	Vector2D tmp = new Vector2D();
	private boolean game_over_accepted;

	public float getHoverDelay () {
		return delay;
	}

	public void setHoverDelay (float delay) {
		this.delay = delay;
	}

	private void renderHoverPlanetStats (GLCanvas canvas) {
		if (hover_timer.getElapsedSeconds() < delay) return;

		Point p = canvas.getMousePosition();
		if (p == null) return;
		float x = cam.getScreenToWorldX(p.x);
		float y = cam.getScreenToWorldY(p.y);

		Planet selected_planet = sim.getPlanet(x, y);

		renderPlanetStats(canvas, selected_planet);

	}

	private void renderPlayerStats (GLCanvas canvas) {
		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		if (loop.getClient().getPlayerList() == null) return;

		PlayerListMessage clients = loop.getClient().getPlayerList();

		float max_width = 0;
		for (String name : clients.getNames())
			max_width = Math.max(max_width, font.getWidth(name));
		max_width += 10;

		float max_score = 0;
		int active_players = 0;
		for (Integer score : sim.getPlayerStats().values()) {
			max_score = Math.max(max_score, score);
			if (score != null) active_players++;
		}

		float y = canvas.getHeight() - (font.getHeight() * active_players - font.getHeight());
		for (int i = 0; i < clients.getIds().size(); i++) {
			Integer score = sim.getPlayerStats().get(clients.getIds().get(i));
			if (score == null) continue;

			gl.glLineWidth(5);
			Color col = renderer.getPlayerColor(clients.getIds().get(i));
			if (col != null) gl.glColor3f(col.getR(), col.getG(), col.getB());

			font.renderText(42, y, clients.getNames().get(i));

			if (max_score != 0) {
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2f(42 + max_width, y - font.getHeight() / 2 - 3);
				gl.glVertex2f(42 + max_width + score / max_score * 50, y - font.getHeight() / 2 - 3);
				gl.glEnd();
			}

			if (clients.getIds().get(i) == loop.getClient().getPlayer().getId()
				&& sim.getCreatureCount(clients.getIds().get(i)) >= Constants.MAX_CREATURES) {
				mark.bind(0);
				Widget.renderTexturedQuad(42 + max_width + score / max_score * 50 + 10, y - 3, 16, 16);
				mark.unbind();
			}

			y += font.getHeight();
			gl.glLineWidth(1.5f);
		}

		gl.glLineWidth(1.5f);
	}

	private void renderPlanetStats (GLCanvas canvas, Planet selected_planet) {
		if (selected_planet == null) return;

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		tmp.set(1, 1).nor().mul(selected_planet.getRadius() * 1.2f);

		float p_x = cam.getWorldToScreenX(selected_planet.getPosition().x + tmp.x);
		float p_y = cam.getWorldToScreenY(selected_planet.getPosition().y + tmp.y);
		float a_x = cam.getWorldToScreenX(selected_planet.getPosition().x + tmp.x + tmp.x / 3);
		float a_y = cam.getWorldToScreenY(selected_planet.getPosition().y + tmp.y + tmp.y / 3);

		float max_width = font.getWidth("strength ");
		max_width = Math.max(max_width, font.getWidth("health "));
		max_width = Math.max(max_width, font.getWidth("speed  "));

		//
		// planet stats like strength, health, etc.
		//
		gl.glColor3f(0.7f, 0.7f, 1);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex2f(p_x, p_y);
		gl.glVertex2f(a_x, a_y);
		gl.glVertex2f(a_x, a_y);
		gl.glVertex2f(a_x + 20, a_y);
		gl.glEnd();

		a_x += 25;

		if (selected_planet.owner == loop.getClient().getPlayer().getId() && this.selected_planet == selected_planet) {
			if (selected_planet.getMoveableCreatures(loop.getClient().getPlayer().getId()) >= Constants.TREE_COST) {
				a_y -= font.getHeight();
			} else
				a_y += font.getHeight() * 0.5f;
		} else
			a_y += font.getHeight() * 0.5f;

		gl.glEnable(GL.GL_BLEND);
		gl.glColor4f(0, 0, 0, 0.7f);
		Widget.renderQuad(a_x - 3, a_y - 2, font.getWidth("resources 000/000") + 3, font.getHeight() * 8 + 3);
		gl.glDisable(GL.GL_BLEND);

		gl.glColor3f(1, 1, 1);
		Widget.renderOutlinedQuad(a_x - 3, a_y - 2, font.getWidth("resources 000/000") + 3, font.getHeight() * 8 + 3);

		gl.glColor3f(1, 1, 1);
		font.renderTextNewLine(a_x, a_y, "strength\nhealth\nspeed\n\n" + "friendly  \n" + "enemies    \n" + "trees    \n"
			+ "resources");

		font.renderTextNewLine(
			font.getWidth("resources ") + a_x,
			a_y,
			"\n\n\n\n" + +selected_planet.getFriendlyCreatures(loop.getClient().getPlayer().getId()) + "\n"
				+ +selected_planet.getEnemeyCreatures(loop.getClient().getPlayer().getId()) + "\n"
				+ +selected_planet.getTrees().size() + "\n" + +selected_planet.getResources() + "/"
				+ selected_planet.getMaxResources());

		gl.glLineWidth(5);
		gl.glBegin(GL.GL_LINES);

		gl.glColor3f(0.2f, 0.2f, 0.2f);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - 3);
		gl.glVertex2f(a_x + max_width + 50, a_y - font.getHeight() / 2 - 3);

		gl.glColor3f(1, 1, 1);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - 3);
		gl.glVertex2f(a_x + max_width + 50 * selected_planet.getStrength(), a_y - font.getHeight() / 2 - 3);

		gl.glColor3f(0.2f, 0.2f, 0.2f);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - font.getHeight() - 3);
		gl.glVertex2f(a_x + max_width + 50, a_y - font.getHeight() / 2 - font.getHeight() - 3);

		gl.glColor3f(1, 1, 1);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - font.getHeight() - 3);
		gl.glVertex2f(a_x + max_width + 50 * selected_planet.getHealth(), a_y - font.getHeight() / 2 - font.getHeight() - 3);

		gl.glColor3f(0.2f, 0.2f, 0.2f);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - font.getHeight() * 2 - 3);
		gl.glVertex2f(a_x + max_width + 50, a_y - font.getHeight() / 2 - font.getHeight() * 2 - 3);

		gl.glColor3f(1, 1, 1);
		gl.glVertex2f(a_x + max_width, a_y - font.getHeight() / 2 - font.getHeight() * 2 - 3);
		gl.glVertex2f(a_x + max_width + 50 * selected_planet.getSpeed(), a_y - font.getHeight() / 2 - font.getHeight() * 2 - 3);

		gl.glEnd();
		gl.glLineWidth(1.5f);
	}

	private Handle getIntersectedHandle (float x, float y) {
		for (Handle handle : move_handles) {
			if (handle.intersects(x, y)) {
				return handle;
			}
		}

		return null;
	}

	public void mouseClicked (MouseEvent e) {
		// TODO Auto-generated method stub
	}

	public void mouseEntered (MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited (MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed (MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (loop.game_over) {
				game_over_accepted = true;
				canvas.removeKeyListener(this);
				canvas.removeMouseListener(this);
				canvas.removeMouseMotionListener(this);
				listener_installed = false;
			}

			if (tree_button != null && tree_button.intersect(e.getX(), e.getY()) && !is_replay) {
				if (selected_planet != null
					&& selected_planet.getMoveableCreatures(loop.getClient().getPlayer().getId()) >= Constants.TREE_COST) {
					SoundManager.playBuffer("tree");
					sim.plantTree(loop.getClient().getPlayer().getId(), selected_planet.getId());
				}
			} else {
				selected_handle = getIntersectedHandle(e.getX(), e.getY());
				if (selected_handle != null)
					selected_handle.setDragged();
				else {
					selected_planet = sim.getPlanet(cam.getScreenToWorldX(e.getX()), cam.getScreenToWorldY(e.getY()));
					move_handles.clear();
					renderer.setSelectedPlanet(selected_planet);

					if (selected_planet != null && !is_replay) {
						for (Integer planet_id : selected_planet.getReachablePlanets())
							move_handles.add(new Handle(selected_planet, sim.getPlanet(planet_id)));
					}
				}
			}
		} else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
			if (selected_planet != null && selected_planet.getOwner() == loop.getClient().getPlayer().getId()) {
				Planet target = sim.getPlanet(cam.getScreenToWorldX(e.getX()), cam.getScreenToWorldY(e.getY()));
				if (target != null && target.isReachablePlanet(selected_planet.getId())) {
					if (target.getChainedPlanet() == selected_planet.getId())
						sim.chainPlanets(loop.getClient().getPlayer().getId(), target.getId(), -1);
					sim.chainPlanets(loop.getClient().getPlayer().getId(), selected_planet.getId(), target.getId());
				}

				if ((target == null || target == selected_planet)
					&& selected_planet.getOwner() == loop.getClient().getPlayer().getId()) {
					sim.chainPlanets(loop.getClient().getPlayer().getId(), selected_planet.getId(), -1);
				}
			}
		}
	}

	public void mouseReleased (MouseEvent e) {
		if (selected_handle != null) {
			selected_handle.reset();

			if (selected_handle.units != 0) {
				SoundManager.playBuffer("move");
				sim.moveCreatures(loop.getClient().getPlayer().getId(), selected_handle.src.getId(), selected_handle.dst.getId(),
					selected_handle.units);
			}
		}
		selected_handle = null;
	}

	public void mouseDragged (MouseEvent e) {
		if (selected_handle != null) selected_handle.move(e.getX(), e.getY());

		Planet planet = sim.getPlanet(cam.getScreenToWorldX(e.getX()), cam.getScreenToWorldY(e.getY()));
		if (planet != last_hovered) {
			hover_timer.stop();
			hover_timer.start();
			last_hovered = planet;
		}
	}

	public void mouseMoved (MouseEvent e) {
		if (tree_button != null) tree_button.intersect(e.getX(), e.getY());

		Planet planet = sim.getPlanet(cam.getScreenToWorldX(e.getX()), cam.getScreenToWorldY(e.getY()));
		if (planet != last_hovered) {
			hover_timer.stop();
			hover_timer.start();
			last_hovered = planet;
		}
	}

	public void setSimulation (Simulation sim) {
		this.sim = sim;
		selected_handle = null;
		hover_timer.stop();
		hover_timer.start();
		selected_planet = null;
		move_handles.clear();
		game_over_accepted = false;

		for (Planet planet : sim.getPlanets()) {
			if (planet.getOwner() == loop.getClient().getPlayer().getId()) {
				this.cam.setScale(50);
				this.cam.moveToTarget(planet.getPosition().x, planet.getPosition().y, 5, 2);
				selected_planet = planet;
				renderer.setSelectedPlanet(planet);
				break;
			}
		}
	}

	public boolean isGameOverAccepted () {
		return game_over_accepted;
	}

	public void keyPressed (KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F1) this.show_stats = !this.show_stats;
	}

	public void keyReleased (KeyEvent e) {
	}

	public void keyTyped (KeyEvent e) {
	}

	public void dispose () {
		if (font != null) font.dispose();
		if (tree_button != null) tree_button.dispose();
		if (mark != null) mark.dispose();
	}

	public void setSelectedPlanet (Planet planet) {
		move_handles.clear();
		selected_handle = null;
		selected_planet = planet;

	}

	public void setIsReplay (boolean b) {
		is_replay = b;
	}

	public static Handle getSelectedHandle () {
		return selected_handle;
	}
}
