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

package com.actelion.research.spiritapp.animalcare.ui.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.ObservationConstants;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;


public class MonitoringAnimalPanel extends JPanel {

	private final MonitoringDlg dlg;
	private Biosample animal;
	private Phase phase;

	private JPanel treatmentPanel;
	private final Result weighResult;
	private final Result obsResult;
	private final NamedTreatment nt;
	private final JCustomLabel weightIncLabel = new JCustomLabel(FastFont.SMALL);

	private final JLabel calculated1Label = new JCustomLabel("", Color.BLUE);
	private final JLabel calculated2Label = new JCustomLabel("", Color.BLUE);
	private final MonitorTextComboBox obsTextField;
	private final MonitorTextField weighTextField;
	private final JLabel formulationCommentsLabel = new JCustomLabel("", Color.BLUE);
	private final JCustomTextField foodTextField = new JCustomTextField(JCustomTextField.DOUBLE);
	private final JCustomTextField waterTextField = new JCustomTextField(JCustomTextField.DOUBLE);
	private final List<JTextComponent> requiredComponents = new ArrayList<>();

	public MonitoringAnimalPanel(final MonitoringDlg dlg, int no, final Biosample animal, final Phase phase, boolean onlyShowRequired) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.animal = animal;
		this.phase = phase;
		this.bio2history = mapTreatments(Collections.singleton(animal));

		//Extract Data
		StudyAction a = animal.getStudyAction(phase);
		nt = a==null? null: a.getNamedTreatment();


