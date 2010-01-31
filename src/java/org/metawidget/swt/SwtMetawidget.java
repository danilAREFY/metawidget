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

package org.metawidget.swt;

import static org.metawidget.inspector.InspectionResultConstants.*;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.metawidget.config.ConfigReader;
import org.metawidget.iface.MetawidgetException;
import org.metawidget.inspectionresultprocessor.iface.InspectionResultProcessor;
import org.metawidget.inspector.iface.Inspector;
import org.metawidget.layout.iface.Layout;
import org.metawidget.pipeline.w3c.W3CPipeline;
import org.metawidget.util.ArrayUtils;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.PathUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.util.simple.PathUtils.TypeAndNames;
import org.metawidget.widgetbuilder.composite.CompositeWidgetBuilder;
import org.metawidget.widgetbuilder.iface.WidgetBuilder;
import org.metawidget.widgetprocessor.iface.WidgetProcessor;
import org.w3c.dom.Element;

/**
 * Metawidget for SWT environments.
 *
 * @author Stefan Ackermann, Richard Kennard
 */

public class SwtMetawidget
	extends Composite
{
	//
	// Private statics
	//

	private final static long			serialVersionUID	= 1l;

	private final static ConfigReader	CONFIG_READER		= new ConfigReader();

	private final static String			DEFAULT_CONFIG		= "org/metawidget/swt/metawidget-swt-default.xml";

	//
	// Private members
	//

	private Object						mToInspect;

	private String						mInspectionPath;

	private String						mConfig;

	private boolean						mNeedsConfiguring	= true;

	private ResourceBundle				mBundle;

	private boolean						mNeedToBuildWidgets;

	private Element						mLastInspection;

	private Map<String, Facet>			mFacets				= CollectionUtils.newHashMap();

	private Pipeline					mPipeline;

	//
	// Constructor
	//

	public SwtMetawidget( Composite parent, int style )
	{
		super( parent, style );
		mPipeline = newPipeline();

		parent.addListener( SWT.Activate, new Listener()
		{
			public void handleEvent( org.eclipse.swt.widgets.Event event )
			{
				buildWidgets();
			}
		} );
	}

	//
	// Public methods
	//

	/**
	 * Sets the Object to inspect.
	 * <p>
	 * If <code>setPath</code> has not been set, or points to a previous <code>setToInspect</code>,
	 * sets it to point to the given Object.
	 */

	public void setToInspect( Object toInspect )
	{
		updateToInspectWithoutInvalidate( toInspect );
		invalidateInspection();
	}

	/**
	 * Updates the Object to inspect, without invalidating the previous inspection results.
	 * <p>
	 * <strong>This is an internal API exposed for WidgetProcessor rebinding support. Clients should
	 * not call it directly.</strong>
	 */

	public void updateToInspectWithoutInvalidate( Object toInspect )
	{
		if ( mToInspect == null )
		{
			if ( mInspectionPath == null && toInspect != null )
				mInspectionPath = ClassUtils.getUnproxiedClass( toInspect.getClass() ).getName();
		}
		else if ( ClassUtils.getUnproxiedClass( mToInspect.getClass() ).getName().equals( mInspectionPath ) )
		{
			if ( toInspect == null )
				mInspectionPath = null;
			else
				mInspectionPath = ClassUtils.getUnproxiedClass( toInspect.getClass() ).getName();
		}

		mToInspect = toInspect;
	}

	/**
	 * Gets the Object being inspected.
	 * <p>
	 * Exposed for binding implementations.
	 *
	 * @return the object. Note this return type uses generics, so as to not require a cast by the
	 *         caller (eg. <code>Person p = getToInspect()</code>)
	 */

	@SuppressWarnings( "unchecked" )
	public <T> T getToInspect()
	{
		return (T) mToInspect;
	}

	/**
	 * Sets the path to be inspected.
	 */

	public void setInspectionPath( String inspectionPath )
	{
		mInspectionPath = inspectionPath;
		invalidateInspection();
	}

	public String getInspectionPath()
	{
		return mInspectionPath;
	}

	public void setConfig( String config )
	{
		mConfig = config;
		mNeedsConfiguring = true;
		invalidateInspection();
	}

	public void setInspector( Inspector inspector )
	{
		mPipeline.setInspector( inspector );
		invalidateInspection();
	}

	public void addInspectionResultProcessor( InspectionResultProcessor<Element, SwtMetawidget> inspectionResultProcessor )
	{
		mPipeline.addInspectionResultProcessor( inspectionResultProcessor );
		invalidateWidgets();
	}

	public void removeInspectionResultProcessor( InspectionResultProcessor<Element, SwtMetawidget> inspectionResultProcessor )
	{
		mPipeline.removeInspectionResultProcessor( inspectionResultProcessor );
		invalidateWidgets();
	}

	public void setInspectionResultProcessors( InspectionResultProcessor<Element, SwtMetawidget>... inspectionResultProcessors )
	{
		mPipeline.setInspectionResultProcessors( CollectionUtils.newArrayList( inspectionResultProcessors ) );
		invalidateWidgets();
	}

	public void setWidgetBuilder( WidgetBuilder<Control, SwtMetawidget> widgetBuilder )
	{
		mPipeline.setWidgetBuilder( widgetBuilder );
		invalidateWidgets();
	}

	public void addWidgetProcessor( WidgetProcessor<Control, SwtMetawidget> widgetProcessor )
	{
		mPipeline.addWidgetProcessor( widgetProcessor );
		invalidateWidgets();
	}

	public void removeWidgetProcessor( WidgetProcessor<Control, SwtMetawidget> widgetProcessor )
	{
		mPipeline.removeWidgetProcessor( widgetProcessor );
		invalidateWidgets();
	}

	public void setWidgetProcessors( WidgetProcessor<Control, SwtMetawidget>... widgetProcessors )
	{
		mPipeline.setWidgetProcessors( CollectionUtils.newArrayList( widgetProcessors ) );
		invalidateWidgets();
	}

	public <T> T getWidgetProcessor( Class<T> widgetProcessorClass )
	{
		buildWidgets();
		return mPipeline.getWidgetProcessor( widgetProcessorClass );
	}

	/**
	 * Set the layout for this Metawidget.
	 * <p>
	 * Named <code>setMetawidgetLayout</code>, rather than the usual <code>setLayout</code>, because
	 * Swing already defines a <code>setLayout</code>. Overloading Swing's <code>setLayout</code>
	 * was considered cute, but ultimately confusing and dangerous. For example, what should
	 * <code>setLayout( null )</code> do?
	 */

	public void setMetawidgetLayout( Layout<Control, Composite, SwtMetawidget> layout )
	{
		mPipeline.setLayout( layout );
		invalidateWidgets();
	}

	public void setBundle( ResourceBundle bundle )
	{
		mBundle = bundle;
		invalidateWidgets();
	}

	public String getLabelString( Map<String, String> attributes )
	{
		if ( attributes == null )
			return "";

		// Explicit label

		String label = attributes.get( LABEL );

		if ( label != null )
		{
			// (may be forced blank)

			if ( "".equals( label ) )
				return null;

			// (localize if possible)

			String localized = getLocalizedKey( StringUtils.camelCase( label ) );

			if ( localized != null )
				return localized.trim();

			return label.trim();
		}

		// Default name

		String name = attributes.get( NAME );

		if ( name != null )
		{
			// (localize if possible)

			String localized = getLocalizedKey( name );

			if ( localized != null )
				return localized.trim();

			return StringUtils.uncamelCase( name );
		}

		return "";
	}

	/**
	 * @return null if no bundle, ???key??? if bundle is missing a key
	 */

	public String getLocalizedKey( String key )
	{
		if ( mBundle == null )
			return null;

		try
		{
			return mBundle.getString( key );
		}
		catch ( MissingResourceException e )
		{
			return StringUtils.RESOURCE_KEY_NOT_FOUND_PREFIX + key + StringUtils.RESOURCE_KEY_NOT_FOUND_SUFFIX;
		}
	}

	public boolean isReadOnly()
	{
		return mPipeline.isReadOnly();
	}

	public void setReadOnly( boolean readOnly )
	{
		if ( mPipeline.isReadOnly() == readOnly )
			return;

		mPipeline.setReadOnly( readOnly );
		invalidateWidgets();
	}

	public int getMaximumInspectionDepth()
	{
		return mPipeline.getMaximumInspectionDepth();
	}

	public void setMaximumInspectionDepth( int maximumInspectionDepth )
	{
		mPipeline.setMaximumInspectionDepth( maximumInspectionDepth );
		invalidateWidgets();
	}

	//
	// The following methods all kick off buildWidgets() if necessary
	//

	/**
	 * Gets the value from the Control with the given name.
	 * <p>
	 * The value is returned as it was stored in the Control (eg. String for JTextField) so may need
	 * some conversion before being reapplied to the object being inspected. This obviously requires
	 * knowledge of which Control SwtMetawidget created, which is not ideal, so clients may prefer
	 * to use bindingClass instead.
	 *
	 * @return the value. Note this return type uses generics, so as to not require a cast by the
	 *         caller (eg. <code>String s = getValue(names)</code>)
	 */

	@SuppressWarnings( "unchecked" )
	public <T> T getValue( String... names )
	{
		Object[] componentAndValueProperty = getControlAndValueProperty( names );
		return (T) ClassUtils.getProperty( (Composite) componentAndValueProperty[0], (String) componentAndValueProperty[1] );
	}

	/**
	 * Sets the Control with the given name to the specified value.
	 * <p>
	 * Clients must ensure the value is of the correct type to suit the Control (eg. String for
	 * JTextField). This obviously requires knowledge of which Control SwtMetawidget created, which
	 * is not ideal, so clients may prefer to use bindingClass instead.
	 */

	public void setValue( Object value, String... names )
	{
		Object[] componentAndValueProperty = getControlAndValueProperty( names );
		ClassUtils.setProperty( (Composite) componentAndValueProperty[0], (String) componentAndValueProperty[1], value );
	}

	/**
	 * Returns the property used to get/set the value of the component.
	 * <p>
	 * If the component is not known, returns <code>null</code>. Does not throw an Exception, as we
	 * want to fail gracefully if, say, someone tries to bind to a JPanel.
	 */

	public String getValueProperty( Control component )
	{
		return getValueProperty( component, mPipeline.getWidgetBuilder() );
	}

	/**
	 * Finds the Control with the given name.
	 */

	@SuppressWarnings( "unchecked" )
	public <T extends Control> T getControl( String... names )
	{
		if ( names == null || names.length == 0 )
			return null;

		Control topControl = this;

		for ( int loop = 0, length = names.length; loop < length; loop++ )
		{
			String name = names[loop];

			// May need building 'just in time' if we are calling getControl
			// immediately after a 'setToInspect'. See
			// SwtMetawidgetTest.testNestedWithManualInspector

			if ( topControl instanceof SwtMetawidget )
				( (SwtMetawidget) topControl ).buildWidgets();

			// Try to find a component...

			Control[] children = ( (Composite) topControl ).getChildren();
			topControl = null;

			for ( Control childControl : children )
			{
				// ...with the name we're interested in

				if ( name.equals( childControl.getData( NAME ) ) )
				{
					topControl = childControl;
					break;
				}
			}

			if ( loop == length - 1 )
				return (T) topControl;

			if ( topControl == null )
				throw MetawidgetException.newException( "No such component '" + name + "' of '" + ArrayUtils.toString( names, "', '" ) + "'" );
		}

		return (T) topControl;
	}

	public Facet getFacet( String name )
	{
		buildWidgets();

		return mFacets.get( name );
	}

	/**
	 * This method is public for use by WidgetBuilders.
	 */

	public Element inspect( Object toInspect, String type, String... names )
	{
		return mPipeline.inspect( toInspect, type, names );
	}

	//
	// Protected methods
	//

	/**
	 * Instantiate the Pipeline used by this Metawidget.
	 * <p>
	 * Subclasses wishing to use their own Pipeline should override this method to instantiate their
	 * version.
	 */

	protected Pipeline newPipeline()
	{
		return new Pipeline();
	}

	protected Pipeline getPipeline()
	{
		return mPipeline;
	}

	/**
	 * Invalidates the current inspection result (if any) <em>and</em> invalidates the widgets.
	 * <p>
	 * As an optimisation we only invalidate the widgets, not the entire inspection result, for some
	 * operations (such as adding/removing stubs, changing read-only etc.)
	 */

	protected void invalidateInspection()
	{
		mLastInspection = null;
		invalidateWidgets();
	}

	/**
	 * Invalidates the widgets.
	 */

	protected void invalidateWidgets()
	{
		if ( mNeedToBuildWidgets )
			return;

		mNeedToBuildWidgets = true;

		for ( Control control : getChildren() )
		{
			if ( control instanceof Facet )
				mFacets.put( (String) control.getData( NAME ), (Facet) control );

			//control.dispose();
		}

		// TODO: not the best place to call this (calls it every time)

		buildWidgets();
	}

	protected void configure()
	{
		if ( !mNeedsConfiguring )
			return;

		// Special support for visual IDE builders

		if ( mInspectionPath == null )
			return;

		mNeedsConfiguring = false;

		try
		{
			if ( mConfig != null )
				CONFIG_READER.configure( mConfig, this );

			// SwtMetawidget uses setMetawidgetLayout, not setLayout

			if ( mPipeline.getLayout() == null )
				CONFIG_READER.configure( DEFAULT_CONFIG, this, "metawidgetLayout" );

			mPipeline.configureDefaults( CONFIG_READER, DEFAULT_CONFIG, SwtMetawidget.class );
		}
		catch ( Exception e )
		{
			throw MetawidgetException.newException( e );
		}
	}

	protected void buildWidgets()
	{
		// No need to build?

		if ( !mNeedToBuildWidgets )
			return;

		configure();

		mNeedToBuildWidgets = false;

		try
		{
			if ( mLastInspection == null )
				mLastInspection = inspect();

			mPipeline.buildWidgets( mLastInspection );
		}
		catch ( Exception e )
		{
			throw MetawidgetException.newException( e );
		}
	}

	protected void addWidget( Control component, String elementName, Map<String, String> attributes )
	{
		// Set the name of the component.
		//
		// If this is a JScrollPane, set the name of the top-level JScrollPane. Don't do this before
		// now, as we don't want binding/validation implementations accidentally relying on the
		// name being set (which it won't be for actualControl)

		component.setData( NAME, attributes.get( NAME ) );

		// Re-order the component

		component.moveBelow( null );

		Map<String, String> additionalAttributes = mPipeline.getAdditionalAttributes( component );

		if ( additionalAttributes != null )
			attributes.putAll( additionalAttributes );

		// BasePipeline will call .layoutWidget
	}

	protected Element inspect()
	{
		if ( mInspectionPath == null )
			return null;

		TypeAndNames typeAndNames = PathUtils.parsePath( mInspectionPath );
		return inspect( mToInspect, typeAndNames.getType(), typeAndNames.getNamesAsArray() );
	}

	protected void initNestedMetawidget( SwtMetawidget nestedMetawidget, Map<String, String> attributes )
	{
		// Don't copy setConfig(). Instead, copy runtime values

		mPipeline.initNestedPipeline( nestedMetawidget.mPipeline, attributes );
		nestedMetawidget.setInspectionPath( mInspectionPath + StringUtils.SEPARATOR_FORWARD_SLASH_CHAR + attributes.get( NAME ) );
		nestedMetawidget.setBundle( mBundle );
		nestedMetawidget.setToInspect( mToInspect );
	}

	//
	// Private methods
	//

	private Object[] getControlAndValueProperty( String... names )
	{
		Control component = getControl( names );

		if ( component == null )
			throw MetawidgetException.newException( "No component named '" + ArrayUtils.toString( names, "', '" ) + "'" );

		String componentProperty = getValueProperty( component );

		if ( componentProperty == null )
			throw MetawidgetException.newException( "Don't know how to getValue from a " + component.getClass().getName() );

		return new Object[] { component, componentProperty };
	}

	private String getValueProperty( Control control, WidgetBuilder<Control, SwtMetawidget> widgetBuilder )
	{
		// Recurse into CompositeWidgetBuilders

		try
		{
			if ( widgetBuilder instanceof CompositeWidgetBuilder<?, ?> )
			{
				for ( WidgetBuilder<Control, SwtMetawidget> widgetBuilderChild : ( (CompositeWidgetBuilder<Control, SwtMetawidget>) widgetBuilder ).getWidgetBuilders() )
				{
					String valueProperty = getValueProperty( control, widgetBuilderChild );

					if ( valueProperty != null )
						return valueProperty;
				}

				return null;
			}
		}
		catch ( NoClassDefFoundError e )
		{
			// May not be shipping with CompositeWidgetBuilder
		}

		// Interrogate ValuePropertyProviders

		if ( widgetBuilder instanceof SwtValuePropertyProvider )
			return ( (SwtValuePropertyProvider) widgetBuilder ).getValueProperty( control );

		return null;
	}

	//
	// Inner class
	//

	protected class Pipeline
		extends W3CPipeline<Control, Composite, SwtMetawidget>
	{
		//
		// Protected methods
		//

		@Override
		protected void addWidget( Control component, String elementName, Map<String, String> attributes )
		{
			SwtMetawidget.this.addWidget( component, elementName, attributes );
			super.addWidget( component, elementName, attributes );
		}

		@Override
		protected Map<String, String> getAdditionalAttributes( Control component )
		{
			if ( component instanceof Stub )
				return ( (Stub) component ).getAttributes();

			return null;
		}

		@Override
		public SwtMetawidget buildNestedMetawidget( Map<String, String> attributes )
			throws Exception
		{
			SwtMetawidget nestedMetawidget = SwtMetawidget.this.getClass().getConstructor( Composite.class, int.class ).newInstance( getPipelineOwner(), SWT.None );
			SwtMetawidget.this.initNestedMetawidget( nestedMetawidget, attributes );

			return nestedMetawidget;
		}

		@Override
		protected SwtMetawidget getPipelineOwner()
		{
			return SwtMetawidget.this;
		}
	}
}