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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.JObjectComboBox;

public class GroupComboBox extends JObjectComboBox<Group> {

	private final GroupLabel groupLabel;

	public GroupComboBox() {
		groupLabel = new GroupLabel();
		setTextWhenEmpty("Group...");
	}

	public GroupComboBox(Collection<Group> groups) {
		this();
		setValues(groups);
	}

	@Override
	public void setValues(Collection<Group> values) {
		if(values!=null) {
			List<Group> res = new ArrayList<>();
			for (Group group : values) {
				if(!SpiritRights.isBlindAll(group.getStudy(), SpiritFrame.getUser())) {
					res.add(group);
				}
			}
			values = res;
		}
		super.setValues(values);

	}

	@Override
	public Component processCellRenderer(JLabel comp, String object, int index) {
		Group group = getMap().get(object);
		if(group==null) {
			return comp;
		} else {
			groupLabel.setGroup(group);
			groupLabel.setForeground(comp.getForeground());
			groupLabel.setBackground(comp.getBackground());
			groupLabel.setBorder(comp.getBorder());
			return groupLabel;
		}
	}

	@Override
	public String convertObjectToString(Group obj) {
		return obj==null?"": obj.getName();
	}

}
