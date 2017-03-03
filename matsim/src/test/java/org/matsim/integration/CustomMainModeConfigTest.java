/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.integration;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Test for section 4.6 Other Modes than Car of "the book" (latest, downloaded on 2017-01-30). 
 * The idea is to specify a second qsim mode "truck" and the corresponding routing option 
 * via config and other input files. Legs with mode="truck" are expected to be routed on 
 * the links of the example equil network. The plans2.xml for this test contains such legs. 
 * The "truck" mode is added to the qsim and planscalcroute modules of the config. 
 * 
 * 
 * @author dgrether / TWT GmbH Science & Innovation
 */
public class CustomMainModeConfigTest
{
  @Rule
  public MatsimTestUtils utils = new MatsimTestUtils();
  
  @Test
  public void testFromCodeConfig() {
    URL configUrl = IOUtils.newUrl(utils.classInputResourcePath(), "config.xml");
    Config config = ConfigUtils.loadConfig(configUrl);
    config.network().setInputFile(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());
    config.plans().setInputFile(IOUtils.newUrl(utils.classInputResourcePath(), "plans2.xml").toString());
    config.controler().setOutputDirectory( utils.getOutputDirectory() );
    config.controler().setCreateGraphs( false );
    final Controler controler = new Controler(config);
    controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    controler.run();
  }
}
