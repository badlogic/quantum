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

public class Player 
{
	String name;
	int id;
	
	public Player( String name, int id )
	{
		this.name = name;
		this.id = id;
	}
	
	public String getName( )
	{
		return name;
	}
	
	public int getId( )
	{
		return id;
	}
	
	public void setId( int id )
	{
		this.id = id;
	}
	
	public String toString( )
	{
		return name;
	}
}
