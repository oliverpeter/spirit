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

package com.actelion.research.spiritcore.business.biosample;

import java.util.Arrays;
import java.util.List;

import com.actelion.research.spiritcore.util.MiscUtils;

public class ActionComments extends ActionBiosample {
		
	public ActionComments() {}
	
	public ActionComments(String comments) {
		setComments(comments);
	}	
	
	@Override
	public String serialize() {	
		return MiscUtils.serializeStrings(Arrays.asList(new String[]{"Comments", comments}));
	}

	public static ActionComments deserialize(List<String> strings) {
		assert strings.size()==2;
		assert strings.get(0).equals("Comments");
		return new ActionComments(strings.get(0)); 
	}
}