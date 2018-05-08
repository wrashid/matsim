package org.matsim.utils.objectattributes.attributable;

/**
 * @author thibautd
 */
public interface Attributable {
	default Attributes getAttributes() {
		return AttributesGlobalStore.getAttributes(this);
	}
}
