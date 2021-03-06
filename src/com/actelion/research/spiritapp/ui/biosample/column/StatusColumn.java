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

package com.actelion.research.spiritapp.ui.biosample.column;


import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StatusColumn extends Column<Biosample, Status> {

	public StatusColumn() {
		super("Sample\nStatus", Status.class, 50, 100);
	}

	@Override
	public float getSortingKey() {
		return 9.6f;
	}

	@Override
	public Status getValue(Biosample row) {
		return row.getStatus();
	}
	@Override
	public boolean isEditable(Biosample row) {
		return false;
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);
		Status status = (Status) value;
		if(status!=null && status.getForeground()!=null) {
			lbl.setForeground(status.getForeground());
		}
		if(status!=null && status.getBackground()!=null) {
			lbl.setBackground(status.getBackground());
		}
	}

	@Override
	public boolean shouldMerge(Biosample r1, Biosample r2) {return false;}


}