/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
 * CH-4123 Allschwil, Switzerland.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritapp.ui.util.formtree;

import javax.swing.JComponent;

public class CheckboxNode extends AbstractCheckboxNode<Boolean> {

	public CheckboxNode(FormTree tree, String label) {
		this(tree, label, null);
	}
	public CheckboxNode(FormTree tree, String label, Strategy<Boolean> strategy) {	
		super(tree, label, strategy);
		checkbox.setText(label);
		
		addEventsToComponent();
		
	}	
	
	@Override
	public JComponent getComponent() {		
		return checkbox;
	}	
	
	@Override
	protected void updateModel() {
		if(strategy!=null) strategy.setModel(checkbox.isSelected());
	}
	@Override
	protected void updateView() {
		if(strategy!=null) {
			boolean value = strategy.getModel()==Boolean.TRUE;
			boolean mustChange = checkbox.isSelected() != value;
			if(mustChange) {		
				checkbox.setSelected(value);
			}
		}
	}
	
}
