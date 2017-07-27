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

package com.actelion.research.spiritapp.spirit.ui.biosample.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.LongTaskDlg;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleDiscardDlg {

	//	public static void createDialogForSynchro(List<Biosample> biosamples) throws Exception {
	//		final SpiritUser user = Spirit.askForAuthentication();
	//
	//		try {
	//			JPAUtil.pushEditableContext();
	//			biosamples = JPAUtil.reload(biosamples);
	//
	//			//Analyze samples
	//			final List<Biosample> toFix = new ArrayList<Biosample>();
	//			for(Biosample b: biosamples) {
	//				Biosample top = b;
	//				int count = 0;
	//				while(top.getParent()!=null && !top.getParent().equals(top)) {
	//					if(count++>10) break;
	//					top = top.getParent();
	//				}
	//				if(!b.getTopParent().equals(top)) toFix.add(b);
	//				else if(b.getParent()!=null && b.getParent().getInheritedGroup()!=null && !b.getParent().getInheritedGroup().equals(b.getInheritedGroup())) toFix.add(b);
	//				else if(b.getParent()!=null && b.getParent().getInheritedSubGroup()!=b.getInheritedSubGroup()) toFix.add(b);
	//			}
	//
	//			if(toFix.size()==0) throw new Exception("Those samples are already synchronized");
	//
	//			//Show dlg
	//			BiosampleTable table = new BiosampleTable();
	//			table.getModel().setCanExpand(false);
	//			table.getModel().setCompressed(true);
	//			table.setRows(toFix);
	//			JScrollPane sp = new JScrollPane(table);
	//			sp.setPreferredSize(new Dimension(700, 400));
	//
	//			JPanel msgPanel = new JPanel(new BorderLayout());
	//			msgPanel.add(BorderLayout.NORTH, new JCustomLabel("Are you sure you want to synchronize (inherited group/subgroup, topparent) of " + (biosamples.size()>1? "those " + biosamples.size() + " samples": " this biosample"), FastFont.BOLD));
	//			msgPanel.add(BorderLayout.CENTER, sp);
	//
	//			int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), msgPanel, "Synchronizes Biosamples", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Synchronize", "Cancel"}, "Cancel");
	//			if(res!=0) return;
	//
	//
	//			for(Biosample b: toFix) {
	//				Biosample top = b;
	//				int count = 0;
	//				while(top.getParent()!=null && !top.getParent().equals(top)) {
	//					if(count++>10) break;
	//					top = top.getParent();
	//				}
	//				b.setTopParent(top);
	//				if(b.getParent()!=null && b.getParent().getInheritedGroup()!=null) {
	//					b.setInheritedGroup(b.getParent().getInheritedGroup());
	//					b.setInheritedSubGroup(b.getParent().getInheritedSubGroup());
	//				}
	//			}
	//			new LongTaskDlg("Deleting Biosamples") {
	//				@Override
	//				public void longTask() throws Exception {
	//					DAOBiosample.persistBiosamples(toFix, user);
	//				}
	//			};
	//
	//		} finally {
	//			JPAUtil.popEditableContext();
	//		}
	//		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
	//
	//	}


	public static void createDialogForDelete(List<Biosample> bios) throws Exception {
		//this.biosamples = biosamples;

		final SpiritUser user = Spirit.askForAuthentication();

		final List<Biosample> biosamples = JPAUtil.reattach(bios);

		List<Biosample> toAdd = new ArrayList<>();
		for (Biosample biosample : biosamples) {
			if(!SpiritRights.canDelete(biosample, user)) throw new Exception("You cannot delete "+biosample);
			if(biosample.getChildren().size()>0) {
				Set<Biosample> children = biosample.getHierarchy(HierarchyMode.CHILDREN);
				children.removeAll(bios);
				toAdd.addAll(children);
			}
		}



		JPanel msgPanel = new JPanel(new BorderLayout());
		if(toAdd.size()>0) {
			biosamples.addAll(toAdd);
			msgPanel.add(BorderLayout.NORTH, new JLabel("<html><div style='color:red'>You can only delete those samples if you also delete their children.<br> Do you want to DEFINITELY delete " + (biosamples.size()>1? "those " + biosamples.size() + " samples?": " this sample?") + "</div>"));
		} else {
			msgPanel.add(BorderLayout.NORTH, new JLabel("Are you sure you want to DEFINITELY delete " + (biosamples.size()>1? "those " + biosamples.size() + " samples?": " this sample?")));
		}

		BiosampleTable table = new BiosampleTable();
		table.getModel().setCanExpand(false);
		table.getModel().setMode(Mode.COMPACT);
		table.setRows(biosamples);
		table.setSelection(toAdd);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(750, 400));

		msgPanel.add(BorderLayout.CENTER, sp);
		if(toAdd.size()>0) msgPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

		int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), msgPanel, "DELETE Biosamples", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Delete Selection" + (toAdd.size()>0? " and Children":""), "Cancel"}, "Cancel");
		if(res!=0) return;

		//Check for children??
		for (Biosample biosample : biosamples) {
			if(biosample==null) throw new Exception("One biosample was alread deleted");
			if(!biosamples.containsAll(biosample.getHierarchy(HierarchyMode.CHILDREN))) {
				throw new Exception("You must first delete the children");
			}
		}


		//Check for existing results
		final List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForBiosampleIds(JPAUtil.getIds(biosamples)), null);
		if(results.size()>0) {
			res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "There are " +results.size()+ " results associated to those samples. Do you want to delete them?", "DELETE Biosamples", JOptionPane.YES_NO_OPTION);
			if(res!=JOptionPane.YES_OPTION) return;
		}

		new LongTaskDlg("Deleting Biosamples") {
			@Override
			public void longTask() throws Exception {
				EntityManager session = JPAUtil.getManager();
				EntityTransaction txn = null;

				try {
					JPAUtil.pushEditableContext(SpiritFrame.getUser());
					txn = session.getTransaction();
					txn.begin();

					DAOResult.deleteResults(session, results, user);
					DAOBiosample.deleteBiosamples(session, biosamples, user);

					txn.commit();
					txn = null;
				} catch (Exception e) {
					if (txn != null)try {txn.rollback();} catch (Exception e2) {}
					throw e;
				} finally {
					JPAUtil.popEditableContext();
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Biosample.class, bios);
				}
			}
		};

	}

}
