/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.taxi.run;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;

/**
 * @author  jbischoff
 *
 */
public class MultipleMultiRunBerlinTaxiLauncher {
public static void main(String[] args) {
	String dir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/av_simulation";
//	String[] subdirs = {"11000","12000","13000","14000","15000"};
	String[] subdirs = {"16000","17000","18000","19000"};
	for (int i = 0; i<subdirs.length;i++ ){
		MultiRunBerlinTaxiLauncher.main(dir,subdirs[i]);
	}
}
}
