/* *********************************************************************** *
 * project: org.matsim.*
 * CentralitySpeedTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.matrix;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import playground.johannes.sna.graph.SparseVertex;
import playground.johannes.sna.graph.matrix.AdjacencyMatrix;
import playground.johannes.sna.graph.spatial.SpatialSparseGraph;
import playground.johannes.sna.graph.spatial.io.SpatialGraphMLReader;


/**
 * @author illenberger
 *
 */
public class CentralitySpeedTest extends TestCase {
//	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger logger = Logger.getLogger(CentralitySpeedTest.class);
	
//	@Test
	public void test() {
//		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphFactory());
//		logger.info("Generating grah...");
//		SparseGraph graph = generator.generate(1000, 0.1, 4711);
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialSparseGraph graph = reader.readGraph("/Volumes/hertz:ils-raid/socialnets/mcmc/runs/run45/output/2000000000/graph.graphml"); 
		logger.info("Converting matrix...");
		AdjacencyMatrix<SparseVertex> y = new AdjacencyMatrix<SparseVertex>(graph);
		
		logger.info("Calculation centrality measures...");
		long time = System.currentTimeMillis();
		MatrixCentrality c = new MatrixCentrality();
		c.run(y);
		logger.info("Done. Took " + (System.currentTimeMillis() - time) + " ms");
		
		logger.info(String.format("Mean closeness is %1$s.", c.getMeanVertexCloseness()));
	}
}