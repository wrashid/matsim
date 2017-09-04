package org.matsim.utils.objectattributes;

import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * A simple helper class to store arbitrary attributes (identified by Strings) for
 * arbitrary objects (identified by String-Ids). Note that this implementation uses
 * large amounts of memory for storing many attributes for many objects, it is not
 * heavily optimized.
 * <p></p>
 * <em>This class is not thread-safe.</em>
 * <p></p>
 * More information can be found in the package's Javadoc.
 * <p></p>
 * Example(s):<ul>
 * <li> {@link RunObjectAttributesExample}
 * </ul>
 * @author mrieser
 */
public interface ObjectAttributes extends MatsimExtensionPoint {
	Object putAttribute(String objectId, String attribute, Object value);

	Object getAttribute(String objectId, String attribute);

	Object removeAttribute(String objectId, String attribute);

	void removeAllAttributes(String objectId);

	void clear();
}
