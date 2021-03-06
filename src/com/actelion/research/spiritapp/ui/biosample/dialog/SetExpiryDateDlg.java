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

package com.actelion.research.spiritapp.ui.biosample.dialog;

import java.awt.BorderLayout;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class SetExpiryDateDlg extends JSpiritEscapeDialog {

	private List<Biosample> biosamples;

	private JCustomTextField expiryDateTextField = new JCustomTextField(CustomFieldType.DATE);


	public SetExpiryDateDlg(List<Biosample> mySamples) {
		super(UIUtils.getMainFrame(), "Set Expiry Date", SetExpiryDateDlg.class.getName());
		this.biosamples = JPAUtil.reattach(mySamples);

		JPanel centerPanel = new JPanel(new BorderLayout());
		JLabel label = new JCustomLabel("Please enter the expiry date of those samples", FastFont.BOLD);
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.add(BorderLayout.NORTH, label);

		JScrollPane sp;
		BiosampleTable table = new BiosampleTable();
		table.getModel().setCanExpand(false);
		//		table.getModel().setMode(Mode.COMPACT);
		table.setRows(biosamples);


		sp = new JScrollPane(table);
		centerPanel.add(BorderLayout.CENTER, sp);

		JButton okButton = new JIconButton(IconType.SAVE, "Set Expiry Date");
		okButton.addActionListener(e->{
			try {
				eventOk();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		Date minDate = null;
		for (Biosample biosample : biosamples) {
			if(biosample.getExpiryDate()!=null) {
				if(minDate == null || biosample.getExpiryDate().before(minDate)) {
					minDate = biosample.getExpiryDate();
				}
			}
		}

		expiryDateTextField.setTextDate(minDate);

		JPanel southPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(), UIUtils.createHorizontalBox(new JLabel("Expiry Date: "), expiryDateTextField, Box.createGlue()));
		southPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));


		setContentPane(UIUtils.createBox(centerPanel, null, southPanel));

		UIUtils.adaptSize(this, 900, 400);
		setVisible(true);

	}

	private void eventOk() throws Exception {
		if(!expiryDateTextField.isValidFormat()) throw new Exception("The date is not well formatted");
		Date expiryDate = expiryDateTextField.getTextDate();
		if(expiryDate==null) {
			int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the expiry date?", "Set Expiry Date", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.YES_OPTION) return;
		}

		for (Biosample biosample : biosamples) {
			biosample.setExpiryDate(expiryDate);
		}

		if(!Spirit.askReasonForChangeIfUpdated(biosamples)) return;
		DAOBiosample.persistBiosamples(biosamples, Spirit.askForAuthentication());
		dispose();
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
	}

}
