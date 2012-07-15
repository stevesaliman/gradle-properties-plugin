package net.saliman.gradle.plugin.properties

import org.gradle.testfixtures.ProjectBuilder

/**
 * Test class for the Properties plugin
 * 
 * @author Steven C. Saliman
 */
class PropertiesPluginTest extends GroovyTestCase {
	def plugin = new PropertiesPlugin()
	def project = ProjectBuilder.builder().build()

	public void setUp() {
		def builder = new AntBuilder()
		builder.copy(file:'src/test/resources/gradle-local.properties',
					       tofile : "${project.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/gradle-test.properties',
						     tofile : "${project.projectDir}/gradle-test.properties")
	}

	/**
	 * Test the CheckProperty method when the property is missing.
	 */
	public void testCheckPropertyMissing() {
		project.ext.someProperty = "someValue"
		// we succeed if we don't get an exception.
		plugin.checkProperty(project, "someProperty")

	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyPresent() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(project, "someProperty")
		}

	}

	/**
	 * Try a build with no environment name given.  Assert that we got the
	 * property from the local file and that the token is right.  Also make sure
	 * the plugin has set the environmentName property.
	 */
	public void testBaseUseDefaultFile() {
		project.apply plugin: 'properties'
		assertEquals("localValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("localValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Build with an environment name given and make sure we got the property
	 * from the right file, that the token is right, and that we didn't change
	 * the environmentName
	 */
	public void testBaseUseAlternateFile() {
		// simulate the -PenvironmentName=test command line option
		project.setProperty('environmentName','test')
		project.apply plugin: 'properties'
		assertEquals("testValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("testValue", testFilter)
		assertEquals("test", project.ext.environmentName)
	}

	/**
	 * Try a build with no environment name, but we override a property in the
	 * file with a value from the command line.  Make sure we preserve the
	 * command line value, and we set the environmentName.
	 */
	public void testBaseOverrideFileValue() {
		// Simulate defining the test property on the command line.
		project.setProperty('testProperty','commandValue')
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("commandValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}
}
