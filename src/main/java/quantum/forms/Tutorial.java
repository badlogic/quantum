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


import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.GameLoop;
import quantum.game.Planet;
import quantum.game.Simulation;
import quantum.gfx.Color;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.CustomDialog;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.Label;
import quantum.gui.VerticalAlignement;
import quantum.gui.Widget;
import quantum.math.Vector2D;
import quantum.net.Client;
import quantum.utils.Timer;

public class Tutorial implements DisplayListener
{
	Quantum quantum;
	Gui gui;		
	GameLoop loop;
	Simulation sim = new Simulation( true );
	Tutorial self = this;		
	Label label;
	CustomDialog dialog;
	
	TutorialState state = TutorialState.Camera;
	Timer timer = new Timer( );
	
	enum TutorialState
	{
		Camera,
		Move, 
		PlantTree, Statistics, Combat, Chain 	
	}
	
	public Tutorial( final Quantum quantum, final Gui gui )
	{
		this.quantum = quantum;
		this.gui = gui;		
		this.loop = new GameLoop( new Client("Tutorial Player"), sim );		
		quantum.addDisplayListener( this );
				
		Button back = new Button( gui, "Back" );
		back.setSize( 50, 24 );
		back.setBackgroundColor( new Color( 0, 0, 0, 0.7f ) );
		back.setClickedListener( new ClickedListener( ) {
			public void clicked(Widget widget) 
			{			
				cleanUp();
				new StartMenu( quantum, gui );
			}			
		});		
		label = new Label( gui, "", 300 );		
		label.appendText( "Welcome to the Tutorial!\n\n" );
		label.appendText( "In the course of this tutorial you will learn" );
		label.appendText( " how to play Quantum. Let's start with the" );
		label.appendText( " camera controls.\n\n" );
		label.appendText( "To move the camera hold the right mouse button");
		label.appendText( " and drag the mouse. To zoom in and out use the" );
		label.appendText( " mouse wheel or the up and down cursor keys. \n\nTry it!");			
		
		dialog = new CustomDialog( gui, HorizontalAlignement.LEFT, VerticalAlignement.TOP, 300, "Tutorial - Camera Controls", label, back  );
		
		gui.add( dialog );		
		
		Planet planet = new Planet( sim, new Vector2D( 0, 0 ), 100, 0.7f, 0.6f, 1, 50 );
		planet.setOwner( loop.getClient().getPlayer().getId() );
		sim.addObject( planet );
		
		for( int i = 0; i < 6; i++ )
			planet.spawnCreature();
		
		timer.stop();					
	}
	
	private void cleanUp( )
	{
		quantum.removeDisplayListener( self );
		gui.remove(dialog);
		loop.dispose();
	}

	public void display(GLCanvas canvas) 
	{	
		loop.update( canvas );
		loop.render( canvas );
		
		if( loop.getRenderer().getPlayerColor(2) == null )
			loop.getRenderer().allocatePlayerColor( sim, 2 );
		
		if( state == TutorialState.Camera )
			doCamera( );
		
		if( state == TutorialState.Move )
			doSelection( );
		
		if( state == TutorialState.PlantTree )
			doPlantTree( );
		
		if( state == TutorialState.Statistics )
			doStatistics( );
		
		if( state == TutorialState.Combat )
			doCombat( );
		
		if( state == TutorialState.Chain )
			doChain( );
	}

	Vector2D old_cam_pos = new Vector2D( );
	float old_cam_scale = -1;
	private void doCamera( )
	{
		if( timer.isRunning() == false && old_cam_scale == -1 )
		{
			old_cam_pos.x = loop.getRenderer().getCamera().getPosition().getX();
			old_cam_pos.y = loop.getRenderer().getCamera().getPosition().getY();
			old_cam_scale = loop.getRenderer().getCamera().getScale();
		}
		
		if( old_cam_pos.x != loop.getRenderer().getCamera().getPosition().getX() &&
			old_cam_pos.y != loop.getRenderer().getCamera().getPosition().getY() &&
			old_cam_scale != loop.getRenderer().getCamera().getScale() && timer.isRunning() == false )
		{
			timer.start();
		}
		
		if( timer.getElapsedSeconds() > 2 )
		{
			Planet planet = new Planet( sim, new Vector2D( 3000, 0 ), 100, 0.8f, 0.4f, 0.9f, 15 );
			sim.addObject( planet );
			sim.calculatePaths();
			state = TutorialState.Move;			
			timer.stop();
		}
	}
	
