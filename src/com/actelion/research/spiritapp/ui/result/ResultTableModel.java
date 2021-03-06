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

package com.actelion.research.spiritapp.ui.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.result.column.CommentsColumn;
import com.actelion.research.spiritapp.ui.result.column.ContainerColumn;
import com.actelion.research.spiritapp.ui.result.column.CreationColumn;
import com.actelion.research.spiritapp.ui.result.column.ElbColumn;
import com.actelion.research.spiritapp.ui.result.column.MetadataColumn;
import com.actelion.research.spiritapp.ui.result.column.PhaseColumn;
import com.actelion.research.spiritapp.ui.result.column.QualityColumn;
import com.actelion.research.spiritapp.ui.result.column.SampleIdColumn;
import com.actelion.research.spiritapp.ui.result.column.StudyGroupColumn;
import com.actelion.research.spiritapp.ui.result.column.StudyIdColumn;
import com.actelion.research.spiritapp.ui.result.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.ui.result.column.TestNameColumn;
import com.actelion.research.spiritapp.ui.result.column.TopIdColumn;
import com.actelion.research.spiritapp.ui.util.component.DocumentLabel;
import com.actelion.research.spiritapp.ui.util.component.DocumentTextField;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.JMapLabelNoRepaint;

public class ResultTableModel extends ExtendTableModel<Result> {


	/**
	 * Create Column to display all values from the givnen type,
	 * documents are excluded
	 * @param type
	 * @return
	 */
	public Column<Result, String> createValueColumn(final OutputType type) {


		return new Column<Result, String>((type==OutputType.OUTPUT? "Output": type==OutputType.INPUT? "Input": "Info"), String.class) {

			private JMapLabelNoRepaint lbl = new JMapLabelNoRepaint();

			@Override
			public float getSortingKey() {
				return 6f + type.ordinal();
			}

			@Override
			public String getValue(Result row) {
				return MiscUtils.flatten(row.getResultValuesAsMap(type));
			}

			@Override
			public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				lbl.setMap(row.getResultValuesAsMap(type));
				return lbl;
			}

			@Override
			public void postProcess(AbstractExtendTable<Result> table, Result row, int rowNo, Object value, JComponent comp) {
				if(row.getQuality()!=null && row.getQuality().getBackground()!=null) {
					comp.setBackground(row.getQuality().getBackground());
				}
			}
			@Override
			public boolean isMultiline() {
				return true;
			}
			@Override
			public boolean shouldMerge(Result r1, Result r2) {
				return false;
			}
		};
	}

	/**
	 * Creates a clickable column
	 * @param index
	 * @return
	 */
	public Column<Result, Document> createDocColumn(final int index) {

		return new Column<Result, Document>("Document " + (index+1), Document.class, 30) {
			private DocumentLabel lbl = new DocumentLabel();

			@Override
			public float getSortingKey() {
				return 9f;
			}

			@Override
			public Document getValue(Result row) {
				Set<TestAttribute> atts = row.getTest().getAttributes();
				int n = 0;
				for (TestAttribute ta : atts) {
					if(ta.getDataType()==DataType.FILES || ta.getDataType()==DataType.D_FILE) {
						if(n==index) {
							return row.getResultValue(ta)==null? null: row.getResultValue(ta).getLinkedDocument();
						}
						n++;
					}
				}
				return null;
			}

			@Override
			public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				lbl.setSelectedDocument((Document)value);
				return lbl;
			}

			@Override
			public boolean mouseDoubleClicked(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				Document doc = getValue(row);

				if(doc!=null) {
					DocumentTextField.open(doc);
					return true;
				}
				return false;
			}

			@Override
			public boolean isMultiline() {
				return true;
			}
			@Override
			public boolean shouldMerge(Result r1, Result r2) {
				return false;
			}
		};
	}


	private List<Column<Result, ?>> inputOutputColumns = new ArrayList<>();
	private final boolean compact;


	public ResultTableModel(boolean compact) {
		this.compact = compact;
		initColumns();
	}


	protected void initColumns() {

		Set<Biosample> biosamples = Result.getBiosamples(getRows());
		Set<Biosample> topSamples = Biosample.getTopParentsInSameStudy(biosamples);
		boolean sameTops = biosamples.equals(topSamples);


		List<Column<Result, ?>> columns = new ArrayList<>();
		columns.add(COLUMN_ROWNO);
		if(!compact) columns.add(new ElbColumn());
		if(!compact) columns.add(new ContainerColumn().setHideable(true));
		if(!compact) columns.add(new StudyIdColumn());
		if(!compact) columns.add(new StudyGroupColumn());
		if(!compact) columns.add(new StudySubGroupColumn().setHideable(true));
		columns.add(new PhaseColumn());
		columns.add(new TopIdColumn());
		columns.add(new SampleIdColumn().setHideable(sameTops));
		if(!compact) columns.add(new MetadataColumn().setHideable(false));
		columns.add(new TestNameColumn());

		columns.addAll(createInputOutputColumns());

		columns.add(new CommentsColumn());
		columns.add(new QualityColumn().setHideable(true));
		columns.add(new CreationColumn(true));
		columns.add(new CreationColumn(false).setHideable(true));

		setColumns(columns);
		showAllHideable(false);
	}

	public List<Column<Result, ?>> createInputOutputColumns() {
		int maxInput = 0;
		int maxOutput = 0;
		int maxInfos = 0;
		int maxDocs = 0;
		Set<Test> tests = Result.getTests(getRows());
		for (Test t : tests) {
			maxInput = Math.max(maxInput, t.getAttributes(OutputType.INPUT).size());
			maxOutput = Math.max(maxOutput, t.getAttributes(OutputType.OUTPUT).size());
			maxInfos = Math.max(maxInfos, t.getAttributes(OutputType.INFO).size());
			maxDocs = Math.max(maxDocs, (int) t.getAttributes().stream().filter(p->p.getDataType()==DataType.D_FILE || p.getDataType()==DataType.FILES).count());
		}

		inputOutputColumns.clear();
		if(maxInput>0) {
			inputOutputColumns.add(createValueColumn(OutputType.INPUT));
		}
		if(maxOutput>0) {
			inputOutputColumns.add(createValueColumn(OutputType.OUTPUT));
		}
		if(maxInfos>0) {
			inputOutputColumns.add(createValueColumn(OutputType.INFO));
		}
		for (int i = 0; i < maxDocs; i++) {
			inputOutputColumns.add(createDocColumn(i));
		}

		return inputOutputColumns;
	}



}
