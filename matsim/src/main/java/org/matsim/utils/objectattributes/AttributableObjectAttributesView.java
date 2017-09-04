package org.matsim.utils.objectattributes;

import org.matsim.utils.objectattributes.attributable.Attributable;

public abstract class AttributableObjectAttributesView implements ObjectAttributes {
	@Override
	public Object putAttribute(String objectId, String attribute, Object value) {
		return get( objectId ).getAttributes().putAttribute( attribute , value );
	}

	@Override
	public Object getAttribute(String objectId, String attribute) {
		return get( objectId ).getAttributes().getAttribute( attribute );
	}

	@Override
	public Object removeAttribute(String objectId, String attribute) {
		return get( objectId ).getAttributes().removeAttribute( attribute );
	}

	@Override
	public void removeAllAttributes(String objectId) {
		get( objectId ).getAttributes().clear();
	}

	@Override
	public void clear() {
		for ( Attributable att : getAll() ) {
			att.getAttributes().clear();
		}
	}

	protected abstract Iterable<? extends Attributable> getAll();

	protected abstract Attributable get(String objectId);
}
