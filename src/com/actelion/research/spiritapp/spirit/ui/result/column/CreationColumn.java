/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
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

package com.actelion.research.spiritapp.spirit.ui.result.column;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.lf.CreationLabel;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class CreationColumn extends Column<Result, String> {
	private final boolean creation;
	public CreationColumn(boolean creation) {
		super(creation?"\nCreDate": "\nLastUpdate", String.class, 40);
		setHideable(!creation);
		this.creation = creation;	
	}
	
	@Override
	public String getCategory() {
		return "Cre";
	}
	
	@Override
	public String getToolTipText() {
		return creation? "Creation Date + Owner": "Last Update";
	}
	
	@Override
	public float getSortingKey() {return 10.1f;}
	
	@Override
	public String getValue(Result row) {
		return creation? row.getCreUser() + "\t" + Formatter.formatDate(row.getCreDate()): 
			row.getUpdUser()  + "\t" + Formatter.formatDate(row.getUpdDate());
	}	
	
	@Override
	public boolean isEditable(Result row) {return false;}
	
	private CreationLabel ownerLabel = new CreationLabel();
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		ownerLabel.setValue(creation? row.getCreUser(): row.getUpdUser(), null, creation? row.getCreDate(): row.getUpdDate(), 
				SpiritRights.canEdit(row, Spirit.getUser())? 
					RightLevel.WRITE: 
					RightLevel.READ);
		return ownerLabel;	
	}
	
	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Result> table, JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));
		
		popupMenu.add(new AbstractAction("Sort by " + (creation?"CreUser": "UpdUser")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(CreationColumn.this, 1, new Comparator<Result>() {
					@Override
					public int compare(Result o1, Result o2) {
						return CompareUtils.compare(creation? o1.getCreUser(): o1.getUpdUser(), creation? o2.getCreUser(): o2.getUpdUser());
					}
				});
			}
		});
		popupMenu.add(new AbstractAction("Sort by " + (creation?"CreDate": "UpdDate")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(CreationColumn.this, 1, new Comparator<Result>() {
					@Override
					public int compare(Result o1, Result o2) {
						return CompareUtils.compare(creation? o1.getCreDate(): o1.getUpdDate(), creation? o2.getCreDate(): o2.getUpdDate());
					}
				});
			}
		});
	}
		
	@Override
	public void postProcess(AbstractExtendTable<Result> table, Result row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(BiosampleTableModel.COLOR_NONEDIT);
	}		
	
}