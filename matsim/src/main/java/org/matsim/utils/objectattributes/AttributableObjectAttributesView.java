package org.matsim.utils.objectattributes;

import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.HashMap;
import java.util.Map;

public class AttributableObjectAttributesView implements ObjectAttributes {
	private final Map<String,Attributable> attributables = new HashMap<>();

	public AttributableObjectAttributesView(Map<?,? extends Attributable> attributables) {
		for ( Map.Entry<?,? extends Attributable> entry : attributables.entrySet() ) {
			this.attributables.put(
					entry.getKey().toString(),
					entry.getValue() );
		}
	}

	@Override
	public Object putAttribute(String objectId, String attribute, Object value) {
		return attributables.get( objectId ).getAttributes().putAttribute( attribute , value );
	}

	@Override
	public Object getAttribute(String objectId, String attribute) {
		return attributables.get( objectId ).getAttributes().getAttribute( attribute );
	}

	@Override
	public Object removeAttribute(String objectId, String attribute) {
		return attributables.get( objectId ).getAttributes().removeAttribute( attribute );
	}

	@Override
	public void removeAllAttributes(String objectId) {
		attributables.get( objectId ).getAttributes().clear();
	}

	@Override
	public void clear() {
		for ( Attributable att : attributables.values() ) {
			att.getAttributes().clear();
		}
	}
}
