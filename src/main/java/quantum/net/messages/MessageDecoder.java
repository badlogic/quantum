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

public strictfp class MessageDecoder 
{	
	public static Message decode( DataInputStream in ) throws Exception
	{
		Message msg = null;					
		
		int type = in.readInt();
		switch( type )
		{
		case MessageTypes.PLAYER_LIST:
			PlayerListMessage cl = new PlayerListMessage( );
			cl.read(in);
			msg = cl;
			break;
		case MessageTypes.DISCONNECTED:
			DisconnectedMessage d = new DisconnectedMessage( );
			d.read(in);
			msg = d;
			break;
		case MessageTypes.READY:
			ReadyMessage r = new ReadyMessage( );
			r.read(in);
			msg = r;
			break;
		case MessageTypes.TEXT_MESSAGE:
			TextMessage t = new TextMessage( );
			t.read(in);
			msg = t;
			break;
		case MessageTypes.SIMULATION:
			SimulationMessage s = new SimulationMessage( );
			s.read(in);
			msg = s;
			break;
		case MessageTypes.COMMAND_BUFFER:
			CommandBufferMessage c = new CommandBufferMessage( );
			c.read(in);
			msg = c;
			break;
		case MessageTypes.PING:
			PingMessage p = new PingMessage( );
			p.read(in);
			msg = p;
			break;
		case MessageTypes.GAME_OVER:
			GameOverMessage e = new GameOverMessage( );
			msg = e;
			break;
		case MessageTypes.MAP_LIST:
			MapListMessage m = new MapListMessage( );
			m.read(in);
			msg = m;
			break;
		case MessageTypes.MAP_IMAGE:
			MapImageMessage mi = new MapImageMessage( );
			mi.read(in);
			msg = mi;
			break;
		case MessageTypes.VOTE:
			VoteMessage vm = new VoteMessage( );
			vm.read(in);
			msg = vm;
			break;
		case MessageTypes.PLAYER:
			PlayerMessage pm = new PlayerMessage( );
			pm.read(in);
			msg = pm;
			break;
		case MessageTypes.VERSION:
			VersionMessage versionm = new VersionMessage( );
			versionm.read(in);
			msg = versionm;
			break;
		default:
			break;
		}
		
		return msg;
	}
}
