package org.matsim.utils.objectattributes.attributable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
final class AttributesGlobalStore {
	// this is only for first experiments.
	// This should be:
	// - SoftHashMap, so that mapping can be GCed if object GCed
	// - Not store attributes, but mappings directly (here, still need for one Attributes per object)
	private static final Map<Object, Attributes> map = new HashMap<>();

	private AttributesGlobalStore() {}

	static <T extends Attributable> Attributes getAttributes(final T attributed) {
		return map.computeIfAbsent(attributed, o -> new Attributes());
	}
}
