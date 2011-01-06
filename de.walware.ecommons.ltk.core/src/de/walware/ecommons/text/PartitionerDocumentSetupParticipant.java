/*******************************************************************************
 * Copyright (c) 2008-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;

import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * Base implementation of {@link IDocumentSetupParticipant} configuring the
 * document for a {@link Partitioner}.
 */
public abstract class PartitionerDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	
	public PartitionerDocumentSetupParticipant() {
	}
	
	
	public void setup(final IDocument document) {
		if (document instanceof IDocumentExtension3) {
			final IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			
			if (extension3.getDocumentPartitioner(getPartitioningId()) == null) {
				// Setup the document scanner
				final Partitioner partitioner = createDocumentPartitioner();
				partitioner.connect(document, true);
				extension3.setDocumentPartitioner(getPartitioningId(), partitioner);
			}
			else {
				final Partitioner partitioner = createDocumentPartitioner();
				partitioner.connect(document, true);
				if (!Partitioner.equalPartitioner(partitioner, extension3.getDocumentPartitioner(getPartitioningId()))) {
					LTKCorePlugin.getDefault().getLog().log(new Status(IStatus.WARNING, LTKCorePlugin.PLUGIN_ID,
							"Different partitioner for same partitioning!")); //$NON-NLS-1$
				}
				partitioner.disconnect();
			}
		}
	}
	
	public abstract String getPartitioningId();
	
	protected abstract Partitioner createDocumentPartitioner();
	
}
