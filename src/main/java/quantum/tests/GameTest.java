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
package quantum.tests;

import javax.swing.SwingUtilities;

public class GameTest extends BasicTest
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1872272579225115117L;

	public GameTest() throws Exception {
		super();	
	}
	
	public static void main( String[] argv ) throws Exception
	{		
		final GameTest app = new GameTest();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                app.setVisible(true);
            }
        }); 
	}
}
