/* ***** BEGIN LICENSE BLOCK *****
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is mozilla.org Chronicle code.

The Initial Developer of the Original Code is Mozilla Foundation.
Portions created by Mozilla Foundation are Copyright (C) 2007
Mozilla Foundation. All Rights Reserved.

Contributor(s): robert@ocallahan.org
*/

package org.ocallahan.chronomancer.views;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.ocallahan.chronomancer.IEventQuery;
import org.ocallahan.chronomancer.IEventQueryFactory;
import org.ocallahan.chronomancer.State;

public class QueryCreationDialog extends TitleAreaDialog {
	private IEventQueryFactory factory;
	private String name;
	private State state;
	private IEventQuery base;
	private IEventQueryFactory.Configurator configurator;
	
	public QueryCreationDialog(Shell parentShell, String name,
			                   State state, IEventQueryFactory factory,
			                   IEventQuery base) {
		super(parentShell);
		this.factory = factory;
		this.name = name;
		this.state = state;
		this.base = base;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Search " + name);
		setMessage("Configure your search");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		configurator = factory.createQueryConfigurator(parent, state, base);
		return (Control)configurator;
	}
	
	@Override
	protected void okPressed() {
		state.addQuery(configurator.create());
	    super.okPressed();	
	}
	
	@Override
	protected void cancelPressed() {
		configurator.cancel();
		super.cancelPressed();
	}
}
