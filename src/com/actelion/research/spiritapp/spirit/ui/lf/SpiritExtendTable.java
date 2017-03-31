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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.ExtendTable;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

/**
 * Generic implementation for the tables used in Spirit. Last changes are automaticcaly tracked and highlighted
 * @author freyssj
 *
 * @param <T>
 */
public class SpiritExtendTable<T> extends ExtendTable<T> {

	private long lastQuery = 0;

	public SpiritExtendTable(ExtendTableModel<T> model) {
		super(model);

		getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				lastQuery = System.currentTimeMillis();
				repaint();
			}
		});
	}

	public long getLastQuery() {
		return lastQuery;
	}

	@SuppressWarnings("unchecked")
	public void reload() {
		List<T> rows = getRows();
		if(rows.size()>0 && rows.get(0) instanceof IObject) {
			List<T> sel = getSelection();
			for (int i = 0; i < rows.size(); i++) {
				IObject old = (IObject) rows.get(i);

				if(old.getId()>0 && !JPAUtil.isValid(old)) {
					//The row is coming from the DB, reload it (or remove it if it deleted)
					T t = (T) JPAUtil.reattach(old);
					if(t==null) {
						rows.remove(i--);
					} else {
						rows.set(i, t);
					}
				} else if(getModel().getTreeColumn()!=null) {
					//The row is not coming from the DB and has children, reload the children (or remove it if it deleted)
					List<IObject> children = (List<IObject>) getModel().getTreeChildren((T) old);
					for (int j = 0; j < children.size(); j++) {
						IObject c = JPAUtil.reattach(children.get(j));
						if(c==null) {
							children.remove(j--);
						} else {
							children.set(j, c);
						}
					}
				}
			}

			getModel().fireTableDataChanged();

			setSelection(sel);
		}
	}

	/**
	 * Highlight rows, if they were updated during the last 10s, or 12h
	 */
	@Override
	public void postProcess(T row, int rowNo, Object value, JComponent c) {

		if(row==null) return;
		Date last = null;
		if(row instanceof Biosample) last = ((Biosample) row).getUpdDate();
		else if(row instanceof Study) last = ((Study) row).getUpdDate();
		else if(row instanceof Result) last = ((Result) row).getUpdDate();
		else if(row instanceof FoodWater) last = ((FoodWater) row).getUpdDate();
		else if(row instanceof Location) last = ((Location) row).getUpdDate();
		else return;


		if(last!=null && Math.abs(last.getTime()-getLastQuery())<10*1000L) { //10s
			c.setBackground(Color.YELLOW);
		} else if(last!=null && Math.abs(last.getTime()-getLastQuery())<12*3600*1000L) { //12h
			c.setBackground(UIUtils.getDilutedColor(c.getBackground(), Color.YELLOW, .9));
		}

	}
}
