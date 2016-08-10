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

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class LocationBrowser extends JPanel {

	public static final String PROPERTY_LOCATION_SELECTED = "location_selected";
	public static enum LocationBrowserFilter {
		ALL,
		RACKS,
		CONTAINER
	}
	
	private LocationBrowserFilter filter;
	private CardLayout cardLayout = new CardLayout();
	private Location location = null;
	
	private JCustomTextField locationTextField = new JCustomTextField();
	private List<LocationComboBox> locationComboBoxes = new ArrayList<LocationComboBox>();
	
	private boolean allowTextEditing = true;
	private JPanel textPanel = new JPanel(new BorderLayout());
	private JPanel comboPanel = new JPanel(null) {
		@Override
		public void doLayout() {
			int width = getWidth();
			if(width<=0) return;
			
			int x = 0;
			int maxX = 0;
			int y = 0;
			boolean first = true;
			for (int i = 0; i < locationComboBoxes.size(); i++) {
				LocationComboBox c = locationComboBoxes.get(i);
				Location l = c.getSelection();
				int w = l==null?50: getFontMetrics(getFont()).stringWidth(l.getName())+36;
				
				if(first || x+w+5<width) {			
					c.setBounds(x, y, w, 24);				
					x+=w-2;
					first = false;
				} else {
					y+=24;
					x=0;
					c.setBounds(x, y, w, 24);				
					x+=w-2;
				}
				maxX = Math.max(maxX, x);
			}	
			layoutSize.width = width;
			layoutSize.height = y+26;
		}
	};
	
	public LocationBrowser() {
		this(LocationBrowserFilter.ALL);
	}
	
	
	public LocationBrowser(LocationBrowserFilter filter) {
		super();
		this.filter = filter;
		setLayout(cardLayout);
		setOpaque(true);
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));

		locationTextField.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
		locationTextField.setFont(FastFont.REGULAR);
		locationTextField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					updateBioLocation(locationTextField.getText());
				} catch(Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		
		comboPanel.setBackground(Color.WHITE);
		textPanel.setOpaque(false);
		comboPanel.setOpaque(false);
		add("text", textPanel);
		add("combo", comboPanel);
		cardLayout.show(LocationBrowser.this, "combo");
		
		textPanel.add(BorderLayout.CENTER, locationTextField);
		
		MouseListener ma = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(isEnabled() && allowTextEditing) {					
					cardLayout.show(LocationBrowser.this, "text");
					locationTextField.setText(location==null?"": location.getHierarchyFull());
					locationTextField.selectAll();
					locationTextField.requestFocusInWindow();
				}
			}
		};
		FocusListener fl = new FocusListener() {			
			@Override
			public void focusLost(FocusEvent e) {
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				Color c = UIUtils.getColor(115,164,209);
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(c, 1), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));	
			}
		}; 
		
		final AWTEventListener listener = new AWTEventListener() {			
			@Override
			public void eventDispatched(AWTEvent event) {
				if(((MouseEvent)event).getID()==MouseEvent.MOUSE_CLICKED) {
					if(event.getSource()!=locationTextField && locationTextField.isFocusOwner()) {
						try {
							updateBioLocation(locationTextField.getText());					
						} catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		};
		
		
		locationTextField.addFocusListener(new FocusListener() {			
			@Override
			public void focusGained(FocusEvent e) {
				Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);
			}
			@Override
			public void focusLost(FocusEvent e) {
				try {
					updateBioLocation(locationTextField.getText());					
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
			}
		});
		
		comboPanel.addMouseListener(ma);
		locationTextField.addFocusListener(fl);
		updateView();
	}
	
	public void setAllowTextEditing(boolean allowTextEditing) {
		this.allowTextEditing = allowTextEditing;
	}
	public boolean isAllowTextEditing() {
		return allowTextEditing;
	}
	
	@Override
	public void setFont(Font font) {
		super.setFont(font);
		if(locationTextField!=null) locationTextField.setFont(font);
	}
	
	private void updateBioLocation(String fullLocation) throws Exception {
		fullLocation = fullLocation.replaceAll("[\n\r]", "");
		cardLayout.show(LocationBrowser.this, "combo");
		if(fullLocation.length()==0) {
			setBioLocation(null);
		} else {
			Location loc = DAOLocation.getCompatibleLocation(fullLocation, Spirit.getUser());
			if(loc==null) throw new Exception("Invalid location: "+fullLocation);
			setBioLocation(loc);
		}
		LocationBrowser.this.firePropertyChange(PROPERTY_LOCATION_SELECTED, null, location);
	}
	
	@Override
	public Dimension getPreferredSize() {
		comboPanel.doLayout();
		Dimension minSize = getMinimumSize();
		Dimension dim = new Dimension(Math.max(layoutSize.width + 2, minSize.width), Math.max(layoutSize.height+2, minSize.height));		
		return dim;
	}
		
	
	private Dimension layoutSize = new Dimension(220, 26);
	
	private void updateView() {
		comboPanel.removeAll();		
		locationComboBoxes.clear();
		List<Location> hierarchy = new ArrayList<Location>();
		if(location!=null) {
			location = JPAUtil.reattach(location);
			hierarchy = location.getHierarchy();
		}
		
		Location parent = null;
		for (int i = 0; i < hierarchy.size()+1; i++) {
			final Location parentFinal = parent;
			
			//Find possible choices
			List<Location> nextChildren = new ArrayList<Location>();
			for (Location l : parent==null? DAOLocation.getLocationRoots(): parent.getChildren()) {
				if(!SpiritRights.canRead(l, Spirit.getUser())) continue;
				if(filter==LocationBrowserFilter.CONTAINER && l.getLocationType().getPositionType()!=LocationLabeling.NONE) continue;
				if(filter==LocationBrowserFilter.RACKS && l.getLocationType().getPositionType()!=LocationLabeling.NONE && l.getLocationType()!=LocationType.RACK) continue;
				
				nextChildren.add(l);
			}
			
			if(nextChildren.size()==0) break;
						
			Collections.sort(nextChildren);
			Location sel = i<hierarchy.size()? hierarchy.get(i): null;
			final LocationComboBox locComboBox = new LocationComboBox(nextChildren);
			locComboBox.setFont(getFont());
			
			locComboBox.setSelection(sel);
			locComboBox.addPropertyChangeListener(LocationComboBox.PROPERTY_TEXTCHANGED, new PropertyChangeListener() {				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					//New location selected
					if(locComboBox.getSelection()==null) {
						location = parentFinal;
					} else {
						location = locComboBox.getSelection();
					}
					updateView();
					LocationBrowser.this.firePropertyChange(PROPERTY_LOCATION_SELECTED, null, location);
					if(getParent()!=null && getParent().getParent() instanceof JScrollPane) {
						final JScrollPane sp = (JScrollPane) getParent().getParent();
						SwingUtilities.invokeLater(new Runnable() {							
							@Override
							public void run() {
								sp.getHorizontalScrollBar().setValue(sp.getHorizontalScrollBar().getMaximum());
							}
						});
					}
				}
			});
			locationComboBoxes.add(locComboBox);
			comboPanel.add(locComboBox);
			
			nextChildren = new ArrayList<Location>();
			parent = sel;
		}
		
		comboPanel.validate();
		repaint();
	}
	
	public void setBioLocation(Location location) {
		this.location = location;
		updateView();
	}
	
	public Location getBioLocation() {
		return location;
	}
	
	
}