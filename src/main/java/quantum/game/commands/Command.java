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
package quantum.game.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public strictfp abstract class Command 
{
	protected int type = CommandTypes.MOVE_CREATURE;
	
	public Command( int type )
	{
		this.type = type;
	}
	
	public int getType( )
	{
		return type;
	}
	
	public abstract void write( DataOutputStream out ) throws Exception;
	public abstract void read( DataInputStream in ) throws Exception;
}
