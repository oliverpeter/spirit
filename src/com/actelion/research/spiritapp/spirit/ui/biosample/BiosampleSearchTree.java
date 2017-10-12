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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.location.LocationFormNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.CheckboxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.MultiNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.ObjectComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeToggleNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.CreDateNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.CreUserNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.DepartmentNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.GroupNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.PhaseNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.QualityComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.lf.StudyNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.UpdDateNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.UpdUserNode;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;

public class BiosampleSearchTree extends FormTree {


	private final BiosampleQuery query = new BiosampleQuery();
	private final LabelNode top = new LabelNode(this, "Query Biosamples:");
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();

	private final LabelNode stuNode = new LabelNode(this, "Study");
	private final LabelNode conNode = new LabelNode(this, "Container");
	private final LabelNode bioNode = new LabelNode(this, "Biosample");

	private final LabelNode moreNode = new LabelNode(this, "Filters");
	private final LabelNode locationNode = new LabelNode(this, "Location");
	private final LabelNode advancedNode = new LabelNode(this, "Advanced");
	private LabelNode catSelectOneNode = new LabelNode(this, "Select One Sample per TopParent");
	private CheckboxNode cb1, cb2;
	private Biotype[] selectableBiotypes;
	private SpiritFrame frame;


	private final StudyNode studyNode = new StudyNode(this, RightLevel.READ, true, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getStudyIds();
		}
		@Override
		public void setModel(String modelValue) {
			query.setStudyIds(modelValue);
		}
		@Override
		public void onAction() {
			updateModel();
			eventStudyChanged();
			initLayout();
			setFocus(studyNode);
		}
	});

	private final GroupNode groupNode = new GroupNode(this, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getGroup();
		}
		@Override
		public void setModel(String modelValue) {
			query.setGroup(modelValue);
		}
	});
	private final PhaseNode phaseNode = new PhaseNode(this, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getPhases();
		}
		@Override
		public void setModel(String modelValue) {
			query.setPhases(modelValue);
		}
	});
	private final InputNode quickSearchNode = new InputNode(this, FieldType.OR_CLAUSE, "SampleIds/ContainerIds", new Strategy<String>() {

		@Override
		public String getModel() {
			return query.getSampleIdOrContainerIds();
		}
		@Override
		public void setModel(String modelValue) {
			query.setSampleIdOrContainerIds(modelValue);
		}
	});

	private final BiotypeNode bioTypeNode;

	private final InputNode keywordsNode = new InputNode(this, FieldType.AND_CLAUSE, "Keywords", new Strategy<String>() {
		@Override
		public String getModel() {
			String s = query.getKeywords();
			return s;
		}
		@Override
		public void setModel(String modelValue) {
			query.setKeywords(modelValue);
		}
	});

	private final LocationFormNode locationFormNode = new LocationFormNode(this, "Location", new Strategy<Location>() {
		@Override
		public Location getModel() {
			return query.getLocationRoot();
		}
		@Override
		public void setModel(Location modelValue) {
			query.setLocationRoot(modelValue);
		}
	});

	private final ObjectComboBoxNode<Quality> minQualityNode = new ObjectComboBoxNode<Quality>(this, "Min Quality", new QualityComboBox(), new Strategy<Quality>() {
		@Override public Quality getModel() {return query.getMinQuality();}
		@Override public void setModel(Quality modelValue) {query.setMinQuality(modelValue);}
	});

	private final ObjectComboBoxNode<Quality> maxQualityNode = new ObjectComboBoxNode<Quality>(this, "Max Quality", new QualityComboBox(), new Strategy<Quality>() {
		@Override public Quality getModel() {return query.getMaxQuality();}
		@Override public void setModel(Quality modelValue) {query.setMaxQuality(modelValue);}
	});

	private final CheckboxNode onlyContainerCheckbox = new CheckboxNode(this, "Only in Containers", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterNotInContainer()==Boolean.TRUE;
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterNotInContainer(modelValue==Boolean.TRUE);
		}
	});

	private final CheckboxNode onlyLocationCheckbox = new CheckboxNode(this, "Only in Locations", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterNotInLocation()==Boolean.TRUE;
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterNotInLocation(modelValue==Boolean.TRUE);
		}
	});

	private final CheckboxNode filterTrashNode = new CheckboxNode(this, "Hide Trashed/Used Up", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterTrashed();
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterTrashed(modelValue==Boolean.TRUE);
		}
	});


	public BiosampleSearchTree(SpiritFrame frame) {
		this(frame, null, false);
	}

	public BiosampleSearchTree(SpiritFrame frame, final Biotype[] selectableBiotypes, final boolean autoQuery) {
		super();
		this.frame = frame;
		setRootVisible(false);
		this.selectableBiotypes = selectableBiotypes;
		query.setFilterTrashed(selectableBiotypes!=null && selectableBiotypes.length>0);
		//		exactCheckBox.setOpaque(false);

		//Study Category
		if(frame==null) {
			stuNode.setCanExpand(false);
			stuNode.add(studyNode);
			stuNode.add(groupNode);
			stuNode.add(phaseNode);
			top.add(stuNode);
		}


		//Container
		conNode.setCanExpand(false);
		conNode.add(new ObjectComboBoxNode<ContainerType>(this, "ContainerType", containerTypeComboBox, new Strategy<ContainerType>() {
			@Override
			public ContainerType getModel() {
				return query.getContainerType();
			}
			@Override
			public void setModel(ContainerType modelValue) {
				query.setContainerType(modelValue);
			}
		}));
		top.add(conNode);

		//Biosample
		if(selectableBiotypes==null) {
			bioTypeNode = new BiotypeNode(this, new Strategy<Biotype>() {
				@Override
				public Biotype getModel() {
					return query.getBiotypes()!=null && query.getBiotypes().length==1? query.getBiotypes()[0]: null;
				}
				@Override
				public void setModel(Biotype modelValue) {
					query.setBiotypes(modelValue==null? null: new Biotype[]{modelValue});
				}

				@Override
				public void onChange() {
					if((bioTypeNode.getSelection()==null && query.getBiotype()!=null) || (bioTypeNode.getSelection()!=null && !bioTypeNode.getSelection().equals(query.getBiotype()))) {
						updateModel();
						eventBiotypeChanged();
					}
				}
			});
		} else {
			bioTypeNode = new BiotypeToggleNode(this, Arrays.asList(selectableBiotypes),  new Strategy<Biotype>() {
				@Override
				public Biotype getModel() {
					return query.getBiotypes()!=null && query.getBiotypes().length==1? query.getBiotypes()[0]: null;
				}
				@Override
				public void setModel(Biotype modelValue) {
					query.setBiotypes(modelValue==null? selectableBiotypes: new Biotype[]{modelValue});
				}

				@Override
				public void onAction() {
					if(bioTypeNode.getSelection()==null || !bioTypeNode.getSelection().getName().equals(query.getBiotype())) {
						updateModel();
						eventBiotypeChanged();
					}
				}
			});
			bioTypeNode.setVisible(selectableBiotypes.length>1);
		}
		bioNode.setCanExpand(false);
		top.add(bioNode);

		//Location
		locationNode.setCanExpand(true);
		locationNode.add(locationFormNode);




		////////////////////////////
		//Filters

		//OneSample
		cb1 = new CheckboxNode(this, "Select the most-left", new Strategy<Boolean>() {
			@Override
			public Boolean getModel() {
				return query.getSelectOneMode()==BiosampleQuery.SELECT_MOST_LEFT;
			}
			@Override
			public void setModel(Boolean modelValue) {
				query.setSelectOneMode(modelValue? BiosampleQuery.SELECT_MOST_LEFT: BiosampleQuery.SELECT_ALL);
				if(modelValue) {
					query.setSelectOneMode(BiosampleQuery.SELECT_MOST_LEFT);
				}
			}
			@Override
			public void onAction() {
				cb2.getCheckbox().setSelected(false);
			}

		});
		catSelectOneNode.add(cb1);

		cb2 = new CheckboxNode(this, "Select the most-right", new Strategy<Boolean>() {
			@Override
			public Boolean getModel() {
				return query.getSelectOneMode()==BiosampleQuery.SELECT_MOST_RIGHT;
			}
			@Override
			public void setModel(Boolean modelValue) {
				if(modelValue) {
					query.setSelectOneMode(BiosampleQuery.SELECT_MOST_RIGHT);
				}
			}
			@Override
			public void onAction() {
				cb1.getCheckbox().setSelected(false);
			}

		});
		catSelectOneNode.add(cb2);

		top.add(moreNode);

		if(frame!=null) {
			advancedNode.add(groupNode);
			advancedNode.add(phaseNode);
		}

		//Creation
		advancedNode.add(new CreUserNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getCreUser();
			}
			@Override
			public void setModel(String modelValue) {
				query.setCreUser(modelValue);
			}
		}));

		advancedNode.add(new CreDateNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getCreDays();
			}
			@Override
			public void setModel(String modelValue) {
				query.setCreDays(modelValue);
			}
		}));

		advancedNode.add(new UpdUserNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getUpdUser();
			}
			@Override
			public void setModel(String modelValue) {
				query.setUpdUser(modelValue);
			}
		}));

		advancedNode.add(new UpdDateNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getUpdDays();
			}
			@Override
			public void setModel(String modelValue) {
				query.setUpdDays(modelValue);
			}
		}));
		advancedNode.add(new DepartmentNode(this, new Strategy<EmployeeGroup>() {
			@Override
			public EmployeeGroup getModel() {
				return query.getDepartment();
			}
			@Override
			public void setModel(EmployeeGroup modelValue) {
				query.setDepartment(modelValue);
			}
		}));
		advancedNode.add(catSelectOneNode);
		advancedNode.add(minQualityNode);
		advancedNode.add(maxQualityNode);
		advancedNode.add(onlyContainerCheckbox);
		advancedNode.add(onlyLocationCheckbox);

		//Trashed
		moreNode.setCanExpand(false);
		if(selectableBiotypes!=null && selectableBiotypes.length>0) {
			stuNode.setVisible(selectableBiotypes.length==1 && (selectableBiotypes[0].getCategory()==BiotypeCategory.LIVING || selectableBiotypes[0].getCategory()==BiotypeCategory.SOLID || selectableBiotypes[0].getCategory()==BiotypeCategory.LIQUID));

			boolean canSelectContainer = false;
			for(Biotype type: selectableBiotypes) {
				if(!type.isAbstract() && !type.isHideContainer() && type.getContainerType()==null) {
					canSelectContainer = true;
					break;
				}
			}
			conNode.setVisible(canSelectContainer);

			bioNode.setExpanded(true);
			catSelectOneNode.setVisible(false);
			if(selectableBiotypes.length==1) {
				query.setBiotype(selectableBiotypes[0]);
				bioTypeNode.setSelection(selectableBiotypes[0]);
			}
			containerTypeComboBox.setVisible(false);
		} else {
			bioTypeNode.setValues(DAOBiotype.getBiotypes());
		}

		setRoot(top);
		eventStudyChanged();
	}


	public void eventStudyChanged() {
		BiosampleQuery query = getQuery();
		List<Study> studies = new ArrayList<>();
		try {
			if(query.getStudyIds()!=null && query.getStudyIds().length()>0) {
				studies = DAOStudy.queryStudies(StudyQuery.createForStudyIds(query.getStudyIds()), SpiritFrame.getUser());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		//Update biotype/container filters
		if(studies.size()>0) {
			Set<ContainerType> allContainerTypes = new TreeSet<ContainerType>();
			Set<Biotype> allBioTypes = new TreeSet<Biotype>();
			for (Study study : studies) {
				allContainerTypes.addAll(DAOStudy.getContainerTypes(study));
				allBioTypes.addAll(DAOStudy.getBiotypes(study));
			}
			containerTypeComboBox.setValues(allContainerTypes);
			bioTypeNode.setValues(allBioTypes);
			conNode.setVisible(containerTypeComboBox.getValues().size()>0);
		} else {
			containerTypeComboBox.setValues(Arrays.asList(ContainerType.values()));
			if(selectableBiotypes==null || selectableBiotypes.length==0) {
				bioTypeNode.setValues(DAOBiotype.getBiotypes());
			}
		}

		//Update group, phases
		Study study = studies.size()==1? studies.get(0) : null;
		study = JPAUtil.reattach(study);
		groupNode.setStudy(study);
		phaseNode.setStudy(study);


		//filter available containers
		containerTypeComboBox.setVisible(containerTypeComboBox.getValues().size()>0);

		eventBiotypeChanged();
		bioNode.setVisible(bioTypeNode.getValues().size()>0);
	}


	public void eventBiotypeChanged() {
		bioNode.clearChildren();
		query.getLinker2values().clear();

		bioTypeNode.setCanExpand(false);
		bioNode.add(bioTypeNode);
		bioNode.add(keywordsNode);
		bioNode.add(quickSearchNode);

		Biotype type = query.getBiotype();

		if(type==null || type.isAbstract()){
			query.setFilterNotInContainer(false);
		}

		moreNode.clearChildren();
		if(type!=null) {

			//Add filter for parents biotypes
			List<Biotype> parentTypes = type.getHierarchy();
			for(int i=0; i<parentTypes.size()-1; i++) {
				Biotype b = parentTypes.get(i);

				LabelNode linkerNode = new LabelNode(this, b.getName());
				linkerNode.setIcon(new ImageIcon(ImageFactory.getImage(b, 26)));
				linkerNode.setCanExpand(true);
				linkerNode.setExpanded(false);
				moreNode.add(linkerNode);

				if(!b.isHideSampleId()) {
					addFilter(linkerNode, new BiosampleLinker(b, LinkerType.SAMPLEID));
				}

				if(b.getSampleNameLabel()!=null) {
					addFilter(linkerNode, new BiosampleLinker(b, LinkerType.SAMPLENAME));
				}
				for(BiotypeMetadata mt2: b.getMetadata()) {
					addFilter(linkerNode, new BiosampleLinker(mt2));
				}
				addFilter(linkerNode, new BiosampleLinker(b, LinkerType.COMMENTS));
			}

			LabelNode linkerNode = new LabelNode(this, type.getName());
			linkerNode.setIcon(new ImageIcon(ImageFactory.getImage(type, 26)));
			linkerNode.setCanExpand(true);
			linkerNode.setExpanded(false);
			moreNode.add(linkerNode);

			//Filter for sampleId
			if(!type.isHideSampleId()) {
				addFilter(linkerNode, new BiosampleLinker(LinkerType.SAMPLEID, type));
			}

			//Filter for sampleName
			if(type.getSampleNameLabel()!=null) {
				addFilter(linkerNode, new BiosampleLinker(LinkerType.SAMPLENAME, type));
			}

			//Filter for metadata
			for(BiotypeMetadata mt2: type.getMetadata()) {
				if(mt2.getDataType()==DataType.BIOSAMPLE && mt2.getParameters()!=null) {
					//Filter for metadata: linked biosample
					Biotype biotype2 = DAOBiotype.getBiotype(mt2.getParameters());
					if(biotype2==null) {
						addFilter(linkerNode, new BiosampleLinker(mt2));
					} else {
						AbstractNode<?> node;
						if(!biotype2.isHideSampleId()) {
							node = addFilter(linkerNode, new BiosampleLinker(mt2, LinkerType.SAMPLEID, biotype2));
						} else {
							node = new LabelNode(this, biotype2.getName());
						}

						if(biotype2.getSampleNameLabel()!=null) {
							addFilter(node, new BiosampleLinker(mt2, LinkerType.SAMPLENAME, biotype2));
						}
						for(BiotypeMetadata mt3: biotype2.getMetadata()) {
							addFilter(node, new BiosampleLinker(mt2, mt3));
						}
						addFilter(node, new BiosampleLinker(mt2, LinkerType.COMMENTS, biotype2));
					}
				} else {
					//Filter for metadata: not linked biosample
					addFilter(linkerNode, new BiosampleLinker(mt2));
				}
			}

			//Filter for comments
			addFilter(linkerNode, new BiosampleLinker(LinkerType.COMMENTS, type));

		}

		if(selectableBiotypes==null || selectableBiotypes.length>1 || !selectableBiotypes[0].isAbstract()) {
			locationNode.setExpanded(false);
			moreNode.add(locationNode);
		}

		advancedNode.setExpanded(false);
		moreNode.add(advancedNode);

		moreNode.add(filterTrashNode);

		updateView();
	}

	private AbstractNode<?> addFilter(AbstractNode<?> linkerNode, BiosampleLinker linker) {
		AbstractNode<?> res = null;
		Biotype biotype2 = linker.getBiotypeForLabel();
		if(biotype2==null) return null;
		String label = linker.getLabelShort();
		if(linker.getType()==LinkerType.SAMPLEID && biotype2.getCategory()==BiotypeCategory.LIBRARY) {
			linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override
				public Collection<String> getChoices() {
					return DAOBiotype.getAutoCompletionFieldsForSampleId(biotype2);
				}
			});
		} else if(linker.getType()==LinkerType.SAMPLENAME && biotype2.isNameAutocomplete()) {
			linkerNode.add(res = new TextComboBoxNode(this, label, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override
				public Collection<String> getChoices() {
					return DAOBiotype.getAutoCompletionFieldsForName(biotype2, frame==null? studyNode.getStudy(): frame.getStudy());
				}
			});
		} else if(linker.getType()==LinkerType.SAMPLENAME || linker.getType()==LinkerType.SAMPLEID){
			linkerNode.add(res = new InputNode(this, label, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}));
		} else if(linker.getType()==LinkerType.COMMENTS) {
			linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
				@Override public String getModel() { return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override public Collection<String> getChoices() {return DAOBiotype.getAutoCompletionFieldsForComments(biotype2, frame==null? studyNode.getStudy(): frame.getStudy());}
			});
		} else if(linker.getBiotypeMetadata()!=null) {
			if(linker.getBiotypeMetadata().getDataType()==DataType.AUTO || linker.getBiotypeMetadata().getDataType()==DataType.LIST) {

				linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}) {
					@Override
					public Collection<String> getChoices() {
						return DAOBiotype.getAutoCompletionFields(linker.getBiotypeMetadata(), frame==null? studyNode.getStudy(): frame.getStudy());
					}
				});
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.D_FILE) {
				//skip
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.FILES) {
				//skip
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {
				linkerNode.add(res = new MultiNode(this, label, linker.getBiotypeMetadata().extractChoices(), new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}));
			} else {
				linkerNode.add(res = new InputNode(this, label, new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}));

			}
		}
		if(res!=null && linker.getBiotypeForLabel()!=null) {
			Image icon = ImageFactory.getImage(linker.getBiotypeForLabel(), 22);
			System.out.println("BiosampleSearchTree.addFilter() "+linker+"> "+icon);
			if(icon!=null && (res.getComponent()) instanceof JCustomTextField) {
				((JCustomTextField) res.getComponent()).setIcon(new ImageIcon(icon));
			}
		}
		return res;
	}

	public BiosampleQuery getQuery() {
		query.setSelectOneMode(BiosampleQuery.SELECT_ALL);
		query.setStudyIds(frame==null? null: frame.getStudyId());

		updateModel();
		return query;
	}

	public void setQuery(BiosampleQuery query) {
		//update our model
		this.query.copyFrom(query);
		if(frame!=null) {
			frame.setStudyId(query.getStudyIds());
		}
		updateView();

		//recreate the study tab
		eventStudyChanged();

		//recreate the metadata tab
		eventBiotypeChanged();
	}

	public String getStudyId() {
		return frame==null? studyNode.getSelection(): frame.getStudyId();
	}

	public void setStudyId(String v) {
		if(v==null || v.equals(frame==null? studyNode.getSelection(): frame.getStudyId())) return;
		setQuery(BiosampleQuery.createQueryForStudyIds(v));
	}

	public Biotype getBiotype() {
		return bioTypeNode.getSelection();
	}
	public void setBiotype(Biotype v) {
		bioTypeNode.setSelection(v);
	}


	//	/**
	//	 * Gets the list of linkers from a given biotype to be displayed such as
	//	 * - Parent2
	//	 * - Parent1
	//	 * - Biotype
	//	 *   - Aggregated
	//	 *   - ...
	//	 *
	//	 * @param biotype
	//	 * @return
	//	 */
	//	public static ListHashMap<Pair<String, Biotype>, BiosampleLinker> getLinkers(Biotype biotype) {
	//		ListHashMap<Pair<String, Biotype>, BiosampleLinker> res = new ListHashMap<>();
	//
	//		//Look at own metadata
	//
	//		Pair<String, Biotype> key = new Pair<String, Biotype>(biotype.getName(), biotype);
	//		if(!biotype.isHideSampleId()) {
	//			res.add(key, new BiosampleLinker(LinkerType.SAMPLEID, biotype));
	//		}
	//		if(biotype.getSampleNameLabel()!=null) {
	//			res.add(key, new BiosampleLinker(LinkerType.SAMPLENAME, biotype));
	//		}
	//		for(BiotypeMetadata mt2: biotype.getMetadata()) {
	//			res.add(key, new BiosampleLinker(mt2));
	//		}
	//		res.add(key, new BiosampleLinker(LinkerType.COMMENTS, biotype));
	//
	//		//Look at aggregated Data
	//		for(BiotypeMetadata mt: biotype.getMetadata()) {
	//			if(mt.getDataType()!=DataType.BIOSAMPLE) continue;
	//			if(mt.getParameters()==null) continue;
	//			Biotype biotype2 = DAOBiotype.getBiotype(mt.getParameters());
	//			if(biotype2==null) continue;
	//			String label = mt.getName();
	//			key = new Pair<String, Biotype>(label, biotype2);
	//
	//			if(!biotype2.isHideSampleId()) {
	//				res.add(key, new BiosampleLinker(mt, LinkerType.SAMPLEID));
	//			}
	//			if(biotype2.getSampleNameLabel()!=null) {
	//				res.add(key, new BiosampleLinker(mt, LinkerType.SAMPLENAME, biotype2));
	//			}
	//			for(BiotypeMetadata mt2: biotype2.getMetadata()) {
	//				res.add(key, new BiosampleLinker(mt, mt2));
	//			}
	//			res.add(key, new BiosampleLinker(mt, LinkerType.COMMENTS, biotype2));
	//		}
	//
	//		//Look at parent types
	//		Biotype b = biotype.getParent();
	//		while(b!=null) {
	//			key = new Pair<String, Biotype>(b.getName(), b);
	//
	//			if(!b.isHideSampleId()) {
	//				res.add(key, new BiosampleLinker(b, LinkerType.SAMPLEID));
	//			}
	//
	//			if(b.getSampleNameLabel()!=null) {
	//				res.add(key, new BiosampleLinker(b, LinkerType.SAMPLENAME));
	//			}
	//			for(BiotypeMetadata mt2: b.getMetadata()) {
	//				res.add(key, new BiosampleLinker(mt2));
	//			}
	//			res.add(key, new BiosampleLinker(b, LinkerType.COMMENTS));
	//
	//			b = b.getParent();
	//		}
	//		return res;
	//	}
}
