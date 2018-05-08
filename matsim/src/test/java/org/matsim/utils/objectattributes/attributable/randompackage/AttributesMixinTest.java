package org.matsim.utils.objectattributes.attributable.randompackage;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Tests that Attributable with default method and package private store works
 * @author thibautd
 */
public class AttributesMixinTest {
	private static class MyAttributable implements Attributable {}

	@Test
	public void testMixin() {
		MyAttributable a = new MyAttributable();
		a.getAttributes().putAttribute("some name", "some value");
		Assert.assertEquals("could not get attribute back",
				a.getAttributes().getAttribute("some name"),
				"some value");
	}
}
