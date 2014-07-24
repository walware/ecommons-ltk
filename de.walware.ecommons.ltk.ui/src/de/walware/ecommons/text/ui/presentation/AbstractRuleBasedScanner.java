/*=============================================================================#
 # Copyright (c) 2005-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui.presentation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;

import de.walware.ecommons.text.ui.settings.TextStyleManager;


/**
 * BufferedRuleBasedScanner with managed text styles/tokens.
 */
public abstract class AbstractRuleBasedScanner extends BufferedRuleBasedScanner {
	
	
	private final TextStyleManager textStyles;
	
	
	public AbstractRuleBasedScanner(final TextStyleManager textStyles) {
		this.textStyles= textStyles;
	}
	
	
	protected void initRules() {
		final List<IRule> rules= new ArrayList<>();
		createRules(rules);
		setRules(rules.toArray(new IRule[rules.size()]));
	}
	
	protected abstract void createRules(final List<IRule> rules);
	
	
	protected IToken getToken(final String key) {
		return this.textStyles.getToken(key);
	}
	
}
