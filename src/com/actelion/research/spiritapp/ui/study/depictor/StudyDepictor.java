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

package com.actelion.research.spiritapp.ui.study.depictor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class StudyDepictor extends JPanel implements MouseListener, MouseMotionListener {

	protected Study study;
	private BufferedImage imgBuffer;

	private boolean isBlind;
	private boolean isBlindAll;

	protected Selection mouseOver;

	protected int OFFSET_Y;
	protected int maxY;
	protected int marginX = 25;
	protected int maxX;

	protected boolean designerMode = false;
	private boolean printLegend = true;

	private Calendar calendar = Calendar.getInstance();

	private Map<Pair<Group, Integer>, Integer> groupSubgroup2Y = new HashMap<>();
	private Map<Pair<Group, Integer>, Integer> groupSubgroup2samplingY = new HashMap<>();
	private Map<Pair<Group, Integer>, Integer> groupSubgroup2height = new HashMap<>();
	private int phaseWidth;
	private Map<Phase, Integer> phase2X = new HashMap<>();
	private int sizeFactor = 0;
	private Map<Phase, Phase> nextPhaseMap;
	private boolean forRevision;

	public StudyDepictor() {
		addMouseListener(this);
		addMouseMotionListener(this);
		setBackground(Color.WHITE);
		ToolTipManager.sharedInstance().registerComponent(this);


		//Mouse scroll and zoom
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.isControlDown()) {
					if(e.getWheelRotation()<0) {
						setSizeFactor(Math.min(7, getSizeFactor()+1));
					} else if(e.getWheelRotation()>0) {
						setSizeFactor(Math.max(-7, getSizeFactor()-1));
					}
					e.consume();
				} else if(getParent() instanceof JViewport) {
					Point pt = ((JViewport) getParent()).getViewPosition();
					if(e.getWheelRotation()<0) {
						((JViewport) getParent()).setViewPosition(new Point(pt.x, Math.max(0, pt.y-100)));
					} else {
						((JViewport) getParent()).setViewPosition(new Point(pt.x, Math.min(getHeight(), pt.y+100)));
					}
				}
			}
		});

	}

	/**
	 * If the depictor is set to be in revion mode, we don't display the date
	 * @param forRevision
	 */
	public void setForRevision(boolean forRevision) {
		this.forRevision = forRevision;
	}

	public boolean isForRevision() {
		return forRevision;
	}

	public void setSizeFactor(int sizeFactor) {
		this.sizeFactor = sizeFactor;
		phase2X = null;
		repaint(true);
	}

	public int getSizeFactor() {
		return sizeFactor;
	}

	public void repaint(boolean force) {
		if (force) {
			imgBuffer = null;
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {

		if (imgBuffer == null /*|| imgBuffer.getWidth() != getWidth() || imgBuffer.getHeight() != getHeight()*/) {
			imgBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) imgBuffer.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			draw(g);
			g.dispose();
		}
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, getWidth(), getHeight());
		graphics.drawImage(imgBuffer, 0, 0, this);

		// Paint mouseover
		if (mouseOver != null) {
			int y = getY(mouseOver.getGroup(), mouseOver.getSubGroup());
			int x = getX(mouseOver.getPhase());
			graphics.setColor(new Color(170, 170, 220, 100));
			graphics.fillRoundRect(x - phaseWidth/2, y - 16, phaseWidth, 30, 6, 6);
		}

	}

	public void setDesignerMode(boolean designerMode) {
		this.designerMode = designerMode;
	}

	private void draw(Graphics2D g) {
		try {
			UIUtils.applyDesktopProperties(g);

			//Refresh actual time
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, -1);
			Date now = cal.getTime();

			if (study == null) return;
			study.resetCache();
			phase2X = null;
			nextPhaseMap = null;
			computePositions(false);
			String s;
			if(isPrintLegend()) {
				s = study.getStudyId();
				if (s == null) s = "";
				g.setColor(Color.BLACK);
				g.setFont(FastFont.BIGGER);
				g.drawString(s, 2, FastFont.BIGGER.getSize());
				if(study.getLocalId()!=null && study.getLocalId().length()>0) {
					g.setFont(FastFont.REGULAR);
					g.drawString(study.getLocalId(), 2, FastFont.BIGGER.getSize()+FastFont.REGULAR.getSize()+2);
				}
			}

			paintBackground(g);

			// Draw Phases
			int prevDays = 0;
			int lapse = study.getPhaseFormat()==PhaseFormat.DAY_MINUTES?
					(study.getPhases().size()==0?
							100:
								(study.getLastPhase().getDays() - study.getFirstPhase().getDays())>7? 7: 1):
									100;
								boolean even = true;
								for (Phase phase : study.getPhases()) {
									if(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES) {
										if (phase.getDays()/lapse != prevDays/lapse) even = !even;
									}

									paintPhase(g, phase, even);

									prevDays = phase.getDays();
								}


								// Show cursor for current date?
								if(!forRevision && study.getFirstDate()!=null && study.getPhases().size()>0) {
									int x = maxX+12;
									for (Phase p : study.getPhases()) {

										Date d = p.getAbsoluteDate();
										if(d!=null && d.after(now)) {
											x = getX(p);
											break;
										}
									}
									g.setColor(UIUtils.getColor(255, 200, 0));

									g.drawLine(x - phaseWidth/2-1, 1, x - phaseWidth/2-1, maxY - 10 - 1);
									g.drawLine(x - phaseWidth/2, 1, x - phaseWidth/2, maxY - 10 - 1);
								}

								//Paint Groups
								paintGroups(g);


								//Show exceptional cases (death, ...): red background
								if(!SpiritRights.isBlind(study, SpiritFrame.getUser())) {
									for (Biosample b : study.getParticipants()) {
										if(b.getInheritedGroup()==null) continue;
										Pair<Status, Phase> lastStatus = b.getLastActionStatus();
										if(lastStatus.getFirst()!=null && lastStatus.getFirst()!=null && !lastStatus.getFirst().isAvailable()) {
											Phase p = lastStatus.getSecond();
											int x = getX(p);
											int y = getY(b.getInheritedGroup(), b.getInheritedSubGroup());

											g.setPaint(new RadialGradientPaint(x, y, 14, new float[]{0,1}, new Color[]{UIUtils.getColor(255,0,0,120), UIUtils.getColor(255,255,255,0)}));
											g.fillOval(x-16, y-12, 32, 24);
										}
									}
								}


								//Draw Actions
								for (Group group : study.getGroups()) {
									for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {
										for (Phase phase : study.getPhases()) {
											StudyAction action = study.getStudyAction(group, subgroupNo, phase);
											paintAction(g, action);
										}
										if(isBlind) break;
									}
								}

								//Draw a cross if the study is stopped
								if("STOPPED".equalsIgnoreCase(study.getState())) {
									g.setColor(Color.RED);
									g.drawLine(10, 10, getWidth()-20, getHeight()-20);
									g.drawLine(10, getHeight()-20, getWidth()-20, 10);
								}


		} catch (Exception e) {
			e.printStackTrace();
			g.setColor(Color.RED);
			g.drawString("Error: " + e.getMessage(), 0, 30);
		}

	}

	public Selection getSelectionAt(int x, int y) {
		if (study == null) return null;
		Group selGroup = null;
		int selSubgroupNo = 0;
		Phase selPhase = null;
		groupLoop: for (Group group : study.getGroups()) {
			if (group.getStudy() == null) continue;

			int currentY = getY(group, 0);

			for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {
				currentY = getY(group, subgroupNo);
				if (y > currentY - 15 && y < currentY + 15) {
					selGroup = group;
					selSubgroupNo = subgroupNo;
					break groupLoop;
				}
			}
		}
		if(selGroup==null) return null;
		if(x<marginX) {
			return new Selection(selGroup, null, selSubgroupNo);
		}

		for(Phase current: study.getPhases()) {
			int x1 = getX(current);

			if (x >= x1 - 10 && x < x1 + phaseWidth) {
				selPhase = current;
			} else if(x<x1+11) {
				break;
			}
		}
		if (selPhase != null) {
			return new Selection(selGroup, selPhase, selSubgroupNo);
		}
		return null;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		Selection sel = getSelectionAt(e.getX(), e.getY());
		if (sel == null || sel.getGroup() == null) {
			return null;
		}
		if(sel.getPhase()==null && !SpiritRights.isBlind(study, SpiritFrame.getUser())) {
			//Selection done on a group
			StringBuilder sb = new StringBuilder();
			sb.append("<body><div style='font-size:8px;margin:0;padding:0; background: " + UIUtils.getHtmlColor(UIUtils.getDilutedColor(Color.WHITE, sel.getGroup().getBlindedColor(SpiritFrame.getUsername()))) + "'>");
			sb.append("<span style='font-size:9px'><b>" + MiscUtils.removeHtml(sel.getGroup().getShortName()) + "</b> " + MiscUtils.removeHtml(sel.getGroup().getNameWithoutShortName()) + "</span><br>");
			Collection<Biosample> biosamples = study.getParticipants(sel.getGroup());
			if(biosamples.size()>0 && biosamples.size()<=10) {
				for (Biosample b : biosamples) {
					Pair<Status, Phase> p = b.getLastActionStatus();
					sb.append(b.getSampleIdName() + (p.getFirst()!=null &&  !p.getFirst().isAvailable()? " " + p.getFirst()+ "->" + p.getSecond().getShortName(): "") +  "<br>");
				}
			} else {
				sb.append(biosamples.size() + " biosamples<br>");
			}
			return "<html><body style='padding:0px;margin:0px'>" + sb.toString();

		} else if(sel.getPhase()!=null) {
			//Selection done on an action

			try {
				StringBuilder sb = new StringBuilder();
				sb.append("<body><div style='font-size:8px;margin:0;padding:0; background: " + UIUtils.getHtmlColor(UIUtils.getDilutedColor(Color.WHITE, sel.getGroup().getBlindedColor(SpiritFrame.getUsername()))) + "'>");
				sb.append("<span style='font-size:9px'><b>" + MiscUtils.removeHtml(sel.getGroup().getShortName()) + (sel.getGroup().getNSubgroups() <= 1 ? "" : "<span style='font-size:8px'> '" + (sel.getSubGroup() + 1) + "</span>") + "</b> / " + sel.getPhase().toString() + "</span>");
				sb.append("<br>");

				if (sel.getPhase().equals(sel.getGroup().getFromPhase())) {
					sb.append("Group Assignment<br>");
				}
				StudyAction action = study.getStudyAction(sel.getGroup(), sel.getSubGroup(), sel.getPhase());
				if (action != null) {
					String measurements = "";
					if (action.isMeasureFood())
						measurements += "Food<br>";
					if (action.isMeasureWater())
						measurements += "Water<br>";
					if (action.isMeasureWeight())
						measurements += "Weighing<br>";
					for(Measurement em: action.getMeasurements()) {
						measurements +=  (em.getTest()==null?"??":em.getTest().getName().replace("<", "&lt;").replace(">", "&gt;")) + (em.getParametersString().length()>0? ": " + em.getParametersString():"") + "<br>";
					}
					if (measurements.length() > 0) {
						sb.append("<span style='color:blue'>" + measurements + "</span>");
					}

					//Treatment
					if (action.getNamedTreatment() != null) {
						NamedTreatment t = action.getNamedTreatment();
						Color c = t.getColor() == null ? Color.BLACK : t.getColor();
						sb.append("<b style='color:" + UIUtils.getHtmlColor(c) + "'>" + t.getName() + "</b><br>");
					}

					//Sampling
					if (action.getNamedSampling1() != null) {
						NamedSampling s = action.getNamedSampling1();
						sb.append("<b style='color:#990000'>" + s.getName() + "</b><br>");
					}
					if (action.getNamedSampling2() != null) {
						NamedSampling s = action.getNamedSampling2();
						sb.append("<b style='color:#990000'>" + s.getName() + "</b><br>");
					}

					//Label
					if(action.getLabel()!=null && action.getLabel().length()>0) {
						sb.append("<u>"+action.getLabel()+"</u><br>");
					}

					//Participants
					List<Biosample> samples =  study.getParticipants(sel.getGroup(), sel.getSubGroup());
					if(samples.size()<10) {
						for (Biosample b : samples) {
							Pair<Status, Phase> p = b.getLastActionStatus();
							sb.append(b.getSampleIdName() + (p.getFirst()!=null && !p.getFirst().isAvailable()? " " + p.getFirst() + (p.getSecond()!=null? "->" + p.getSecond().getShortName(): ""): "") +  "<br>");
						}
					}

				}
				return "<html><body style='padding:0px;margin:0px'>" + sb + "</body></html>";
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;

	}

	private void drawHorizontalArrow(Graphics g, int x, int y, int len) {
		g.drawLine(x, y, x + len - 3, y);

		int[] xs = new int[] { x + len - 8, x + len - 8, x + len };
		int[] ys = new int[] { y - 4, y + 4, y };
		g.fillPolygon(xs, ys, xs.length);
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study s) {
		this.study = s;

		isBlindAll = SpiritRights.isBlindAll(this.study, SpiritFrame.getUser());
		isBlind = SpiritRights.isBlind(this.study, SpiritFrame.getUser());

		imgBuffer = null;
		phase2X = null;
		computePositions(true);

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mouseOver = null;
		super.repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			mouseClicked(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			mouseClicked(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Selection s = getSelectionAt(e.getX(), e.getY());
		if ((s == null && mouseOver == null) || (s != null && s.equals(mouseOver)))
			return;
		if (s == null || s.getGroup() == null || s.getPhase() == null) {
			mouseOver = null;
		} else {
			mouseOver = s;
		}
		super.repaint();
	}

	public Selection getMouseOver() {
		return mouseOver;
	}

	public BufferedImage getImage() {
		computePositions(false);
		int width = maxX + 30;
		int height = maxY + (isPrintLegend()? 80:0);
		setSize(width, height);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		Color oldBg = getBackground();
		setBackground(new Color(235, 235, 245));
		g.setColor(new Color(235, 235, 245));
		g.fillRoundRect(0, 0, width - 1, height - 1, 5, 5);
		draw((Graphics2D) g);
		setBackground(oldBg);
		g.setColor(Color.LIGHT_GRAY);
		g.drawRoundRect(0, 0, width - 1, height - 1, 5, 5);
		g.dispose();
		return img;
	}


	protected void computePositions(boolean adjustZoomFactor) {
		if (study == null) return;

		if (phase2X == null) {


			//Calculate the marginX: between groups and phases
			marginX = 60;
			Font f = FastFont.REGULAR;
			for (Group group : study.getGroups()) {
				String s = group.getBlindedName(SpiritFrame.getUsername());
				if (s.length() > 24) s = s.substring(0, 24);
				if(group.getNSubgroups()>1) s+="'"+group.getNSubgroups();
				int w = getFontMetrics(f).stringWidth(s) + 15;
				marginX = Math.max(marginX, w);
			}

			int nPhases = study.getPhases().size();
			int normalPhaseWidth = FastFont.getDefaultFontSize()*2+18;

			if(adjustZoomFactor) {
				//estimate zoom factor so that nPhases*40*1.35^SF=800
				double SF = Math.log(Math.max(300.0, getWidth()-marginX-50)/(normalPhaseWidth*(nPhases+1)))/Math.log(1.2);
				sizeFactor = (int)Math.round(SF-.3);
			}
			sizeFactor = Math.max(-7, Math.min(7, sizeFactor));

			phaseWidth = (int) (normalPhaseWidth * Math.pow(1.2, sizeFactor));
			int x = marginX + 20;

			// PhaseNo2X positions
			phase2X = new HashMap<>();
			List<Phase> phases = new ArrayList<>(study.getPhases());
			Collections.sort(phases);
			for (Phase p : phases) {
				phase2X.put(p, x);
				x += phaseWidth;
			}
			maxX = x;

			// Offset_Y
			FontMetrics fm1 = getFontMetrics(FastFont.MEDIUM);
			FontMetrics fm2 = getFontMetrics(FastFont.SMALL);
			int height = fm1.stringWidth("00/00/00 We");
			for (Phase p : study.getPhases()) {
				int w = fm1.stringWidth(p.getShortName());
				if(phaseWidth<19) {
					w+=fm2.stringWidth("00/00/00");
				} else if(p.getLabel()!=null && p.getLabel().length()>0) {
					w+= fm1.stringWidth(p.getLabel().length()>10? p.getLabel().substring(0,10): p.getLabel());
				}
				height = Math.max(height, w + 2);
			}
			OFFSET_Y = height + 2;

			// GroupNo2Y positions
			groupSubgroup2Y.clear();
			groupSubgroup2samplingY.clear();
			groupSubgroup2height.clear();
			int y = OFFSET_Y + FastFont.getDefaultFontSize()*2;

			List<Group> groupsSortedHierarchical = study.getGroupsHierarchical();
			for (Group group : groupsSortedHierarchical) {
				if (group.getStudy() == null) continue;

				if(isBlind) {
					for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {
						groupSubgroup2Y.put(new Pair<Group, Integer>(group, subgroupNo), y);
						groupSubgroup2samplingY.put(new Pair<Group, Integer>(group, subgroupNo), y+10);
						groupSubgroup2height.put(new Pair<Group, Integer>(group, subgroupNo), 40);
					}
					y += FastFont.getDefaultFontSize()*3;
				} else {
					for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {


						boolean hasSamplings2 = false;
						for(StudyAction a: study.getStudyActions(group, subgroupNo)) {
							if(a.getNamedSampling2()!=null) {hasSamplings2 = true; break;}
						}

						int groupHeight = 2 + FastFont.getDefaultFontSize()*2 + (hasSamplings2?FastFont.SMALL.getSize()-1:0) + (subgroupNo==group.getNSubgroups()-1? FastFont.SMALL.getSize()-1:0);
						groupSubgroup2Y.put(new Pair<Group, Integer>(group, subgroupNo), y);
						groupSubgroup2samplingY.put(new Pair<Group, Integer>(group, subgroupNo), y + FastFont.SMALL.getSize()-1);
						groupSubgroup2height.put(new Pair<Group, Integer>(group, subgroupNo), groupHeight);
						y += groupHeight;
					}
				}
			}
			maxY = y;
		}
		maxY+=10;
		setSize(new Dimension(Math.max(200, maxX) + 50, maxY + (isPrintLegend()? 80:0)));
	}

	@Override
	public Dimension getPreferredSize() {
		if (phase2X == null) computePositions(false);
		Dimension d = new Dimension(Math.max(200, maxX) + 50, maxY + (isPrintLegend()? 80:0));
		return d;
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public int getY(Group group, int subGroupNo) {
		Integer y = groupSubgroup2Y.get(new Pair<Group, Integer>(group, subGroupNo));
		if (y != null) return y;
		return 0;
	}

	public int getSamplingY(Group group, int subGroupNo) {
		Integer y = groupSubgroup2samplingY.get(new Pair<Group, Integer>(group, subGroupNo));
		if (y != null) return y;
		return 0;
	}

	public int getHeight(Group group, int subGroupNo) {
		Integer y = groupSubgroup2height.get(new Pair<Group, Integer>(group, subGroupNo));
		if (y != null) return y;
		return 0;
	}

	public int getX(Phase phase) {
		if (phase2X == null) return 0;// Should not happen

		Integer x = phase2X.get(phase);
		if (x != null) return x;
		return 0;
	}


	protected void paintBackground(Graphics2D g) {
		//Group background
		for (Group group : study.getGroups()) {
			int y1 = getY(group, 0);
			int y2 = getY(group, group.getNSubgroups() - 1);
			Color bg = group.getBlindedColor(SpiritFrame.getUsername());
			Color c1 = bg;
			Color c2 = UIUtils.getDilutedColor(bg, getBackground(), .3);
			g.setColor(c1);

			g.setPaint(new GradientPaint(0, 0, c1, marginX, 0, c2));
			int offsetTop = FastFont.getDefaultFontSize()+5;
			g.fillRect(1, y1-offsetTop, marginX, y2-y1+getHeight(group, group.getNSubgroups()-1)-1 );

			g.setColor(c2);
			g.fillRect(marginX, y1-offsetTop, maxX+6-marginX, y2-y1+getHeight(group, group.getNSubgroups()-1)-1 );
			g.fillPolygon(new int[]{maxX+6, maxX+28, maxX},
					new int[]{y1-offsetTop, (y1-offsetTop + y2-offsetTop+getHeight(group, group.getNSubgroups()-1))/2-1, y2-offsetTop+getHeight(group, group.getNSubgroups()-1)-1}, 3);

		}
	}

	protected void paintGroups(Graphics2D g) {
		final int offset=0;

		//Paint background + groupName + description + horizontal lines
		for (Group group : study.getGroups()) {
			int y1 = getY(group, 0);
			int y2 = getY(group, group.getNSubgroups() - 1);
			Color bg = group.getBlindedColor(SpiritFrame.getUsername());

			//Write Group.FullName
			g.setFont(FastFont.REGULAR);
			String s = group.getBlindedName(SpiritFrame.getUser().getUsername());
			if (s.length() > 24) s = s.substring(0, 24);
			g.setColor(UIUtils.getForeground(bg));
			g.drawString(s, 5, y1 - 3);

			if(designerMode) {
				//Write summary
				String desc = group.getDescription(-1);
				g.setColor(UIUtils.getColor(60,60,90));
				g.setFont(FastFont.SMALLER);
				UIUtils.drawString(g, desc, 2, y1+7, marginX-8, y2 + getHeight(group, group.getNSubgroups() - 1) - 24  - y1 , false);
			}

			for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {
				List<Phase> newGroupingPhases = group.getNewGroupingPhases();
				Phase endPhase = group.getEndPhase(subgroupNo);
				int y = getY(group, subgroupNo);
				int x1 = group.getFromPhase() == null ? marginX+16 : getX(group.getFromPhase());
				boolean endWithNecro = endPhase != null;

				// Draw gray line from begin to start
				g.setColor(Color.LIGHT_GRAY);
				if (marginX < x1) g.drawLine(marginX, y, x1, y);

				// Find how the group is split
				for (Phase phase : newGroupingPhases) {
					int n = group.getNAnimals(phase);
					if (n <= 0 && (endPhase == null || phase.compareTo(endPhase) < 0)) {
						endPhase = phase;
						endWithNecro = false;
					}
				}

				// Draw solid line from start to end
				g.setColor(Color.BLACK);
				if (endPhase != null) {
					int x2 = getX(endPhase);
					if (endWithNecro) { // Necropsy, draw horizontal line and cross
						g.drawLine(x1, y, x2, y);
						g.drawLine(x2 - 4, y - 4, x2 + 5, y + 4);
						g.drawLine(x2 - 4, y + 4, x2 + 5, y - 4);
					} else { // Split to group, draw horizontal line
						g.drawLine(x1, y, x2 - offset, y);
					}
				} else {// Continuing Group, draw horizontal line to the end
					int x2 = maxX;
					drawHorizontalArrow(g, x1, y, Math.max(30, x2 - x1 + 18));
				}

				if (group.getFromGroup() != null) {
					int y0 = getY(group.getFromGroup(), 0);
					g.drawLine(x1 - offset, y0, x1 - offset, y);
					g.drawLine(x1 - offset, y, x1, y);

					g.setColor(Color.LIGHT_GRAY);
					g.drawLine(x1 - offset+1, y0+1, x1 - offset+1, y-1);
				}
				if(isBlind) break;
			}
		}

		//
		//paint subgroups and lines
		for (Group group : study.getGroups()) {
			for (int subgroupNo = 0; subgroupNo < group.getNSubgroups(); subgroupNo++) {
				int y = getY(group, subgroupNo);
				Phase randoPhase = group.getFromPhase() != null ? group.getFromPhase() : null;
				int x1 = randoPhase == null ? marginX+16 : getX(randoPhase);

				// Write the initial expected number of animals to the left of the phase (red if there is mismatch)
				if(!isBlind) {
					//Write SubGroupNo if we have subgroups
					if (group.getNSubgroups() > 1) {
						g.setFont(FastFont.SMALL);
						String s1 = "'" + (1 + subgroupNo);
						g.setColor(UIUtils.getForeground(group.getBlindedColor(SpiritFrame.getUser().getUsername())));
						g.drawString(s1, marginX - g.getFontMetrics().stringWidth(s1) - 2 - 1, y + 3);
					}

					String s = "";
					if(designerMode) {
						s = study.getParticipants(group, subgroupNo).size() + (group.getSubgroupSize(subgroupNo)>0?"/" + group.getSubgroupSize(subgroupNo):"");
					} else if(study.getParticipants(group, subgroupNo).size()>0) {
						s = "" + study.getParticipants(group, subgroupNo).size();
					}
					g.setFont(FastFont.MEDIUM);
					g.setColor(Color.BLACK);
					g.drawString( s, x1 - 3 - g.getFontMetrics().stringWidth(s), y - 1);
				}



				// Draw Oval at groupsassignment
				if (group.getFromGroup() == null && group.getFromPhase() != null) {
					g.setColor(Color.BLACK);
					g.fillOval(x1 - 3, y - 3, 7, 7);
				} else if (group.getFromGroup() != null) {
					g.setColor(Color.BLACK);
					g.fillOval(x1 - 3, y - 4, 7, 7);
				}




				if(isBlind) break;
			}
		}


	}

	private Phase getNext(Phase phase) {
		if (nextPhaseMap == null) {
			nextPhaseMap = new HashMap<Phase, Phase>();
			Phase previous = null;
			for (Phase p : study.getPhases()) {
				if (previous != null) {
					nextPhaseMap.put(previous, p);
				}
				previous = p;
			}
			if (previous != null)
				nextPhaseMap.put(previous, null);
		}

		return nextPhaseMap.get(phase);
	}

	protected void paintPhase(Graphics2D g, Phase phase, boolean even) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date now = cal.getTime();

		try {
			int x = getX(phase);

			int toY = maxY - 10;

			// Find nextPhases
			Phase nextPhase = getNext(phase);

			int x2 = nextPhase == null ? maxX : getX(nextPhase);
			Date date = phase.getAbsoluteDate();

			// Background: yellow if the absolute date is today
			if (!forRevision && date != null && Phase.isSameDay(date, now)) {
				g.setColor(UIUtils.getColor(255, 255, 0, 60));
				g.fillRect(x - phaseWidth/2, 1, x2 - x, toY - 1);
			}

			AffineTransform normal = g.getTransform();
			Color c = g.getColor();

			int y = OFFSET_Y + 3;
			AffineTransform rotated = new AffineTransform();
			rotated.setToRotation(-Math.PI / 2, x + 3, y);
			g.setTransform(rotated);

			// Draw phase.shortname
			boolean on2lines = x2-x>19;
			int y1 = on2lines? y-4: y;
			String s = phase.getShortName().replace("_", " ");
			g.setColor(Color.BLACK);
			g.setFont(FastFont.BOLD);
			g.drawString(s, x, y1);
			int w = g.getFontMetrics().stringWidth(s)+2;


			// format date
			if (date != null) {
				calendar.setTime(date);
				int d = calendar.get(Calendar.DAY_OF_WEEK);
				s = FormatterUtils.formatDate(date) + " " + new String[] { "", "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" }[d];
			} else {
				s = "";
			}

			g.setColor(Color.DARK_GRAY);
			if(on2lines) {
				//2nd line: write label
				g.setFont(FastFont.MEDIUM);
				g.drawString(s, x, y+FastFont.MEDIUM.getSize()-6);
				//1st line: write date+day
				g.setFont(FastFont.SMALL);
				if (phase.getLabel() != null && phase.getLabel().length()>0) {
					s = phase.getLabel();
					if(s.length()>10) s = s.substring(0, 10);
					g.setColor(Color.BLACK);
					g.drawString(s, x+w, y1);
					w+= g.getFontMetrics().stringWidth(s)+3;
				}
			} else {
				//1 line: write date+day only
				g.setFont(FastFont.SMALL);
				g.drawString(s, x+w, y1);
			}


			g.setColor(c);
			g.setTransform(normal);

			// Draw vertical line
			g.setColor(UIUtils.getColor(210, 210, 210));
			g.drawLine(x, OFFSET_Y + 9, x, toY);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void paintAction(Graphics2D g, StudyAction action) {
		if (action == null) return;
		Group group = action.getGroup();
		int subgroupNo = action.getSubGroup();
		Phase phase = action.getPhase();
		int arrowSize = FastFont.getDefaultFontSize()/4;

		Shape clip = g.getClip();
		try {
			int y = getY(group, subgroupNo);
			int x = getX(phase);
			Phase nextPhase = getNext(phase);
			int x2 = nextPhase == null ? maxX : getX(nextPhase);


			// Paint the measurement
			String abbr = action.getMeasurementAbbreviations();
			if(abbr.length()>0) {
				g.setFont(FastFont.SMALL.deriveFont(AffineTransform.getScaleInstance(.8, 1)));
				AffineTransform transform = AffineTransform.getRotateInstance(-Math.PI / 2, x, y);
				g.setTransform(transform);
				int px = x-g.getFontMetrics().stringWidth(abbr)-1;
				//				g.setColor(Color.WHITE);
				//				g.drawString(abbr, px-1, y-3);
				g.setColor(UIUtils.getColor(0, 60, 220));
				g.drawString(abbr, px, y-4);
				g.setTransform(AffineTransform.getTranslateInstance(0, 0));
			}

			if(isBlindAll) return;
			// Paint the treatment
			g.setFont(FastFont.SMALL);
			if (action.getNamedTreatment() != null) {
				// Get next action with treatment
				int nextXWithTreatment = getWidth();
				for (Phase p = getNext(phase); p != null; p = getNext(p)) {
					StudyAction a = study.getStudyAction(group, subgroupNo, p);
					if (a != null && a.getNamedTreatment() != null) {
						nextXWithTreatment = getX(p);
						break;
					}
				}
				g.setColor(Color.BLUE);

				Color c = action.getNamedTreatment().getColor();

				// Draw Treatment Arrow
				g.setColor(c==null?Color.BLUE: c);
				g.fillPolygon(new int[] { x, x - arrowSize, x + arrowSize }, new int[] { y, y - arrowSize*3, y - arrowSize*3 }, 3);

				// Draw Treatment Name (if not same treatment than before)
				g.setColor(UIUtils.getDilutedColor(Color.BLACK, g.getColor()));
				Rectangle r = new Rectangle(x - arrowSize, y - getHeight(group, subgroupNo), nextXWithTreatment - x, getHeight(group, subgroupNo) * 2);
				g.setClip(r);

				String s = action.getNamedTreatment().getName();
				if (s == null) s = "NoName";
				g.drawString(s, x + arrowSize, y - 2);
			}
			g.setClip(clip);

			if(isBlind) return;

			// Paint Sampling
			if ((action.getNamedSampling1() != null || action.getNamedSampling2() != null)) {
				// Get next action with sampling
				int nextXWithSampling = getWidth();
				for (Phase p = getNext(phase); p != null; p = getNext(p)) {
					StudyAction a = study.getStudyAction(group, subgroupNo, p);
					if (a != null && (a.getNamedSampling1() != null || a.getNamedSampling2() != null)) {
						nextXWithSampling = getX(p);
						break;
					}
				}
				g.setClip(new Rectangle(x - arrowSize, y - getHeight(group, subgroupNo), nextXWithSampling - x, getHeight(group, subgroupNo) * 2));

				// Draw Sampling Arrow
				g.setColor(Color.RED);
				g.fillPolygon(new int[] { x - arrowSize, x, x + arrowSize }, new int[] { y + 1, y + arrowSize*3, y + 1 }, 3);
			}

			y = getSamplingY(group, subgroupNo);
			if (action.getNamedSampling1() != null) {
				String s = action.getNamedSampling1().getName() + (action.getNamedSampling1().getStudy() != null && !study.equals(action.getNamedSampling1().getStudy()) ? "(" + action.getNamedSampling1().getStudy() + ")" : "");

				g.setColor(Color.RED);
				g.drawString(s, x + arrowSize, y);
				y += g.getFont().getSize();
			}
			if (action.getNamedSampling2() != null) {
				String s = action.getNamedSampling2().getName() + (action.getNamedSampling2().getStudy() != null && !study.equals(action.getNamedSampling2().getStudy()) ? "(" + action.getNamedSampling2().getStudy() + ")" : "");
				g.setColor(Color.RED);
				g.drawString(s, x + arrowSize, y);
				y += g.getFont().getSize();
			}

			// Paint Label
			g.setClip(new Rectangle(x - arrowSize, y - getHeight(group, subgroupNo), x2 - x, getHeight(group, subgroupNo) * 2));
			if (action.getLabel() != null) {
				g.setColor(Color.DARK_GRAY);
				String s = action.getLabel();
				g.drawString(s, x + arrowSize, y);
				y += g.getFont().getSize();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		g.setClip(clip);

	}

	public void setPrintLegend(boolean printLegend) {
		this.printLegend = printLegend;
	}

	public boolean isPrintLegend() {
		return printLegend;
	}

	public static BufferedImage getImage(Study study, int maxWidth, int maxHeight) {
		return getImage(study, maxWidth, maxHeight, 1);
	}

	public static BufferedImage getImage(Study study, int maxWidth, int maxHeight, int sizeFactor) {
		StudyDepictor depictor = new StudyDepictor();
		depictor.setPrintLegend(false);
		depictor.setSize(maxWidth, maxHeight);
		depictor.setStudy(study);
		depictor.setSizeFactor(sizeFactor);
		Dimension dim = depictor.getPreferredSize();
		BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		depictor.paintComponent(g);
		g.dispose();

		Image tmp = img;

		//Calculate newWidth
		int newWidth = Math.min(dim.width, maxWidth);
		int newHeight = Math.min(dim.height, maxHeight);

		//Keep aspect Ratio
		double ratio = 1.0*dim.width/dim.height;

		if(newWidth>newHeight*ratio) {
			newWidth = (int)(newHeight*ratio);
		} else {
			newHeight = (int)(newWidth/ratio);
		}

		if (img.getWidth() > maxWidth || img.getHeight() > maxHeight) {
			tmp = img.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH);
		}

		BufferedImage res = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g2 = res.getGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, newWidth, newHeight);
		g2.drawImage(tmp, 0, 0, depictor);
		g2.setColor(Color.BLACK);
		g2.drawRect(0, 0, newWidth-1, newHeight-1);
		g2.dispose();
		return res;

	}
}
