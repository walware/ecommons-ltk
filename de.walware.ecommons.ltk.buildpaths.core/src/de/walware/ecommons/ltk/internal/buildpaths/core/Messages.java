/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.core;

import org.eclipse.osgi.util.NLS;


public class Messages {
	
	
	public static String BuildpathStatus_Entry_IllegalSourceFolderPath_message;
	public static String BuildpathStatus_Entry_UnboundSourceFolder_message;
	public static String BuildpathStatus_Entry_DuplicateExtraAttribute_message;
	
	public static String BuildpathStatus_DuplicateEntryPath_message;
	
	
	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
