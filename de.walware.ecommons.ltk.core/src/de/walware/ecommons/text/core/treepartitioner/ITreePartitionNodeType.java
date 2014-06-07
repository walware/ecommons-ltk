/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.core.treepartitioner;

import org.eclipse.jface.text.IDocument;


public interface ITreePartitionNodeType {
	
	/**
	 * Returns the partition content type.
	 * 
	 * @return the identifier of the type
	 */
	String getPartitionType();
	
	/**
	 * Returns if the this partition should be preferred to the parent at the begin of the position.
	 * 
	 * @return <code>true</code> to prefere this partition, otherwise false
	 */
	boolean prefereAtBegin(ITreePartitionNode node, IDocument document);
	
	/**
	 * Returns if the this partition should be preferred to the parent at the end of the position.
	 * 
	 * @return <code>true</code> to prefere this partition, otherwise false
	 */
	boolean prefereAtEnd(ITreePartitionNode node, IDocument document);
	
}
