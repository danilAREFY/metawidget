// Metawidget
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

'use strict';

/**
 * WidgetBuilders.
 */

var metawidget = metawidget || {};
metawidget.widgetbuilder = metawidget.widgetbuilder || {};

//
// CompositeWidgetBuilder
//

metawidget.widgetbuilder.CompositeWidgetBuilder = function( widgetBuilders ) {

	if ( ! ( this instanceof metawidget.widgetbuilder.CompositeWidgetBuilder ) ) {
		throw new Error( "Constructor called as a function" );
	}

	this.onStartBuild = function() {

		for ( var loop = 0, length = widgetBuilders.length; loop < length; loop++ ) {

			var widgetBuilder = widgetBuilders[loop];

			if ( widgetBuilder.onStartBuild ) {
				widgetBuilder.onStartBuild();
			}
		}
	};

	this.buildWidget = function( attributes, mw ) {

		for ( var loop = 0, length = widgetBuilders.length; loop < length; loop++ ) {

			var widget;
			var widgetBuilder = widgetBuilders[loop];

			if ( widgetBuilder.buildWidget ) {
				widget = widgetBuilder.buildWidget( attributes, mw );
			} else {
				widget = widgetBuilder( attributes, mw );
			}

			if ( widget ) {
				return widget;
			}
		}
	};

	this.onEndBuild = function() {

		for ( var loop = 0, length = widgetBuilders.length; loop < length; loop++ ) {

			var widgetBuilder = widgetBuilders[loop];

			if ( widgetBuilder.onEndBuild ) {
				widgetBuilder.onEndBuild();
			}
		}
	};
};

//
// ReadOnlyWidgetBuilder
//

metawidget.widgetbuilder.ReadOnlyWidgetBuilder = function() {

	if ( ! ( this instanceof metawidget.widgetbuilder.ReadOnlyWidgetBuilder ) ) {
		throw new Error( "Constructor called as a function" );
	}
};

metawidget.widgetbuilder.ReadOnlyWidgetBuilder.prototype.buildWidget = function( attributes, mw ) {

	// Not read-only?

	if ( !metawidget.util.isReadOnly( attributes, mw ) ) {
		return;
	}

	// Hidden

	if ( attributes.hidden == 'true' ) {
		return document.createElement( 'stub' );
	}

	if ( attributes.type == 'string' ) {
		return document.createElement( 'output' );
	}
	
	// Not simple, but don't expand
	
	if ( attributes.dontExpand == 'true' ) {
		return document.createElement( 'output' );
	}	
};

//
// HtmlWidgetBuilder
//

metawidget.widgetbuilder.HtmlWidgetBuilder = function() {

	if ( ! ( this instanceof metawidget.widgetbuilder.HtmlWidgetBuilder ) ) {
		throw new Error( "Constructor called as a function" );
	}
};

metawidget.widgetbuilder.HtmlWidgetBuilder.prototype.buildWidget = function( attributes, mw ) {

	// Hidden

	if ( attributes.hidden == 'true' ) {
		return document.createElement( 'stub' );
	}

	// Select box

	if ( attributes.lookup ) {
		var select = document.createElement( 'select' );

		if ( !attributes.required || attributes.required == 'false' ) {
			select.appendChild( document.createElement( 'option' ) );
		}

		var lookupSplit = attributes.lookup.split( ',' );

		for ( var loop = 0, length = lookupSplit.length; loop < length; loop++ ) {
			var option = document.createElement( 'option' );

			if ( attributes.lookupLabels ) {
				option.setAttribute( 'value', lookupSplit[loop] );
				option.innerHTML = attributes.lookupLabels.split( ',' )[loop];
			} else {
				option.innerHTML = lookupSplit[loop];

			}

			select.appendChild( option );
		}
		return select;
	}

	// Action

	if ( attributes.type == 'function' ) {
		var button = document.createElement( 'button' );

		if ( attributes.label ) {
			button.innerHTML = attributes.label;
		} else {
			button.innerHTML = metawidget.util.uncamelCase( attributes.name );
		}
		return button;
	}

	// Number

	if ( attributes.type == 'number' ) {

		if ( attributes.minimumValue && attributes.maximumValue ) {
			var range = document.createElement( 'input' );
			range.setAttribute( 'type', 'range' );
			range.setAttribute( 'min', attributes.minimumValue );
			range.setAttribute( 'max', attributes.maximumValue );
			return range;
		}

		var number = document.createElement( 'input' );
		number.setAttribute( 'type', 'number' );
		return number;
	}

	// Boolean

	if ( attributes.type == 'boolean' ) {
		var checkbox = document.createElement( 'input' );
		checkbox.setAttribute( 'type', 'checkbox' );
		return checkbox;
	}

	// Date
	
	if ( attributes.type == 'date' ) {
		var date = document.createElement( 'input' );
		date.setAttribute( 'type', 'date' );
		return date;
	}

	// String

	if ( attributes.type == 'string' ) {

		if ( attributes.masked == 'true' ) {
			var password = document.createElement( 'input' );
			password.setAttribute( 'type', 'password' );
			
			if ( attributes.maximumLength ) {
				password.setAttribute( 'maxlength', attributes.maximumLength );
			}
			
			return password;
		}

		if ( attributes.large == 'true' ) {
			return document.createElement( 'textarea' );
		}

		var text = document.createElement( 'input' );
		text.setAttribute( 'type', 'text' );
		
		if ( attributes.maximumLength ) {
			text.setAttribute( 'maxlength', attributes.maximumLength );
		}
		
		return text;
	}
	
	// Not simple, but don't expand
	
	if ( attributes.dontExpand == 'true' ) {
		var text = document.createElement( 'input' );
		text.setAttribute( 'type', 'text' );
		return text;		
	}
};