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

package org.ocallahan.chronomancer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.Type;

public class RenderedData {
    private DataContext dataContext;
    private Type type;
    private IDataSource dataSource;
    private long tStamp;
    
    public RenderedData(DataContext dataContext, Type type,
    		IDataSource dataSource, long tStamp) {
		this.dataContext = dataContext;
		this.type = type;
		this.dataSource = dataSource;
		this.tStamp = tStamp;
	}

	public DataContext getContext() {
		return dataContext;
	}

	public IDataSource getSource() {
		return dataSource;
	}

	public Type getType() {
		return type;
	}
	
	public Type getBareType() {
		return type.getBareType();
	}
	
	public long getTStamp() {
		return tStamp;
	}
	
	private void findLastWrite() {
		dataSource.findPreviousChangeTStamp(0, type.getSize(), new IDataSource.TStampReceiver() {
			public void receiveChange(long stamp, long offset, long length) {
				// TODO
			}
			public void receiveEndOfScope(long stamp) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	public void populateContextMenu(State state, MenuManager menuManager) {
		if (type.getSize() != Type.UNKNOWN_SIZE) {
			Action findLastWrite = new Action() {
				public void run() {
					findLastWrite();
				}
			};
			findLastWrite.setText("Find Last Write");
			findLastWrite.setToolTipText("Find last write to this data");
			findLastWrite.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
					getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
			menuManager.add(findLastWrite);
		}
		
		menuManager.add(new Separator());
		for (ITypeRenderer r : state.getTypeRenderer().getRenderers(this)) {
			Action render = new Action() {
				public void run() {
					// TODO fill me in
				}
			};
			render.setText("View As " + r.getName());
			render.setToolTipText("View this data as " + r.getName());
			menuManager.add(render);
		}
    }
	
	public static boolean allValid(boolean[] valid) {
		for (boolean b : valid) {
			if (!b)
				return false;
		}
		return true;
	}
}
