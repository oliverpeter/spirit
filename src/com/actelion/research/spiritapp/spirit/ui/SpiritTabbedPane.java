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

package com.actelion.research.spiritapp.spirit.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.ui.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.UIUtils;

/**
 * SpiritTabbedPane is the panel, showing all the Spirit perspectives.
 * In addition, it is possible to select a study for all the perspectives
 *
 * - The event onTabSelect is called on each tab change
 * - The event onStudySelect is called on each study change (and after a tab select if the study is not current)
 *
 * @author Joel Freyss
 *
 */
public class SpiritTabbedPane extends JComponent {
	public static String STUDY_ACTION = "study_action";
	private JCustomTabbedPane tabbedPane = new JCustomTabbedPane();
	private Dimension dim;
	private JPanel leadingPanel;

	private StudyComboBox studyComboBox = new StudyComboBox();

	public SpiritTabbedPane() {
		studyComboBox.setColumns(14);
		leadingPanel = UIUtils.createHorizontalBox(studyComboBox);
		leadingPanel.setOpaque(true);
		leadingPanel.setBackground(Color.WHITE);
		setBackground(Color.WHITE);
		initLayout();

		//Add events
		addChangeListener(e -> {
			if(tabbedPane!=null && tabbedPane.getSelectedComponent() instanceof SpiritTab) {
				SpiritTab tab = (SpiritTab)tabbedPane.getSelectedComponent();
				tab.onTabSelect();
				if(!tab.getSelectedStudyId().equals(getStudyId())) {
					((SpiritTab)tabbedPane.getSelectedComponent()).onStudySelect();
					tab.setSelectedStudyId(getStudyId());
				}
			}
		});
		getStudyComboBox().addTextChangeListener(l-> {
			if(tabbedPane!=null && tabbedPane.getSelectedComponent() instanceof SpiritTab) {
				SpiritTab tab = (SpiritTab)tabbedPane.getSelectedComponent();
				if(!tab.getSelectedStudyId().equals(getStudyId())) {
					((SpiritTab)tabbedPane.getSelectedComponent()).onStudySelect();
					tab.setSelectedStudyId(getStudyId());
				}
			}
		});
		getStudyComboBox().addActionListener(e-> {
			if(getStudyId().length()>0 && getSelectedComponent() instanceof IHomeTab) {
				Study s = DAOStudy.getStudyByStudyId(getStudyId());
				SpiritContextListener.setStudy(s);
			}
		});
	}

	public void initLayout() {
		dim = leadingPanel.getPreferredSize();
		tabbedPane.getUI().setLeadingOffset(leadingPanel.isVisible()? dim.width+5: 0);
		tabbedPane.getUI().setMinHeight(dim.height);
		setLayout(null);
		add(leadingPanel);
		add(tabbedPane);
		tabbedPane.setOpaque(false);
		setComponentZOrder(leadingPanel, 0);
		setComponentZOrder(tabbedPane, 1);
	}


	public void setStudyLevel(RightLevel level, boolean multipleChoices) {
		studyComboBox.setLevel(level);
		studyComboBox.setMultipleChoices(multipleChoices);
	}

	public StudyComboBox getStudyComboBox() {
		return studyComboBox;
	}

	@Override
	public void doLayout() {
		tabbedPane.setBounds(0, 2, getWidth(), getHeight()-2);
		leadingPanel.setBounds(0, 1, dim.width, dim.height);
	}

	public int getSelectedIndex() {
		return tabbedPane.getSelectedIndex();
	}

	public void setSelectedIndex(int index) {
		tabbedPane.setSelectedIndex(index);
	}

	public Component getSelectedComponent() {
		return tabbedPane.getSelectedComponent();
	}

	public void setSelectedComponent(Component component) {
		tabbedPane.setSelectedComponent(component);
	}

	public void addChangeListener(ChangeListener listener) {
		tabbedPane.addChangeListener(listener);
	}

	public void addTab(String title, Component component) {
		tabbedPane.addTab(title, component);
	}

	public void setIconAt(int index, Icon icon) {
		tabbedPane.setIconAt(index, icon);
	}

	public int getTabCount() {
		return tabbedPane.getTabCount();
	}

	@Override
	public void setFont(Font font) {
		tabbedPane.setFont(font);
	}

	public void setStudyId(String studyId) {
		studyComboBox.setText(studyId);
	}

	public String getStudyId() {
		return studyComboBox.getText();
	}

	public void setStudies(List<Study> studies) {
		studyComboBox.setValues(studies);
	}

	public void setStudyVisible(boolean v) {
		leadingPanel.setVisible(v);
		initLayout();
	}

	/**
	 * Gets the tab, which extends/implements the given claz
	 * Returns null, if there is no implemenation
	 * @param claz
	 * @return
	 */
	public SpiritTab getTab(Class<?> claz) {
		if(tabbedPane==null) return null;
		for(Component comp: tabbedPane.getComponents()) {
			if(claz.isInstance(comp) && comp instanceof SpiritTab) return (SpiritTab) comp;
		}
		return null;
	}

}