	private void doSelection( )
	{
		
		if( timer.isRunning() == false )
		{
			label.setText( "" );
			label.appendText( "Good! Now that you know how to use " + 
							  "the camera let's move on to what " + 
							  "you see in front of you.\n\n" +
							  "By now you will have noticed the " +
							  "red objects orbiting the circle. " + 
							  "These objects are yours! We'll call " + 
							  "them creatures for the sake of simplicity. " +
							  "The circle is a planet that also belongs " + 
							  "to you. This is indicated by the color of " +
							  "creature depicted inside the planet\n\n" +
							  "Creatures are your means to conquer " +
							  "new planets. To do so we have to send " +
							  "them to other planets.\n\n" +
							  "Click on your home planet. You will see " +
							  "lines indicating where you can send your " + 
							  "creatures. Additionally you will see a " +
							  "triangle on each line. If you click and drag " +
							  "that triangle you can specify how many of " +
							  "your creatures should be moved to that planet.\n\n" +
							  "Send at least 3 of your creatures to the " +
							  "planet on the left!" );
							  								
			timer.start();
			dialog.setCaption("Tutorial - Creature Movement" );
		}
		
		
		if( sim.getPlanets().get(1).getFriendlyCreatures(0) == 3 )
		{
			state = TutorialState.PlantTree;
			timer.stop();
		}
	}
	
	
	private void doPlantTree() 
	{
		if( timer.isRunning() == false )
		{
			label.setText( "" );
			label.appendText( "Fine. The planet you just conquered " + 
							  "belongs to you now. This is again indicated " +
							  "by the color of the symbol in the middle of " + 
							  "the planet.\n\n" +
							  "To create new creatures you have to plant one " + 
							  "or more trees on one of your planets. A tree " + 
							  "costs you 10 of your creatures orbiting the " + 
							  "planet. \n\nI just spawned 10 additional creatures " + 
							  "for you on the planet to the right. If you click the planet you might notice " +
							  "the symbol looking like a tree that's displayed beside " +
							  "the planet. This symbol indicates that there's enough " +
							  "creatures on that planet to build a tree.\n\n" +
							  "Click the tree symbol and plant a tree!"
							 );
			
			for( int i = 0; i < 10; i++ )
				sim.getPlanets().get(1).spawnCreature();
			timer.start();
			dialog.setCaption("Tutorial - Tree Planting" );
		}
		
		if( sim.getPlanets().get(1).getTrees().size() != 0 || sim.getPlanets().get(0).getTrees().size() != 0 )
		{
			state = TutorialState.Statistics;
			timer.stop();
		}
	}
	
	private void doStatistics( )
	{
		if( timer.isRunning() == false )
		{
			label.setText("");
			label.appendText( "You can see the tree grow now. It will grow, " +
							  "produce a creature, grow two new branches, produce " +
							  "another two creatures and so on. To produce a creature " +
							  " the tree has to use up one resource of the planet. Planets " +
							  "have limited resources. This is indicated by their size, the " +
							  "bigger a planet the more resources it has.\n\n" +
							  "Once all resources of the planet are used up no more creatures " +
							  "will be produced unless you move all your creatures away from that " +
							  "planet. The planet will then regain resources until it has reachead " +
							  "its maximum amount of resources or you send one of your creatures to " +
							  "it. While the planet regrows resources your trees will wither down to " +
							  "their roots.\n\n" + 
							  "For demonstration purposes i removed the other planet. While the tree " +
							  "uses up all of its planets resources let's talk about the creatures again. " +
							  "Creatures have 3 properties: strenght, health and speed. Strength determines " + 
							  "how much damage they will incure when combating with enemy creatures, " +
							  "health is a measure of how much damage a creature can take and speed dictates " + 
							  "how fast a creature is. A creature inherits this attributes from the planet it " +
							  "was produced on. If you move your mouse over a planet and wait a little you " +
							  "will be shown what type of creature will be produced on that planet. Also, " +
							  "the symbol in the middle of a planet encodes its creatures attributes. The top " + 
							  "triangle indicates strength, the center triangle stands for health and the bottom two " + 
							  "triangles encode speed.\n\n" +
							  "By the time the resources of a planet are used up an excalamtion mark will " + 
							  "be shown indicating that you should move your creatures to another planet. ");
			timer.start();
			
			if( sim.getPlanets().get(1).getTrees().size() != 0 )
				sim.removeObject( sim.getPlanets().get(0) );
			else			
				sim.removeObject( sim.getPlanets().get(1) );
			
			loop.getRenderer().setSelectedPlanet( null );
			loop.getGameInterface().setSelectedPlanet( null );
			dialog.setCaption("Tutorial - Statistics" );
		}
		
		if( sim.getPlanets().get(0).getResources() == 0 )
		{								
			timer.stop();
			state = TutorialState.Combat;
		}
	}
	
