/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.ISynchronizable;

import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.SourceDocumentRunnable;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * WorkingBuffer using {@link ITextFileBuffer}.
 * <p>
 * Usually used for editors / the editor context.</p>
 */
public class FileBufferWorkingBuffer extends de.walware.ecommons.ltk.core.impl.FileBufferWorkingBuffer {
	
	
	public static void syncExec(final SourceDocumentRunnable runnable)
			throws InvocationTargetException {
		final AtomicReference<InvocationTargetException> error= new AtomicReference<>();
		UIAccess.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Object docLock= null;
				final AbstractDocument document= runnable.getDocument();
				if (document instanceof ISynchronizable) {
					docLock= ((ISynchronizable) document).getLockObject();
				}
				if (docLock == null) {
					docLock= new Object();
				}
				
				DocumentRewriteSession rewriteSession= null;
				try {
					if (runnable.getRewriteSessionType() != null) {
						rewriteSession= document.startRewriteSession(runnable.getRewriteSessionType());
					}
					synchronized (docLock) {
						if (runnable.getStampAssertion() > 0 && document.getModificationStamp() != runnable.getStampAssertion()) {
							throw new CoreException(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
									"Document out of sync (usuallly caused by concurrent document modifications)." ));
						}
						runnable.run();
					}
				}
				catch (final InvocationTargetException e) {
					error.set(e);
				}
				catch (final Exception e) {
					error.set(new InvocationTargetException(e));
				}
				finally {
					if (rewriteSession != null) {
						document.stopRewriteSession(rewriteSession);
					}
				}
			}
		});
		if (error.get() != null) {
			throw error.get();
		}
	}
	
	
	public FileBufferWorkingBuffer(final ISourceUnit unit) {
		super(unit);
	}
	
	
}
