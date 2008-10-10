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

package org.metawidget.faces;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.Application;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.metawidget.util.ArrayUtils;

/**
 * Utilities for working with Java Server Faces.
 *
 * @author Richard Kennard
 */

@SuppressWarnings( "deprecation" )
public final class FacesUtils
{
	//
	// Public statics
	//

	/**
	 * Return <code>true</code> if the specified value conforms to the
     * syntax requirements of a value binding expression.
     * <p>
     * This method is a mirror of the one in <code>UIComponentTag.isValueReference</code>, but
     * that one is deprecated so may be removed in the future.
     *
     * @param value The value to evaluate
     *
     * @throws NullPointerException if <code>value</code> is
     *  <code>null</code>
	 */

	public static boolean isValueReference( String value )
	{
		return PATTERN_VALUE_EXPRESSION.matcher( value ).matches();
	}

	/**
	 * @return the original String, not wrapped in #{...}. If the original String was not wrapped,
	 *         returns the original String
	 */

	public static String unwrapValueReference( String value )
	{
		Matcher matcher = PATTERN_VALUE_EXPRESSION.matcher( value );

		if ( !matcher.matches() )
			return value;

		return matcher.group( 3 );
	}

	/**
	 * @return the original String, wrapped in #{...}. If the original String was already wrapped,
	 *         returns the original String
	 */

	public static String wrapValueReference( String value )
	{
		if ( isValueReference( value ))
			return value;

		return VALUE_EXPRESSION_START + unwrapValueReference( value ) + VALUE_EXPRESSION_END;
	}

	/**
	 * Finds the child component of the given component that is both rendered and has the given
	 * value expression.
	 * <p>
	 * Note: this method does <em>not</em> recurse into sub-children.
	 */

	public static UIComponent findRenderedComponentWithValueBinding( UIComponent component, String expressionString )
	{
		// Try to find a child...

		@SuppressWarnings( "unchecked" )
		List<UIComponent> children = component.getChildren();

		for ( UIComponent child : children )
		{
			// ...with the binding we're interested in

			ValueBinding childValueBinding = child.getValueBinding( "value" );

			if ( childValueBinding == null )
				continue;

			// (note: ValueBinding.equals() does not compare expression strings)

			if ( expressionString.equals( childValueBinding.getExpressionString() ) )
			{
				if ( child.isRendered() )
					return child;
			}
		}

		return null;
	}

	/**
	 * Finds the child component of the given component that is both rendered and has the given
	 * method expression.
	 * <p>
	 * Note: this method does <em>not</em> recurse into sub-children.
	 */

	public static UIComponent findRenderedComponentWithMethodBinding( UIComponent component, String expressionString )
	{
		// Try to find a child...

		@SuppressWarnings( "unchecked" )
		List<UIComponent> children = component.getChildren();

		for ( UIComponent child : children )
		{
			if ( !( child instanceof ActionSource ))
				continue;

			// ...with the binding we're interested in

			MethodBinding childMethodBinding = ((ActionSource) child).getAction();

			if ( childMethodBinding == null )
				continue;

			// (note: MethodBinding.equals() does not compare expression strings)

			if ( expressionString.equals( childMethodBinding.getExpressionString() ) )
			{
				if ( child.isRendered() )
					return child;
			}
		}

		return null;
	}

	public static UIParameter findParameterWithName( UIComponent component, String name )
	{
		// Try to find a child parameter...

		@SuppressWarnings( "unchecked" )
		List<UIComponent> children = component.getChildren();

		for ( UIComponent child : children )
		{
			if ( !( child instanceof UIParameter ) )
				continue;

			// ...with the name we're interested in

			UIParameter parameter = (UIParameter) child;

			if ( name.equals( parameter.getName() ) )
				return parameter;
		}

		return null;
	}

	public static void render( FacesContext context, UIComponent component )
		throws IOException
	{
		if ( !component.isRendered() )
			return;

		component.encodeBegin( context );

		if ( component.getRendersChildren() )
			component.encodeChildren( context );
		else
			renderChildren( context, component );

		component.encodeEnd( context );
	}

	@SuppressWarnings( "unchecked" )
	public static void copyAttributes( UIComponent from, UIComponent to )
	{
		to.getAttributes().putAll( from.getAttributes() );
	}

	public static void copyParameters( UIComponent from, UIComponent to, String... exclude )
	{
		FacesContext context = FacesContext.getCurrentInstance();
		Application application = context.getApplication();
		UIViewRoot viewRoot = context.getViewRoot();

		// For each child parameter...

		@SuppressWarnings( "unchecked" )
		List<UIComponent> fromChildren = from.getChildren();

		@SuppressWarnings( "unchecked" )
		List<UIComponent> toChildren = to.getChildren();

		for ( UIComponent component : fromChildren )
		{
			if ( !( component instanceof UIParameter ) )
				continue;

			// ...that is not excluded...

			UIParameter parameter = (UIParameter) component;

			String name = parameter.getName();

			if ( ArrayUtils.contains( exclude, name ) )
				continue;

			// ...create a copy

			UIParameter parameterCopy = (UIParameter) application.createComponent( "javax.faces.Parameter" );
			parameterCopy.setId( viewRoot.createUniqueId() );

			parameterCopy.setName( name );
			parameterCopy.setValue( parameter.getValue() );

			toChildren.add( parameterCopy );
		}
	}

	//
	// Private statics
	//

	private static void renderChildren( FacesContext context, UIComponent component )
		throws IOException
	{
		@SuppressWarnings( "unchecked" )
		List<UIComponent> children = component.getChildren();

		for ( UIComponent componentChild : children )
		{
			render( context, componentChild );
		}
	}

	private final static Pattern	PATTERN_VALUE_EXPRESSION	= Pattern.compile( "((#|\\$)\\{)([^}]*)(\\})" );

	private final static String		VALUE_EXPRESSION_START	= "#{";

	private final static String		VALUE_EXPRESSION_END		= "}";

	//
	// Private constructor
	//

	private FacesUtils()
	{
		// Can never be called
	}
}
