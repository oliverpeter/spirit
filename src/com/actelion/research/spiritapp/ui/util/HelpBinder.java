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

package com.actelion.research.spiritapp.ui.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.admin.BiotypeEditDlg;
import com.actelion.research.spiritapp.ui.admin.TestEditDlg;
import com.actelion.research.spiritapp.ui.admin.config.ConfigDlg;
import com.actelion.research.spiritapp.ui.admin.user.EmployeeEditDlg;
import com.actelion.research.spiritapp.ui.admin.user.EmployeeGroupEditDlg;
import com.actelion.research.spiritapp.ui.audit.StudyHistoryDlg;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.ui.biosample.edit.CreateChildrenDlg;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.ui.biosample.form.BiosampleFormDlg;
import com.actelion.research.spiritapp.ui.home.HomeTab;
import com.actelion.research.spiritapp.ui.location.CheckinDlg;
import com.actelion.research.spiritapp.ui.location.CheckoutDlg;
import com.actelion.research.spiritapp.ui.location.LocationTab;
import com.actelion.research.spiritapp.ui.location.edit.LocationEditDlg;
import com.actelion.research.spiritapp.ui.pivot.PivotAnalyzerDlg;
import com.actelion.research.spiritapp.ui.result.ResultTab;
import com.actelion.research.spiritapp.ui.result.edit.EditResultDlg;
import com.actelion.research.spiritapp.ui.result.edit.PivotDlg;
import com.actelion.research.spiritapp.ui.study.ManageSamplesDlg;
import com.actelion.research.spiritapp.ui.study.SetLivingStatusDlg;
import com.actelion.research.spiritapp.ui.study.StudyTab;
import com.actelion.research.spiritapp.ui.study.edit.AttachSamplesManuallyDlg;
import com.actelion.research.spiritapp.ui.study.monitor.MonitoringDlg;
import com.actelion.research.spiritapp.ui.study.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.ui.study.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.ui.study.sampleweighing.SampleWeighingDlg;
import com.actelion.research.spiritapp.ui.study.sampling.NamedSamplingDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyDesignDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyInfoDlg;
import com.actelion.research.spiritapp.ui.study.wizard.group.StudyGroupDlg;
import com.actelion.research.spiritapp.ui.study.wizard.phase.PhaseDlg;
import com.actelion.research.spiritapp.ui.study.wizard.treatment.StudyTreatmentDlg;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class HelpBinder {

	private static Map<Class<?>, String> class2Ref = null;


	static {
		if(class2Ref==null) {
			class2Ref = new HashMap<>();
			class2Ref.put(Spirit.class, null);
			class2Ref.put(HomeTab.class, "home");
			class2Ref.put(StudyTab.class, "study_visualization");
			class2Ref.put(BiosampleTab.class, "biosample_visualization");
			class2Ref.put(LocationTab.class, "location_visualization");
			class2Ref.put(ResultTab.class, "result_visualization");

			class2Ref.put(StudyInfoDlg.class, "study_infos");
			class2Ref.put(StudyDesignDlg.class, "study_design");
			class2Ref.put(PhaseDlg.class, "study_phase");
			class2Ref.put(StudyGroupDlg.class, "study_group");
			class2Ref.put(StudyTreatmentDlg.class, "study_treatment");
			class2Ref.put(NamedSamplingDlg.class, "study_sampling");
			class2Ref.put(RandomizationDlg.class, "study_auto");
			class2Ref.put(AttachSamplesManuallyDlg.class, "study_manual");
			class2Ref.put(MonitoringOverviewDlg.class, "study_overview");
			class2Ref.put(MonitoringDlg.class, "study_monitoring");
			class2Ref.put(SetLivingStatusDlg.class, "study_living");
			class2Ref.put(ManageSamplesDlg.class, "study_manage");
			class2Ref.put(SampleWeighingDlg.class, "study_measure");
			class2Ref.put(StudyHistoryDlg.class, "study_history");

			class2Ref.put(EditBiosampleDlg.class, "biosample_batch");
			class2Ref.put(BiosampleFormDlg.class, "biosample_form");
			class2Ref.put(CreateChildrenDlg.class, "biosample_children");
			class2Ref.put(CheckinDlg.class, "biosample_checkin");
			class2Ref.put(CheckoutDlg.class, "biosample_checkout");

			//these buttons need to be added
			class2Ref.put(LocationEditDlg.class, "location_edit");
			class2Ref.put(EditResultDlg.class, "result_edit");
			class2Ref.put(PivotDlg.class, "result_customize");


			class2Ref.put(PivotAnalyzerDlg.class, "result_analysis");

			class2Ref.put(EmployeeGroupEditDlg.class, "Edit Groups");
			class2Ref.put(EmployeeEditDlg.class, "admin_user");
			class2Ref.put(BiotypeEditDlg.class, "admin_user");
			class2Ref.put(TestEditDlg.class, "admin_tests");
			class2Ref.put(ConfigDlg.class, "Balance Configuration");
		}

	}

	public static JButton createHelpButton() {
		final JButton res = new JIconButton(IconType.HELP);
		res.addActionListener( e-> showHelp(res));
		if(! DBAdapter.isConfigurable()) res.setVisible(false);
		return res;
	}

	public static void bindHelp() {

		Toolkit.getDefaultToolkit ().addAWTEventListener (new AWTEventListener() {

			@Override
			public void eventDispatched(AWTEvent event) {
				if(event instanceof KeyEvent && ((KeyEvent)event).getKeyCode()==112 && ((KeyEvent)event).getID()==KeyEvent.KEY_PRESSED) {
					if(event.getSource()!=null && event.getSource() instanceof Component)  {
						showHelp((Component) event.getSource());
					}
				}
			}
		}, AWTEvent.KEY_EVENT_MASK);


	}

	public static void showHelp(Component source) {
		while(source!=null && source.getParent()!=null && !class2Ref.containsKey(source.getClass())) {
			source = source.getParent();
		}
		String ref = class2Ref.get(source==null? null: source.getClass());
		showHelp(ref);

	}

	public static void showHelp(String ref) {
		try {
			String url = "https://jfreyss.github.io/spirit/doc/index.html" + (ref==null? "": "#"+URLEncoder.encode(ref, "UTF-8"));
			Desktop.getDesktop().browse(new URI(url));
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
	}

}
