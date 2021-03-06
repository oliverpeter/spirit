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

package com.actelion.research.spiritapp.ui.audit;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.actelion.research.spiritapp.ui.util.component.UserIdComboBox;
import com.actelion.research.spiritcore.business.audit.LogEntry;
import com.actelion.research.spiritcore.business.audit.LogEntry.Action;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable.BorderStrategy;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTable;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class LogEntryDlg extends JEscapeDialog {

	private UserIdComboBox userTextField = new UserIdComboBox();
	private JGenericComboBox<LogEntry.Action> actionComboBox = new JGenericComboBox<LogEntry.Action>(LogEntry.Action.values(), true);
	private JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(31, 0, null, 1));
	private ExtendTable<LogEntry> table;

	public LogEntryDlg() {
		super(UIUtils.getMainFrame(), "Log Entries");


		//Create table
		List<Column<LogEntry, ?>> columns = new ArrayList<>();
		columns.add(new Column<LogEntry, Date>("Date", Date.class, 140) {
			@Override
			public Date getValue(LogEntry row) {return row.getDate();}
			@Override
			public void postProcess(AbstractExtendTable<LogEntry> table, LogEntry row, int rowNo, Object value, JComponent comp) {
				((JLabelNoRepaint) comp).setText(FormatterUtils.formatDateTime((Date)value));
			}
		});
		columns.add(new Column<LogEntry, String>("User", String.class) {
			@Override
			public String getValue(LogEntry row) {return row.getUser();}
		});
		//		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
		//			columns.add(new Column<LogEntry, String>("Dept.", String.class) {
		//				@Override
		//				public String getValue(LogEntry row) {
		//					Employee emp = DAOEmployee.getEmployee(row.getUser());
		//					return emp.getEmployeeGroups()==null? null: MiscUtils.flatten(EmployeeGroup.getNames(emp.getEmployeeGroups()));
		//				}
		//			});
		//		}
		columns.add(new Column<LogEntry, String>("IP", String.class) {
			@Override
			public String getValue(LogEntry row) {return row.getIpAddress();}
		});
		columns.add(new Column<LogEntry, Action>("Action", Action.class) {
			@Override
			public Action getValue(LogEntry row) {return row.getAction();}
		});

		columns.add(new Column<LogEntry, String>("Rights", String.class) {
			@Override
			public String getValue(LogEntry row) {return row.getComments();}
		});

		table = new ExtendTable<>(columns);
		table.setBorderStrategy(BorderStrategy.ALL_BORDER);


		//events
		ActionListener filterAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filter();
			}
		};
		actionComboBox.setSelection(Action.LOGON_SUCCESS);
		JButton filterButton = new JButton("Filter");
		filterButton.addActionListener(filterAction);
		userTextField.addActionListener(filterAction);
		actionComboBox.addActionListener(filterAction);
		daysSpinner.setPreferredSize(new Dimension(60, 24));

		//Layout
		setContentPane(UIUtils.createBox(new JScrollPane(table),
				UIUtils.createTitleBox("Filters", UIUtils.createTable(
						new JLabel("Since (days): "), daysSpinner,
						new JLabel("User: "), userTextField,
						new JLabel("Action: "), actionComboBox,
						null, filterButton)),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction()))));
		UIUtils.adaptSize(this, 600, 640);
		filter();
		setVisible(true);
	}


	private void filter() {
		new SwingWorkerExtended("Querying", getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			List<LogEntry> logs;
			@Override
			protected void doInBackground() throws Exception {
				logs = DAOLog.getLogs(userTextField.getText(), actionComboBox.getSelection(), daysSpinner.getValue()==null?-1: (Integer) daysSpinner.getValue());
			}
			@Override
			protected void done() {
				table.setRows(logs);
			}
		};


	}
}
