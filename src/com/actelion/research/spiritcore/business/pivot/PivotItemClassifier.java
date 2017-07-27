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

package com.actelion.research.spiritcore.business.pivot;

import java.awt.Color;

public enum PivotItemClassifier {
	STUDY_GROUP("Study", new Color(255, 200, 200)),
	STUDY_PHASE("Phase", new Color(245, 200, 200)),
	LOCATION("Location", new Color(240, 240, 225)),
	PARTICIPANT("Participant", new Color(180, 240, 255)),
	BIOSAMPLE("Sample", new Color(180, 240, 255)),
	//		COMPOUND("Compound", new Color(210, 210, 240)),
	RESULT("Result", new Color(255, 255, 180));

	private final String label;
	private final Color bgcolor;
	private PivotItemClassifier(String label, Color bgcolor) {
		this.label = label;
		this.bgcolor = bgcolor;
	}

	public String getLabel() {return label; }
	public Color getBgcolor() {return bgcolor;}
	@Override
	public String toString() {return label;}
}