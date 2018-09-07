/* *********************************************************************** *
 * project: org.matsim.*
 * NonFlatConfigReader.java
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
package org.matsim.core.config;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

/**
 * @author thibautd
 */
 class ConfigReaderMatsimV2 extends MatsimXmlParser {
	private final Config config;

	private final Deque<ConfigGroup> moduleStack = new ArrayDeque<ConfigGroup>();

	ConfigReaderMatsimV2(
			final Config config) {
		this.config = config;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( ConfigV2XmlNames.MODULE ) ) {
			startModule(atts);
		}
		else if ( name.equals( ConfigV2XmlNames.PARAMETER_SET ) ) {
			startParameterSet(atts);
		}
		else if ( name.equals( ConfigV2XmlNames.PARAMETER ) ) {
			startParameter(atts);
		}
		else if ( !name.equals( "config" ) ) {
			// this is the job of the dtd validation,
			// but better too much safety than too little...
			throw new IllegalArgumentException( "unkown tag "+name );
		}
	}

	private void startParameter(final Attributes atts) {
		moduleStack.getFirst().addParam(
				atts.getValue( ConfigV2XmlNames.NAME ),
				atts.getValue( ConfigV2XmlNames.VALUE ) );
	}

	private void startParameterSet(final Attributes atts) {
		final ConfigGroup m = moduleStack.getFirst().createParameterSet( atts.getValue( ConfigV2XmlNames.TYPE ) );
		moduleStack.addFirst( m );
	}

	private void startModule(final Attributes atts) {
		final ConfigGroup m = config.getModule( atts.getValue( ConfigV2XmlNames.NAME ) );
		moduleStack.addFirst(
				m == null ?
				config.createModule( atts.getValue( ConfigV2XmlNames.NAME ) ) :
				m );
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( ConfigV2XmlNames.MODULE ) || name.equals( ConfigV2XmlNames.PARAMETER_SET ) ) {
			final ConfigGroup head = moduleStack.removeFirst();
			
			if ( !moduleStack.isEmpty() ) moduleStack.getFirst().addParameterSet( head );
		}
	}
}

