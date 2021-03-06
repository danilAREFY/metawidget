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

package org.metawidget.config.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.SwingConstants;

import junit.framework.TestCase;

import org.metawidget.config.iface.ConfigReader;
import org.metawidget.config.impl.AllTypesInspectorConfig.FooEnum;
import org.metawidget.iface.MetawidgetException;
import org.metawidget.inspector.composite.CompositeInspector;
import org.metawidget.inspector.iface.Inspector;
import org.metawidget.inspector.propertytype.PropertyTypeInspector;
import org.metawidget.inspector.xml.XmlInspector;
import org.metawidget.util.IOUtils;
import org.metawidget.util.LogUtils;
import org.metawidget.util.LogUtilsTest;

/**
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

public class ConfigReaderTest
	extends TestCase {

	//
	// Public methods
	//

	public void testNoDefaultConstructor()
		throws Exception {

		// With config hint

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget>";
		xml += "	<xmlInspector xmlns=\"java:org.metawidget.inspector.xml\"/>";
		xml += "</metawidget>";

		ConfigReader configReader = new BaseConfigReader();

		try {
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "class org.metawidget.inspector.xml.XmlInspector does not have a default constructor. Did you mean config=\"XmlInspectorConfig\"?".equals( e.getMessage() ) );
		}

		// With out-of-package config hint

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget>";
		xml += "	<outOfPackageConfigInspector xmlns=\"java:org.metawidget.config.impl.subpackage\"/>";
		xml += "</metawidget>";

		try {
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.config.impl.subpackage.OutOfPackageConfigInspector does not have a default constructor. Did you mean config=\"org.metawidget.config.impl.AllTypesInspectorConfig\"?", e.getMessage() );
		}

		// Without config hint

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget>";
		xml += "	<class xmlns=\"java:java.lang\"/>";
		xml += "</metawidget>";

		try {
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Class.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "class java.lang.Class does not have a default constructor".equals( e.getMessage() ) );
		}
	}

	public void testBadUrl()
		throws Exception {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget>";
		xml += "	<xmlInspector xmlns=\"java:org.metawidget.inspector.xml\" config=\"XmlInspectorConfig\">";
		xml += "		<inputStream>";
		xml += "			<url>http://foo.nowhere</url>";
		xml += "		</inputStream>";
		xml += "	</xmlInspector>";
		xml += "</metawidget>";

		ConfigReader configReader = new BaseConfigReader();

		try {
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {

			// Bizzarely, the host may actually resolve if your ISP or DNS provider (eg. OpenDNS)
			// puts in a special page. In that case you'll get a FileNotFoundException or a
			// SAXParseException

			assertTrue( true );
		}
	}

	public void testBadFile()
		throws Exception {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget>";
		xml += "	<xmlInspector xmlns=\"java:org.metawidget.inspector.xml\" config=\"XmlInspectorConfig\">";
		xml += "		<inputStream>";
		xml += "			<file>/tmp/no.such.file</file>";
		xml += "		</inputStream>";
		xml += "	</xmlInspector>";
		xml += "</metawidget>";

		ConfigReader configReader = new BaseConfigReader();

		try {
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage(), e.getMessage().contains( File.separatorChar + "tmp" + File.separatorChar + "no.such.file" ));
		}
	}

	public void testForgottenConfigAttribute()
		throws Exception {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\">";
		xml += "<propertyStyle><javaBeanPropertyStyle xmlns=\"java:org.metawidget.inspector.impl.propertystyle.javabean\"/></propertyStyle>";
		xml += "</propertyTypeInspector></metawidget>";

		try {
			ConfigReader configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().endsWith( "class org.metawidget.inspector.propertytype.PropertyTypeInspector.setPropertyStyle(JavaBeanPropertyStyle)" ));
		}
	}

	public void testLikelyMethod()
		throws Exception {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\">";
		xml += "<propertyStyle><boolean>true</boolean></propertyStyle>";
		xml += "</propertyTypeInspector></metawidget>";

		try {
			ConfigReader configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().endsWith( "class org.metawidget.inspector.impl.BaseObjectInspectorConfig.setPropertyStyle(Boolean). Did you mean setPropertyStyle(PropertyStyle)?" ));
		}

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<xmlInspector xmlns=\"java:org.metawidget.inspector.xml\" config=\"org.metawidget.inspector.xml.XmlInspectorConfig\">";
		xml += "<inputStreams><list><int>0</int></list></inputStreams>";
		xml += "</xmlInspector></metawidget>";

		try {
			ConfigReader configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), XmlInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().endsWith( "class org.metawidget.inspector.xml.XmlInspectorConfig.setInputStreams(ArrayList). Did you mean setInputStreams(InputStream[])?" ));
		}
	}

	public void testSupportedTypes() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<int><int>3</int></int>";
		xml += "<constant><constant>CONSTANT_VALUE</constant></constant>";
		xml += "<externalConstant><constant>javax.swing.SwingConstants.LEFT</constant></externalConstant>";
		xml += "<list>";
		xml += "<list>";
		xml += "<string>foo</string>";
		xml += "<string>bar</string>";
		xml += "<class>java.lang.String</class>";
		xml += "<class>java.util.Date</class>";
		xml += "<class>java.lang.Long</class>";
		xml += "<null/>";
		xml += "<instanceOf>java.util.Date</instanceOf>";
		xml += "</list>";
		xml += "</list>";
		xml += "<set>";
		xml += "<set>";
		xml += "<string>baz</string>";
		xml += "</set>";
		xml += "</set>";
		xml += "<booleanPrimitive><boolean>true</boolean></booleanPrimitive>";
		xml += "<pattern><pattern>.*?</pattern></pattern>";
		xml += "<inputStream><resource>org/metawidget/config/metawidget-test-logging.xml</resource></inputStream>";
		xml += "<resourceBundle><bundle>org/metawidget/config/Resources</bundle></resourceBundle>";
		xml += "<stringArray><array><string>foo</string><string>bar</string></array></stringArray>";
		xml += "<enum><enum>BAR</enum></enum>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		AllTypesInspector inspector = (AllTypesInspector) new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
		assertEquals( 3, inspector.getInt() );
		assertEquals( AllTypesInspectorConfig.CONSTANT_VALUE, inspector.getConstant() );
		assertEquals( SwingConstants.LEFT, inspector.getExternalConstant() );

		List<Object> list = inspector.getList();
		assertTrue( "foo".equals( list.get( 0 ) ) );
		assertTrue( "bar".equals( list.get( 1 ) ) );
		assertTrue( String.class.equals( list.get( 2 ) ) );
		assertTrue( Date.class.equals( list.get( 3 ) ) );
		assertTrue( Long.class.equals( list.get( 4 ) ) );
		assertEquals( null, list.get( 5 ) );
		assertTrue( list.get( 6 ) instanceof Date );
		assertEquals( 7, list.size() );

		Set<Object> set = inspector.getSet();
		assertTrue( "baz".equals( set.iterator().next() ) );

		assertEquals( true, inspector.isBoolean() );
		assertTrue( ".*?".equals( inspector.getPattern().toString() ) );

		ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
		IOUtils.streamBetween( inspector.getInputStream(), streamOut );
		assertTrue( streamOut.toString().contains( "<metawidget xmlns=\"http://metawidget.org\"" ) );

		assertTrue( "value1".equals( inspector.getResourceBundle().getString( "key1" ) ) );

		assertEquals( 2, inspector.getStringArray().length );
		assertTrue( "foo".equals( inspector.getStringArray()[0] ) );
		assertTrue( "bar".equals( inspector.getStringArray()[1] ) );

		assertTrue( FooEnum.BAR.equals( inspector.getEnum() ) );
	}

	public void testUnsupportedType() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<date><date>1/1/2001</date></date>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().endsWith( "No such tag <date> or class org.metawidget.config.impl.Date (is it on your CLASSPATH?)" ) );
		}
	}

	public void testBadNamespace() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<date><date>1/1/2001</date></date>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().contains( "org.xml.sax.SAXException: Namespace 'org.metawidget.config.impl' of element <allTypesInspector> must start with java:" ));
		}
	}

	public void testEmptyCollection() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<list>";
		xml += "<list/>";
		xml += "</list>";
		xml += "<set>";
		xml += "<set/>";
		xml += "</set>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		AllTypesInspector inspector = (AllTypesInspector) new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
		assertTrue( inspector.getList().isEmpty() );
		assertTrue( inspector.getSet().isEmpty() );
	}

	public void testMetawidgetExceptionDuringConstruction() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<failDuringConstruction><boolean>true</boolean></failDuringConstruction>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "Failed during construction".equals( e.getCause().getMessage() ) );
		}
	}

	public void testSetterWithNoParameters() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\">";
		xml += "<noParameters/>";
		xml += "</allTypesInspector>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( e.getMessage().endsWith( ": Called setNoParameters" ));
		}
	}

	public void testNoInspector() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "No match for class org.metawidget.config.impl.AllTypesInspector within config".equals( e.getMessage() ) );
		}
	}

	public void testMultipleInspectors() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\"/>";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"AllTypesInspectorConfig\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "Already configured a class org.metawidget.config.impl.AllTypesInspector, ambiguous match with class org.metawidget.config.impl.AllTypesInspector".equals( e.getMessage() ) );
		}
	}

	public void testMissingResource() {

		ConfigReader configReader = new BaseConfigReader();

		try {
			configReader.configure( (String) null, null );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "java.io.FileNotFoundException: No resource specified".equals( e.getMessage() ) );
		}

		try {
			configReader.configure( "", null );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "java.io.FileNotFoundException: No resource specified".equals( e.getMessage() ) );
		}

		try {
			configReader.configure( " ", null );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "java.io.FileNotFoundException: No resource specified".equals( e.getMessage() ) );
		}

		try {
			configReader.configure( " foo", null );
			fail();
		} catch ( MetawidgetException e ) {
			assertTrue( "java.io.FileNotFoundException: Unable to locate  foo on CLASSPATH".equals( e.getMessage() ) );
		}
	}

	public void testLogging() {

		ConfigReader configReader = new BaseConfigReader();
		configReader.configure( "org/metawidget/config/metawidget-test-logging.xml", CompositeInspector.class, "inspectors", "array" );
		configReader.configure( "org/metawidget/config/metawidget-test-logging.xml", Inspector.class, "inspectors", "array" );

		// Test it doesn't log 'Instantiated immutable class
		// org.metawidget.inspector.composite.CompositeInspector' a second time

		if ( LogUtils.getLog( ConfigReader.class ).isDebugEnabled() ) {
			assertEquals( "Reading resource from org/metawidget/config/metawidget-test-logging.xml/org.metawidget.inspector.iface.Inspector/inspectors/array", LogUtilsTest.getLastDebugMessage() );
		} else {
			assertTrue( !LogUtils.getLog( ConfigReader.class ).isDebugEnabled() );
			assertEquals( "Reading resource from {0}", LogUtilsTest.getLastDebugMessage() );
			assertEquals( "org/metawidget/config/metawidget-test-logging.xml/org.metawidget.inspector.iface.Inspector/inspectors/array", LogUtilsTest.getLastDebugArguments()[0] );
		}
	}

	public void testPatternCache()
		throws Exception {

		assertFalse( Pattern.compile( "foo" ).equals( Pattern.compile( "foo" ) ) );

		BaseConfigReader configReader = new BaseConfigReader();
		assertTrue( configReader.createNative( "pattern", null, "foo" ).equals( configReader.createNative( "pattern", null, "foo" ) ) );
	}

	public void testUppercase() {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<UPPERCASEInspector xmlns=\"java:org.metawidget.config.impl\"/>";
		xml += "</metawidget>";

		assertTrue( new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), UPPERCASEInspector.class ) instanceof UPPERCASEInspector );
	}

	public void testBadConfigImplementation() {

		// No equals

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"NoEqualsInspectorConfig\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.config.impl.NoEqualsInspectorConfig does not override .equals(), so cannot cache reliably", e.getMessage() );
		}

		// No hashCode

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"NoHashCodeInspectorConfig\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.config.impl.NoHashCodeInspectorConfig does not override .hashCode(), so cannot cache reliably", e.getMessage() );
		}

		// Unbalanced

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"UnbalancedEqualsInspectorConfig\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );

			// fail();
			//
			// (works running JUnit in Eclipse, but not via Maven. Does the VM cache reflection
			// results or something?)
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.config.impl.NoHashCodeInspectorConfig implements .equals(), but .hashCode() is implemented by class org.metawidget.config.impl.UnbalancedEqualsInspectorConfig, so cannot cache reliably", e.getMessage() );
		}

		// No such constructor

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"java.lang.String\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), AllTypesInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.config.impl.AllTypesInspector does not have a constructor that takes a class java.lang.String, as specified by your config attribute", e.getMessage() );
		}

		// Different constructor

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<xmlInspector xmlns=\"java:org.metawidget.inspector.xml\" config=\"java.lang.String\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), XmlInspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class org.metawidget.inspector.xml.XmlInspector does not have a constructor that takes a class java.lang.String, as specified by your config attribute. Did you mean config=\"XmlInspectorConfig\"?", e.getMessage() );
		}

		// Config-less constructor

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<object xmlns=\"java:java.lang\" config=\"java.lang.String\"/>";
		xml += "</metawidget>";

		try {
			new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), Object.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "class java.lang.Object does not have a constructor that takes a class java.lang.String, as specified by your config attribute. It only has a config-less constructor", e.getMessage() );
		}

		// Superclass does, but subclass doesn't, but no methods

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"NoEqualsSubclassInspectorConfig\"/>";
		xml += "</metawidget>";

		LogUtilsTest.clearLastWarnMessage();

		new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );

		assertEquals( null, LogUtilsTest.getLastWarnMessage() );

		// Superclass does, but subclass doesn't, and has methods

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"NoEqualsHasMethodsSubclassInspectorConfig\"/>";
		xml += "</metawidget>";

		new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );

		assertEquals( "class org.metawidget.config.impl.NoEqualsHasMethodsSubclassInspectorConfig does not override .equals() (only its superclass org.metawidget.config.impl.AllTypesInspectorConfig does), so may not be cached reliably", LogUtilsTest.getLastWarnMessage() );

		// Overridden, but uses super.hashCode

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<allTypesInspector xmlns=\"java:org.metawidget.config.impl\" config=\"DumbHashCodeInspectorConfig\"/>";
		xml += "</metawidget>";

		new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );

		assertEquals( "class org.metawidget.config.impl.DumbHashCodeInspectorConfig overrides .hashCode(), but it returns the same as System.identityHashCode, so cannot be cached reliably", LogUtilsTest.getLastWarnMessage() );

		// Immutable has setter method

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<mutableInspector xmlns=\"java:org.metawidget.config.impl\"/>";
		xml += "</metawidget>";

		new BaseConfigReader().configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );

		assertEquals( "class org.metawidget.config.impl.MutableInspector must be immutable, but appears to have a setter method (public void org.metawidget.config.impl.MutableInspector.setFoo(java.lang.String))", LogUtilsTest.getLastWarnMessage() );
	}

	public void testLookupClass()
		throws Exception {

		assertEquals( new BaseConfigReader().lookupClass( "java:" + ConfigReaderTest.class.getPackage().getName(), ConfigReaderTest.class.getSimpleName(), null ), ConfigReaderTest.class );
		assertEquals( new BaseConfigReader().lookupClass( "java:" + ConfigReaderTest.class.getName(), Foo.class.getSimpleName(), null ), Foo.class );
	}

	public void testIdAttribute()
		throws Exception {

		String xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\">";
		xml += "<propertyStyle><javaBeanPropertyStyle xmlns=\"java:org.metawidget.inspector.impl.propertystyle.javabean\" id=\"fooPropertyStyle\"/></propertyStyle>";
		xml += "<propertyStyle><javaBeanPropertyStyle refId=\"fooPropertyStyle\"/></propertyStyle>";
		xml += "</propertyTypeInspector></metawidget>";

		ConfigReader configReader = new BaseConfigReader();
		Inspector inspector = (Inspector) configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
		assertTrue( inspector instanceof PropertyTypeInspector );

		// Non-existent id

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\">";
		xml += "<propertyStyle><javaBeanPropertyStyle refId=\"fooPropertyStyle\"/></propertyStyle>";
		xml += "</propertyTypeInspector></metawidget>";

		try {
			configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "Attribute refId=\"fooPropertyStyle\" refers to non-existent id", e.getMessage() );
		}

		// Duplicate id

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\" id=\"fooPropertyStyle\">";
		xml += "<propertyStyle><javaBeanPropertyStyle xmlns=\"java:org.metawidget.inspector.impl.propertystyle.javabean\" id=\"fooPropertyStyle\"/></propertyStyle>";
		xml += "</propertyTypeInspector></metawidget>";

		try {
			configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "Attribute id=\"fooPropertyStyle\" appears more than once", e.getMessage() );
		}

		// Bad id

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<compositeInspector xmlns=\"java:org.metawidget.inspector.composite\" config=\"CompositeInspectorConfig\">";
		xml += "<inspectors><array>";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\" id=\"fooPropertyStyle\"/>";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\">";
		xml += "<propertyStyle><javaBeanPropertyStyle refId=\"fooPropertyStyle\"/></propertyStyle>";
		xml += "</propertyTypeInspector>";
		xml += "</array></inspectors>";
		xml += "</compositeInspector></metawidget>";

		try {
			configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "refId=\"fooPropertyStyle\" points to an object of class org.metawidget.inspector.propertytype.PropertyTypeInspector, not a <javaBeanPropertyStyle>", e.getMessage() );
		}

		// Mutual exclusion

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<compositeInspector refId=\"foo\" xmlns=\"java:org.metawidget.inspector.composite\" config=\"CompositeInspectorConfig\"/>";
		xml += "</metawidget>";

		try {
			configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "Elements with 'refId' attributes (refId=\"foo\") cannot also have 'config' attributes (config=\"CompositeInspectorConfig\")", e.getMessage() );
		}

		xml = "<?xml version=\"1.0\"?>";
		xml += "<metawidget xmlns=\"http://metawidget.org\"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"	xsi:schemaLocation=\"http://metawidget.org http://metawidget.org/xsd/metawidget-1.0.xsd\" version=\"1.0\">";
		xml += "<compositeInspector xmlns=\"java:org.metawidget.inspector.composite\" config=\"CompositeInspectorConfig\">";
		xml += "<inspectors><array>";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" config=\"org.metawidget.inspector.impl.BaseObjectInspectorConfig\" id=\"fooPropertyStyle\"/>";
		xml += "<propertyTypeInspector xmlns=\"java:org.metawidget.inspector.propertytype\" refId=\"fooPropertyStyle\">";
		xml += "<propertyStyle/>";
		xml += "</propertyTypeInspector>";
		xml += "</array></inspectors>";
		xml += "</compositeInspector></metawidget>";

		try {
			configReader = new BaseConfigReader();
			configReader.configure( new ByteArrayInputStream( xml.getBytes() ), Inspector.class );
			fail();
		} catch ( MetawidgetException e ) {
			assertEquals( "<propertyStyle> not expected here. Elements with a 'refId' must have an empty body", e.getMessage() );
		}
	}

	//
	// Inner class
	//

	static class Foo {

		// Just an inner class
	}
}