		//Weight Field
		JCustomLabel lastWeightLabel = new JCustomLabel(FastFont.SMALL);
		{
			weighResult = animal.getAuxResult(DAOTest.getTest(DAOTest.WEIGHING_TESTNAME), phase);

			Result prevWeighResult = weighResult==null? null: SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getPrevious(weighResult, dlg.getAllPreviousResults());
			lastWeightLabel.setPreferredSize(new Dimension(80, 22));
			lastWeightLabel.setForeground(Color.DARK_GRAY);
			lastWeightLabel.setText(prevWeighResult==null?"": (prevWeighResult.getPhase().getShortName()+ ": " + (prevWeighResult.getFirstAsDouble()==null?"NA": prevWeighResult.getFirstAsDouble()) + "g"));

			//Init field
			boolean required = (a!=null && a.isMeasureWeight()) || (a!=null && a.getNamedTreatment()!=null);
			weighTextField = new MonitorTextField(weighResult, 0, required);
			if(required) requiredComponents.add(weighTextField);

			weighTextField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(nt==null && weighTextField.getText().length()==0) return;
					openTreatmentDialog();
				}
			});
			try {
				updateTreatmentPanel();
			} catch(Exception e) {
				JExceptionDialog.showError(e); //should never happen here
			}
		}


		List<Component> measurementComps = new ArrayList<>();
		//Observation Field
		JCustomLabel lastObsLabel = new JCustomLabel(FastFont.SMALL);
		{
			obsResult = animal.getAuxResult(DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME), phase);
			assert obsResult!=null;

			//Find previousObservation
			Result prevObsResult = SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getPrevious(obsResult, dlg.getAllPreviousResults());
			lastObsLabel.setForeground(Color.DARK_GRAY);
			lastObsLabel.setText( prevObsResult==null?"": (prevObsResult.getPhase().getShortName()+ ": " + prevObsResult.getFirstValue()));

			//Init field
			obsTextField = new MonitorTextComboBox(animal.getAuxResult(DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME), phase), 0, false);
			obsTextField.setChoices(Arrays.asList(ObservationConstants.ALL_OBSERVATIONS));
			obsTextField.setColumns(9);
			measurementComps.add(new JLabel("Observation: "));
			measurementComps.add(UIUtils.createHorizontalBox(obsTextField, lastObsLabel));
		}


		//Other Measurements
		final List<MonitorTextField> formulaTextFields = new ArrayList<>();
		for (Measurement em : phase.getStudy().getAllMeasurementsFromActions()) {
			assert em.getTest()!=null;
			final Result result = animal.getAuxResult(em.getTest(), phase, em.getParameters());
			//			assert result!=null;


			List<TestAttribute> tas = em.getTest().getOutputAttributes();
			for (int i = 0; i < tas.size(); i++) {
				TestAttribute ta = tas.get(i);
				boolean required = a!=null && a.getMeasurements().contains(em);



				if(ta.getDataType()==DataType.NUMBER || ta.getDataType()==DataType.ALPHA || ta.getDataType()==DataType.AUTO || ta.getDataType()==DataType.FORMULA) {
					//Find previousValue
					JLabel lastMeasurementLabel = new JCustomLabel(FastFont.SMALL);
					lastMeasurementLabel.setPreferredSize(new Dimension(80, 22));
					lastMeasurementLabel.setForeground(Color.DARK_GRAY);
					Result prevResult = SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getPrevious(result, dlg.getAllPreviousResults());
					lastMeasurementLabel.setText( prevResult==null || prevResult.getResultValue(ta)==null? "": (prevResult.getPhase().getShortName()+ ": " + prevResult.getResultValue(ta).getValue()));

					MonitorTextField tf = new MonitorTextField(result, i, required);
					measurementComps.add(new JLabel(em.getDescription() + (tas.size()>1?"."+tas.get(i).getName():"")));
					measurementComps.add(UIUtils.createHorizontalBox(tf, lastMeasurementLabel));
					if(ta.getDataType()==DataType.FORMULA) {
						formulaTextFields.add(tf);
					} else {
						if(required) requiredComponents.add(tf);
						tf.addFocusListener(new FocusAdapter() {
							@Override
							public void focusLost(FocusEvent e) {
								if(formulaTextFields.size()==0) return;
								DAOResult.computeFormula(Collections.singletonList(result));
								for(MonitorTextField ftf: formulaTextFields) {
									ftf.refreshText();
								}
							}
						});
					}
				}
			}
		}
		JPanel measurementPanel = UIUtils.createTable(measurementComps.toArray(new Component[0]));


		//Formulation Fields
		final ActionTreatment actionTreatment = getTreatment(animal, phase);
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openTreatmentDialog();
			}
		};
		formulationCommentsLabel.addMouseListener(ma);
		updateFormulationLabel(actionTreatment);

		//FoodWater
		foodTextField.setBorderColor(Color.GRAY);
		waterTextField.setBorderColor(Color.GRAY);
		foodTextField.setEnabled(false);
		waterTextField.setEnabled(false);
		Result fwResult = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), phase);
		String valFood = fwResult==null? "": fwResult.getOutputResultValues().get(0).getValue();
		String valWater = fwResult==null? "": fwResult.getOutputResultValues().get(1).getValue();
		foodTextField.setText(valFood);
		waterTextField.setText(valWater);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//Treatment
		JCustomLabel treatmentLabel = new JCustomLabel(nt!=null?nt.getName(): "NoTreatment", FastFont.BOLD, nt==null?Color.BLACK: nt.getColor());
		JComponent commentsPanel = new JLabel();
		JPanel compoundsPanel = null;
		if(nt!=null) {

			commentsPanel = UIUtils.createHorizontalBox(formulationCommentsLabel);
			compoundsPanel = UIUtils.createHorizontalBox(
					nt.getDose1()!=null? UIUtils.createHorizontalBox(new JCustomLabel(nt.getCompoundAndUnit1()+" ", FastFont.BOLD), calculated1Label):new JLabel(),
							nt.getDose2()!=null? UIUtils.createHorizontalBox(new JCustomLabel(nt.getCompoundAndUnit2()+" ", FastFont.BOLD), calculated2Label) :new JLabel());
		}
		treatmentPanel = UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(treatmentLabel, Box.createHorizontalStrut(15), commentsPanel, Box.createHorizontalGlue()),
				compoundsPanel!=null? UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), compoundsPanel, Box.createVerticalGlue()): new JLabel()
				);
		treatmentPanel.addMouseListener(ma);
		treatmentPanel.setOpaque(true);
		treatmentPanel.setBackground(nt==null? Color.LIGHT_GRAY: new Color(235, 235, 230));
		treatmentPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		treatmentPanel.setMinimumSize(new Dimension(300, 40));

		weightIncLabel.setPreferredSize(new Dimension(80, 22));

		JPanel foodWaterPanel = new JPanel(new GridBagLayout());
		boolean foodWaterRequired = a!=null && (a.isMeasureFood() || a.isMeasureWater());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.anchor = GridBagConstraints.WEST;
		if(!onlyShowRequired || (a!=null && a.isMeasureFood())) {
			c2.gridx=0; c2.gridy=0; foodWaterPanel.add(new JCustomLabel("Food: ", FastFont.REGULAR), c2);
			c2.gridx=1; c2.gridy=0; foodWaterPanel.add(UIUtils.createHorizontalBox(foodTextField, new JLabel("g/d/a ")), c2);
		}
		if(!onlyShowRequired || (a!=null && a.isMeasureWater())) {
			c2.gridx=2; c2.gridy=0; foodWaterPanel.add(new JCustomLabel("Water: ", FastFont.REGULAR), c2);
			c2.gridx=3; c2.gridy=0; foodWaterPanel.add(UIUtils.createHorizontalBox(waterTextField, new JLabel("ml/d/a ")), c2);
		}

		////////////////////////////////////////////////////////////////////////////////
		//centerPanel
		obsTextField.setColumns(18);
		JPanel centerPanel = UIUtils.createVerticalBox(BorderFactory.createEmptyBorder(1, 1, 5, 1),
				UIUtils.createHorizontalBox(new JLabel("Weight [g]:"), weighTextField, lastWeightLabel, weightIncLabel, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(foodWaterPanel, Box.createHorizontalGlue()),
				Box.createVerticalGlue(),
				UIUtils.createHorizontalBox(treatmentPanel, Box.createHorizontalGlue()));
		foodWaterPanel.setVisible(!onlyShowRequired || foodWaterRequired);
		treatmentPanel.setVisible((!onlyShowRequired || nt!=null) && !SpiritRights.isBlindAll(phase.getStudy(), SpiritFrame.getUser()));


		//TopPanel
		JPanel animalPanel = createAnimalPanel(no+1, animal, phase);


		//Layout
		add(BorderLayout.CENTER, UIUtils.createHorizontalBox(BorderFactory.createLoweredSoftBevelBorder(),
				UIUtils.createBox(centerPanel, animalPanel),
				measurementPanel,
				Box.createHorizontalGlue()));
	}

	private final Map<Biosample, Map<String, ActionTreatment>> bio2history;


	public static Map<Biosample, Map<String, ActionTreatment>> mapTreatments(Collection<Biosample> animals) {
		Map<Biosample, Map<String, ActionTreatment>> bio2history = new HashMap<>();
		for(Biosample b: animals) {
			bio2history.put(b, new HashMap<String, ActionTreatment>());
			List<Biosample> history = DAORevision.getHistory(b);
			for (Biosample b2 : history) {
				ActionTreatment t = null;
				if(b2.getLastAction() instanceof ActionTreatment) {
					t = (ActionTreatment) b2.getLastAction();
				}
				if(t!=null) {
					//Add the treatment, only if it is the most recent (ie, the first)
					String key = t.getPhaseName() + "_" + t.getTreatmentName();
					if(!bio2history.get(b).containsKey(key)) {
						bio2history.get(b).put(key, t);
					}
				}
			}
		}
		return bio2history;
	}

	public ActionTreatment getTreatment(Biosample animal, Phase phase) {
		StudyAction a = phase.getStudy().getStudyAction(phase, animal);
		if(a==null) return null;
		NamedTreatment nt = a.getNamedTreatment();
		ActionTreatment treatment = bio2history.get(animal)==null? null: bio2history.get(animal).get(phase.getShortName() + "_" + (nt==null?"":nt.getName()));
		return treatment;
	}

	public static JPanel createAnimalPanel(int no, Biosample animal, Phase phase) {

		SampleIdLabel sampleIdLabel = new SampleIdLabel(false, false);
		sampleIdLabel.setHighlight(true);
		sampleIdLabel.setSizeIncrement(2);
		sampleIdLabel.setBiosample(animal);

		JPanel animalPanel = UIUtils.createHorizontalBox(
				new JLabel(no+". "),
				Box.createHorizontalStrut(5),
				animal.getContainerId()==null? new JLabel(): new ContainerLabel(ContainerDisplayMode.CONTAINERID, animal.getContainer()),
						Box.createHorizontalStrut(10),
						sampleIdLabel,
						Box.createHorizontalStrut(5),
						new JCustomLabel(animal.getSampleName()==null? "": "[" + animal.getSampleName() + "]", FastFont.BIGGER),
						Box.createHorizontalStrut(10),
						new JCustomLabel(animal.getInheritedGroupString(SpiritFrame.getUser().getUsername()), FastFont.REGULAR),
						Box.createHorizontalStrut(10),
						new JCustomLabel(phase==null?"": phase.getName(), FastFont.REGULAR),
						Box.createHorizontalGlue());
		animalPanel.setPreferredSize(new Dimension(500, 24));
		if(animal.getInheritedGroup()!=null) {
			animalPanel.setOpaque(true);
			animalPanel.setBackground(UIUtils.getDilutedColor(animal.getInheritedGroup().getBlindedColor(SpiritFrame.getUser().getUsername()), animalPanel.getBackground()));
		}
		animalPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.LIGHT_GRAY));
		return animalPanel;
	}

	/**
	 * Update the dose, and the weight
	 * return true, if the result is coherent
	 * @param addAction
	 * @throws Exception
	 */
	private void updateTreatmentPanel() {
		//Calculate dose (always after entering weight)
		Double dose1, dose2;
		StudyAction a = animal.getStudyAction(phase);
		NamedTreatment nt = a==null? null: a.getNamedTreatment();
		ActionTreatment action = getTreatment(animal, phase);
		Result weighResult = animal.getAuxResult(DAOTest.getTest(DAOTest.WEIGHING_TESTNAME), phase);
		Result prevWeighResult = SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getPrevious(weighResult, dlg.getAllPreviousResults());
		Double weight = weighResult.getFirstAsDouble();


		if(nt==null) {
			dose1 = null;
			dose2 = null;
			calculated2Label.setText("NA");
		} else {
			dose1 = nt.getCalculatedDose1(weight);
			dose2 = nt.getCalculatedDose2(weight);
		}
		if(nt==null) {
			calculated1Label.setText("");
		} else if(dose1==null) {
			calculated1Label.setText("NA");
		} else {
			String unit1 = nt.getUnit1()==null?"" : nt.getUnit1().getNumerator();
			String eff = "";
			if(action!=null && action.getEff1()!=null && action.getEff1().length()>0) {
				eff = " (Eff. "+action.getEff1()+unit1+")";
			}
			calculated1Label.setText("<html><b>" + FormatterUtils.format3(dose1) + "</b>" + unit1 + eff);
		}
		if(nt==null) {
			calculated2Label.setText("");
		} else if(dose2==null) {
			calculated2Label.setText("NA");
		} else {
			String unit2 = nt.getUnit2()==null?"" : nt.getUnit2().getNumerator();
			String eff = "";
			if(action!=null && action.getEff2()!=null && action.getEff2().length()>0) {
				eff = " (Eff. "+action.getEff2()+unit2+")";
			}

			calculated2Label.setText("<html><b>" + FormatterUtils.format3(dose2) + "</b>" + unit2 + eff);
		}

		updateWeightIncreaseLabel(prevWeighResult==null? null: prevWeighResult.getOutputResultValues().get(0).getDoubleValue(), weight, weightIncLabel, prevWeighResult==null?"": "since " +prevWeighResult.getPhase().getShortName());
	}

	private Double updateWeightIncreaseLabel(Double prevWeight, Double curWeight, JLabel weighIncreaseLabel, String suffix) {

		if(prevWeight==null || prevWeight<=0) {
			weighIncreaseLabel.setText("");
			return null;
		} else if(curWeight==null) {
			weighIncreaseLabel.setText("");
			return null;
		} else {
			double percent = 100*(curWeight - prevWeight)/prevWeight;
			weighIncreaseLabel.setText("<html><span style='white-space:nowrap'>" + (percent<0?"<span style='color:red'>":"<span style='color:green'>+") + FormatterUtils.format1(percent) + "%</span>"
					+ " " + suffix
					+ "</html>");
			return percent;
		}
	}

	public void updateFW() {

		Result fwResult = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), phase);

		String valFood = fwResult==null? null: fwResult.getOutputResultValues().get(0).getValue();
		String valWater = fwResult==null? null: fwResult.getOutputResultValues().get(1).getValue();

		if(valFood==null) valFood = "";
		if(valWater==null) valWater = "";

		if(!foodTextField.getText().equals(valFood)) {
			foodTextField.setBorderColor(Color.BLUE);
			foodTextField.setText(valFood);
		}
		if(!waterTextField.getText().equals(valWater)) {
			waterTextField.setBorderColor(Color.BLUE);
			waterTextField.setText(valWater);
		}
	}

	public Biosample getAnimal() {
		return animal;
	}

	public List<JTextComponent> getRequiredComponents() {
		return requiredComponents;
	}


	private void openTreatmentDialog() {
		if(SpiritRights.isBlindAll(phase.getStudy(), SpiritFrame.getUser())) return;
		final ActionTreatment actionTreatment = getTreatment(animal, phase);
		try {

			String presetFormulation = actionTreatment==null || actionTreatment.getFormulation()==null? dlg.getFormulation(nt): actionTreatment.getFormulation();
			String presetComments = actionTreatment==null? "": actionTreatment.getComments();
			final JCustomTextField popFormulationTextField = new JCustomTextField(14, presetFormulation);
			final JCustomTextField popCommentsTextField = new JCustomTextField(14, presetComments);

			if(popFormulationTextField.getText().length()==0) {
				popFormulationTextField.setBackground(LF.BGCOLOR_REQUIRED);
			}

			final JCustomTextField effDose1TextField = new JCustomTextField(JCustomTextField.DOUBLE, 8);
			effDose1TextField.setWarningWhenEdited(true);
			final JCustomTextField effDose2TextField = new JCustomTextField(JCustomTextField.DOUBLE, 8);
			effDose2TextField.setWarningWhenEdited(true);

			if(actionTreatment!=null) {
				effDose1TextField.setText(actionTreatment.getEff1());
				effDose2TextField.setText(actionTreatment.getEff2());
			}

			//Get previous weights
			Double weight = weighTextField.getTextDouble();
			Result prevResult = SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getPrevious(weighResult, dlg.getAllPreviousResults());
			Result firstResult = SpiritRights.isBlind(phase.getStudy(), SpiritFrame.getUser())? null: Result.getFirst(weighResult, dlg.getAllPreviousResults());

			JLabel weightIncreaseLabelFromPrevious = new JLabel();
			JLabel weightIncreaseLabelFromStart = new JLabel();
			Double inc = updateWeightIncreaseLabel(prevResult==null? null: prevResult.getFirstAsDouble(),  weight, weightIncreaseLabelFromPrevious, prevResult==null? "": "since "+prevResult.getPhase().getShortName());
			if(inc!=null && Math.abs(inc)>10) {
				weightIncreaseLabelFromPrevious.setOpaque(true);
				weightIncreaseLabelFromPrevious.setBackground(UIUtils.getDilutedColor(Color.RED, Color.WHITE));
			}
			if(firstResult!=prevResult) {
				inc = updateWeightIncreaseLabel(firstResult==null? null: firstResult.getFirstAsDouble(),  weight, weightIncreaseLabelFromStart, firstResult==null? "": "since "+firstResult.getPhase().getShortName());
				if(inc!=null && inc<-20) {
					weightIncreaseLabelFromStart.setOpaque(true);
					weightIncreaseLabelFromStart.setBackground(UIUtils.getDilutedColor(Color.RED, Color.WHITE));
				}
			}


			//Get doses
			Double d1 = nt==null? null: nt.getCalculatedDose1(weight);
			Double d2 = nt==null? null: nt.getCalculatedDose2(weight);

			SampleIdLabel sampleIdLabel = new SampleIdLabel(animal, true, true);
			sampleIdLabel.setOpaque(false);
			sampleIdLabel.setHighlight(true);
			JPanel box = UIUtils.createVerticalBox(
					UIUtils.createTitleBox(UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(sampleIdLabel, Box.createHorizontalStrut(20), new ContainerLabel(ContainerDisplayMode.CONTAINERID, animal.getContainer()),  Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(new JCustomLabel(weight==null?"NA": weight+"g", FastFont.BOLD), Box.createHorizontalStrut(15), weightIncreaseLabelFromPrevious, Box.createHorizontalStrut(15), weightIncreaseLabelFromStart, Box.createHorizontalGlue()))),
					nt==null? new JLabel(): UIUtils.createTitleBox(UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(new JCustomLabel(nt.getName(), FastFont.BOLD, nt.getColor()), Box.createHorizontalGlue()),
							UIUtils.createTable(2, 5, 0,
									new JLabel("Formulation: "), popFormulationTextField,
									new JLabel("Comments:   "), popCommentsTextField),
							UIUtils.createTable(3, 5, 0,
									new JCustomLabel(nt.getCompoundAndUnit1()), nt.getDose1()==null? new JLabel(): new JCustomLabel(d1==null?"NA":""+d1+(nt.getUnit1()==null?"": nt.getUnit1().getNumerator()), Color.BLUE), nt.getDose1()==null? new JLabel(): UIUtils.createHorizontalBox(new JLabel("Effective: "), effDose1TextField),
											new JCustomLabel(nt.getCompoundAndUnit2()), nt.getDose2()==null? new JLabel(): new JCustomLabel(d2==null?"NA":""+d2+(nt.getUnit2()==null?"": nt.getUnit2().getNumerator()), Color.BLUE)), nt.getDose2()==null? new JLabel(): UIUtils.createHorizontalBox(new JLabel("Effective: "), effDose2TextField))
							)
					);


			JOptionPane.showMessageDialog(MonitoringAnimalPanel.this, box, "Treatment", JOptionPane.PLAIN_MESSAGE);
			{
				JPAUtil.pushEditableContext(SpiritFrame.getUser());
				try {
					//Persist the treatment action
					ActionTreatment at = new ActionTreatment(nt, phase, weight, effDose1TextField.getTextDouble(), effDose2TextField.getTextDouble(), popFormulationTextField.getText(), popCommentsTextField.getText());
					animal.setLastAction(at);
					DAOBiosample.persistBiosamples(Collections.singleton(animal), SpiritFrame.getUser());

					//Show that this is recorded
					treatmentPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLUE), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
					updateTreatmentPanel();
					updateFormulationLabel(at);
					dlg.setFormulation(nt, popFormulationTextField.getText());
				} catch(Exception e) {
					treatmentPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
					JExceptionDialog.showError(e);
				} finally {
					JPAUtil.popEditableContext();
				}
			}

		} catch(Exception e) {
			JExceptionDialog.showError(e);

		}
	}

	private void updateFormulationLabel(ActionTreatment actionTreatment) {
		if(actionTreatment==null) {
			formulationCommentsLabel.setText("");
		} else {
			formulationCommentsLabel.setText((actionTreatment.getFormulation()==null || actionTreatment.getFormulation().length()==0?"": actionTreatment.getFormulation() + (actionTreatment.getComments()==null || actionTreatment.getComments().length()==0? " ": " / ")) + (actionTreatment.getComments()==null?"": actionTreatment.getComments()));
		}
	}

}
