package de.walware.ecommons.ltk;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.text.ISourceFragment;
import de.walware.ecommons.text.ReadOnlyDocument;

public abstract class GenericFragmentSourceUnit implements ISourceUnit {
	
	
	private final IElementName fName;
	
	private final ISourceFragment fFragment;
	private final long fTimestamp;
	
	private AbstractDocument fDocument;
	
	private int fCounter = 0;
	
	
	public GenericFragmentSourceUnit(final String id, final ISourceFragment fragment) {
		fFragment = fragment;
		fName = new IElementName() {
			@Override
			public int getType() {
				return 0x011;
			}
			@Override
			public String getDisplayName() {
				return fFragment.getName();
			}
			@Override
			public String getSegmentName() {
				return fFragment.getName();
			}
			@Override
			public IElementName getNextSegment() {
				return null;
			}
		};
		fTimestamp = System.currentTimeMillis();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	@Override
	public boolean isSynchronized() {
		return true;
	}
	
	@Override
	public String getId() {
		return fFragment.getId();
	}
	
	public ISourceFragment getFragment() {
		return fFragment;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_CHUNK C2_SOURCE_CHUNK}.
	 */
	@Override
	public int getElementType() {
		return IModelElement.C2_SOURCE_CHUNK;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IElementName getElementName() {
		return fName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists() {
		return fCounter > 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually doesn't have a resource/path.
	 */
	@Override
	public Object getResource() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized AbstractDocument getDocument(final IProgressMonitor monitor) {
		if (fDocument == null) {
			fDocument = new ReadOnlyDocument(fFragment.getSource(), fTimestamp);
		}
		return fDocument;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentStamp(final IProgressMonitor monitor) {
		return fTimestamp;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SourceContent getContent(final IProgressMonitor monitor) {
		return new SourceContent(fTimestamp, fFragment.getSource());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdapter(final Class required) {
		if (ISourceFragment.class.equals(required)) {
			return fFragment;
		}
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public AstInfo getAstInfo(final String type, final boolean ensureSync, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ISourceUnitModelInfo getModelInfo(final String type, final int syncLevel, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IModelElement getModelParent() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasModelChildren(final Filter filter) {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<? extends IModelElement> getModelChildren(final Filter filter) {
		return NO_CHILDREN;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProblemRequestor getProblemRequestor() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void connect(final IProgressMonitor monitor) {
		fCounter++;
		if (fCounter == 1) {
			final SubMonitor progress = SubMonitor.convert(monitor, 1);
			register();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void disconnect(final IProgressMonitor monitor) {
		fCounter--;
		if (fCounter == 0) {
			unregister();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean isConnected() {
		return (fCounter > 0);
	}
	
	protected void register() {
	}
	
	protected void unregister() {
	}
	
}
