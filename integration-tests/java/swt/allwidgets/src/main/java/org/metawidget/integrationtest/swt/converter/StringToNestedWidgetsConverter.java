// Metawidget
//
// This file is dual licensed under both the LGPL
// (http://www.gnu.org/licenses/lgpl-2.1.html) and the EPL
// (http://www.eclipse.org/org/documents/epl-v10.php). As a
// recipient of Metawidget, you may choose to receive it under either
// the LGPL or the EPL.
//
// Commercial licenses are also available. See http://metawidget.org
// for details.

package org.metawidget.integrationtest.swt.converter;

import org.eclipse.core.databinding.conversion.Converter;
import org.metawidget.integrationtest.shared.allwidgets.model.AllWidgets.NestedWidgets;
import org.metawidget.util.ArrayUtils;

/**
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

public class StringToNestedWidgetsConverter
	extends Converter {

	//
	// Constructor
	//

	public StringToNestedWidgetsConverter() {

		super( String.class, NestedWidgets.class );
	}

	//
	// Public methods
	//

	public Object convert( Object toConvert ) {

		String[] values = ArrayUtils.fromString( (String) toConvert );

		if ( values.length == 0 ) {
			return null;
		}

		NestedWidgets nestedWidgets = new NestedWidgets();
		nestedWidgets.setNestedTextbox1( values[0] );

		if ( values.length > 1 ) {
			nestedWidgets.setNestedTextbox2( values[1] );
		}

		return nestedWidgets;
	}
}