	private void doCombat() 
	{	
		if( timer.isRunning() == false )
		{
			label.setText( "" );
			label.appendText( "It's time for combat. To attack an enemy planet simply move " + 
							  "your creatures to it. The battle will be won by the player who " + 
							  "has more power arithmetically. Once all enemy creatures and trees " + 
							  "have been erased from a planet it is yours.\n\n" +
							  "When you move your creatures to an enemy planet a bar will be shown " +
							  "indicating the strength of your creatures in red compared to the creatures " + 
							  "of the enemy in green. Take over the enemy planet now!" );
			
			Planet planet = new Planet( sim, new Vector2D( 2000, 2000 ), 100, 1, 1, 1, 50 );
			planet.setOwner( 2 );
			sim.addObject( planet );
			for( int i = 0; i < 5; i++ )
				planet.spawnCreature();
			sim.calculatePaths();
			timer.start();
			dialog.setCaption("Tutorial - Attacking" );
		}
		
		if( sim.getPlanets().get(1).getOwner() == 0 )
		{
			timer.stop();
			state = TutorialState.Chain;
		}
	}
	
	private void doChain( )
	{
		if( timer.isRunning() == false )
		{
			label.setText( "" );
			label.appendText( "As it can be tedious to move creatures from planets " + 
							  "to the battle front there's one more element called " + 
							  "chaining. If you chain one of your planets to another " + 
							  " one of your planets creatures will automatically move " +
							  " from the source planet to the target planet. This can " +
							  "relieve you from some micro managment.\n\n" + 
							  "To chain a planet with another planet select the planet " +
							  "you want the creatures to move from, hold down the CTRL " + 
							  "key and click the planet you want the creatures to move to. " +
							  "To unchain planets simply click the planet the chain originates " +
							  "from, hold down the CTRL key and click into empty space.\n\n" +
							  "You are now equiped to play epic Quantum battles. Click the back " + 
							  "button if you are comfortable with chaining to return to the main " + 
							  "menu" );
			timer.start();
			dialog.setCaption("Tutorial - Chaining" );
			
			sim.removeObject( sim.getPlanets().get(0) );
//			sim.removeObject( sim.getPlanets().get(0) );
			
			float x = -5000;
			for( int i = 0; i < 5; i++ )
			{
				Planet planet = new Planet( sim, new Vector2D( x, -5000 + (float)Math.random() * 100 ), 100, 1, 1, 1, 10 );
				planet.setOwner( 0 );
				sim.addObject( planet );
				
				for( int j = 0; j < 10; j++ )				
					planet.spawnCreature();	
				
				x+= 1500;
				planet.spawnTree();
			}
			
			x = -3500;
			for( int i = 0; i < 3; i++ )
			{
				Planet planet = new Planet( sim, new Vector2D( x, 5000 + (float)Math.random() * 100), 100, 1, 1, 1, 10 );
				planet.setOwner( 2 );
				sim.addObject( planet );
				
				for( int j = 0; j < 10; j++ )				
					planet.spawnCreature();	
				
				x+= 1500;
				planet.spawnTree();
			}
			
			Planet planet = new Planet( sim, new Vector2D( -1500, 0 ), 100, 1, 1, 1, 10 );
			sim.addObject( planet );
			planet.setOwner(0);
			loop.getRenderer().getCamera().moveToTarget( planet.getPosition().x, planet.getPosition().y, 20, 2 );						
			loop.getRenderer().setSelectedPlanet( planet );
			loop.getGameInterface().setSelectedPlanet( planet );
			sim.calculatePaths();
		}		
	}
}
