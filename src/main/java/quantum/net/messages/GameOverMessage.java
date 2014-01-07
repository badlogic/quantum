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
package quantum.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public strictfp class GameOverMessage extends Message
{
	public GameOverMessage( )
	{
		super( MessageTypes.GAME_OVER );
	}
	
	@Override
	public void read(DataInputStream in) throws Exception 
	{	
		
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{	
		out.writeInt( type );	
	}

}
