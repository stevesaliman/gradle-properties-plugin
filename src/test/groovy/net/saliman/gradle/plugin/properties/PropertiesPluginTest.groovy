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

	public void testCheckPropertyMissing() {
		project.ext.someProperty = "someValue"
		// we succeed if we don't get an exception.
		plugin.checkProperty(project, "someProperty")

	}

	public void testCheckPropertyPresent() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(project, "someProperty")
		}

	}

	// build with no value, assert that we got the property set from local and
	// token is right.
	public void testBaseUseDefaultFile() {
		project.apply plugin: 'properties'
		assertEquals("localValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("localValue", testFilter)
	}

	// build with value, assert that we got the property from the other file
	// and the token is right
	public void testBaseUseAlternateFile() {
		// simulate the -PenvironmentName=test command line option
		project.setProperty('environmentName','test')
		project.apply plugin: 'properties'
		assertEquals("testValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("testValue", testFilter)
	}

	// build with no value, but override property from file and make sure
	// the property exists, and the token is right.
	public void testBaseOverrideFileValue() {
		// Simulate defining the test property on the command line.
		project.setProperty('testProperty','commandValue')
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.testProperty)
		def testFilter = project.ext.filterTokens["test.property"]
		assertEquals("commandValue", testFilter)
	}
}
