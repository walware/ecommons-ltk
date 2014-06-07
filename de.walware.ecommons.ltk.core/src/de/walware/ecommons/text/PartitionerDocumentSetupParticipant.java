/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitionerExtension3;
import org.eclipse.jface.text.ISynchronizable;


/**
 * Base implementation of {@link IDocumentSetupParticipant} configuring the document for a
 * {@link IDocumentPartitioner document partitioner}.
 */
public abstract class PartitionerDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	
	public PartitionerDocumentSetupParticipant() {
	}
	
	
	@Override
	public void setup(final IDocument document) {
		if (document instanceof IDocumentExtension3) {
			final Object synch = getLockObject(document);
			synchronized (synch) {
				doSetup(document);
			}
		}
		else {
			throw new UnsupportedOperationException("IDocumentExtension3 required."); //$NON-NLS-1$
		}
	}
	
	private Object getLockObject(final IDocument document) {
		Object synch;
		if (document instanceof ISynchronizable) {
			synchronized (document) {
				synch = ((ISynchronizable) document).getLockObject();
				if (synch == null) {
					synch = new Object();
					((ISynchronizable) document).setLockObject(synch);
				}
			}
		}
		else {
			synch = new Object();
		}
		return synch;
	}
	
	protected void doSetup(final IDocument document) {
		final IDocumentExtension3 extension3 = (IDocumentExtension3) document;
		if (extension3.getDocumentPartitioner(getPartitioningId()) == null) {
			// Setup the document scanner
			final IDocumentPartitioner partitioner= createDocumentPartitioner();
			if (partitioner instanceof IDocumentPartitionerExtension3) {
				((IDocumentPartitionerExtension3) partitioner).connect(document, true);
			}
			else {
				partitioner.connect(document);
			}
			extension3.setDocumentPartitioner(getPartitioningId(), partitioner);
		}
	}
	
	public abstract String getPartitioningId();
	
	protected abstract IDocumentPartitioner createDocumentPartitioner();
	
}
