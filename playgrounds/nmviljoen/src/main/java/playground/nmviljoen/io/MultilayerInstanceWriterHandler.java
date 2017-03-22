/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.nmviljoen.io;

import java.io.BufferedWriter;
import java.io.IOException;

public interface MultilayerInstanceWriterHandler {
	
	/* <sets> ... </sets> */
	public void startSets(final BufferedWriter out) throws IOException;
	public void endSets(final BufferedWriter out) throws IOException;
	
	/* <set> ... </set> */
	public void startSet(final BufferedWriter out) throws IOException;
	public void endSet(final BufferedWriter out) throws IOException;
	
	/* <path> ... </path> */
	public void startPath(final BufferedWriter out) throws IOException;
	public void endPath(final BufferedWriter out) throws IOException;
	
	/* <node> ... </node> */
	public void startNode(final BufferedWriter out) throws IOException;
	public void endNode(final BufferedWriter out) throws IOException;
	
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}

