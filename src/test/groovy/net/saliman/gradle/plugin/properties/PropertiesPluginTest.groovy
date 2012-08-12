package net.saliman.gradle.plugin.properties

import org.gradle.testfixtures.ProjectBuilder

/**
 * Test class for the Properties plugin.  Note that we set properties in each
 * test because we can't seem to clear a property once it is set.
 * 
 * @author Steven C. Saliman
 */
class PropertiesPluginTest extends GroovyTestCase {
	def projectDir = new File("build/test/project")
	def plugin = new PropertiesPlugin()
	def project = ProjectBuilder.builder().withProjectDir(projectDir).build();

	public void setUp() {
		def userDir = project.gradle.gradleUserHomeDir
		def builder = new AntBuilder()
		builder.copy(file:'src/test/resources/gradle-user.properties',
						     tofile : "${userDir}/gradle-user.properties")
		builder.copy(file:'src/test/resources/gradle-home.properties',
   						   tofile : "${userDir}/gradle.properties")
		builder.copy(file:'src/test/resources/gradle-local.properties',
					       tofile : "${project.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/gradle-test.properties',
						     tofile : "${project.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/gradle-project.properties',
						tofile : "${project.projectDir}/gradle.properties")
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

	// These tests are split up into multiples so that if one part works but
	// another doesn't we have an easier time finding things.
	/**
	 * Verify that a command line value overrides everything else.
	 */
	public void testApplyCommandProperty() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.commandProperty)
		def testFilter = project.ext.filterTokens["command.property"]
		assertEquals("commandValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Verify that when a property is set everywhere but the command line, the
	 * the user value wins
	 */
	public void testApplyUserProperty() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.apply plugin: 'properties'
		assertEquals("userValue", project.ext.userProperty)
		def testFilter = project.ext.filterTokens["user.property"]
		assertEquals("userValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Verify that when a property is set everywhere but the command line, but
	 * no user is given, we get the value from the home file.
	 */
	public void testApplyUserPropertyNoUser() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.apply plugin: 'properties'
		assertEquals("homeUserValue", project.ext.userProperty)
		def testFilter = project.ext.filterTokens["user.property"]
		assertEquals("homeUserValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Verify that when a property is set everywhere but the command line and
	 * user file, the home file wins.
	 */
	public void testApplyHomeProperty() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.apply plugin: 'properties'
		assertEquals("homeValue", project.ext.homeProperty)
		def testFilter = project.ext.filterTokens["home.property"]
		assertEquals("homeValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we don't specify an environment, the local environment file wins.
	 */
	public void testApplyUseDefaultFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.apply plugin: 'properties'
		assertEquals("localEnvironmentValue", project.ext.environmentProperty)
		def testFilter = project.ext.filterTokens["environment.property"]
		assertEquals("localEnvironmentValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we do specify an environment, the specified environment file wins.
	 */
	public void testApplyUseAlternateFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		// simulate the -PenvironmentName=test command line option
		project.ext.setProperty("gradleUserName", "user")
		project.ext.setProperty('environmentName','test')
		project.apply plugin: 'properties'
		assertEquals("testEnvironmentValue", project.ext.environmentProperty)
		def testFilter = project.ext.filterTokens["environment.property"]
		assertEquals("testEnvironmentValue", testFilter)
		assertEquals("test", project.ext.environmentName)
	}

	/**
	 * Verify that when we only specify a property in the project property file,
	 * it still gets set and is in the filters.
	 */
	public void testApplyProjectProperties() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.apply plugin: 'properties'
		assertEquals("projectValue", project.ext.projectProperty)
		def testFilter = project.ext.filterTokens["project.property"]
		assertEquals("projectValue", testFilter)
		assertEquals("local", project.ext.environmentName)
	}

	// This set of tests tests what happens when certain files are missing.
	// To be thorough, we'll test all the properties and tokens.

	/**
	 * Test applying the plugin when we have no user file, but we didn't specify
	 * a user.  This is not an error.
	 */
	public void testApplyMissingUnspecifiedUserFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		def propFile = new File("${project.gradle.gradleUserHomeDir}/gradle-user.properties")
		propFile.delete()
		assertFalse("Failed to delete user file", propFile.exists())
		project.properties.remove("gradleUserName")
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.commandProperty)
		assertEquals("homeUserValue", project.ext.userProperty)
		assertEquals("homeValue", project.ext.homeProperty)
		assertEquals("localEnvironmentValue", project.ext.environmentProperty)
		assertEquals("projectValue", project.ext.projectProperty)
		assertEquals(5, project.ext.filterTokens.size())
		assertEquals("commandValue", project.ext.filterTokens["command.property"])
		assertEquals("homeUserValue", project.ext.filterTokens["user.property"])
		assertEquals("homeValue", project.ext.filterTokens["home.property"])
		assertEquals("localEnvironmentValue", project.ext.filterTokens["environment.property"])
		assertEquals("projectValue", project.ext.filterTokens["project.property"])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "dummy")
		try {
			project.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing a user file.")
		} catch ( FileNotFoundException e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no home file.  This should not produce an
	 * error.
	 */
	public void testApplyMissingHomeFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("projectHomeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		// Unset the home property setUp set.
		project.ext.setProperty("gradleUserName", "user")
		def propFile = new File("${project.gradle.gradleUserHomeDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete home file", propFile.exists())
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.commandProperty)
		assertEquals("userValue", project.ext.userProperty)
		assertEquals("localHomeValue", project.ext.homeProperty)
		assertEquals("localEnvironmentValue", project.ext.environmentProperty)
		assertEquals("projectValue", project.ext.projectProperty)
		assertEquals(5, project.ext.filterTokens.size())
		assertEquals("commandValue", project.ext.filterTokens["command.property"])
		assertEquals("userValue", project.ext.filterTokens["user.property"])
		assertEquals("localHomeValue", project.ext.filterTokens["home.property"])
		assertEquals("localEnvironmentValue", project.ext.filterTokens["environment.property"])
		assertEquals("projectValue", project.ext.filterTokens["project.property"])
	}

	/**
	 * Test what happens when we have no environment file, and we're using the
	 * default "local" file.  This should not be an error.
	 */
	public void testApplyMissingUnspecifiedEnvFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		def propFile = new File("${project.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse("Failed to delete local file", propFile.exists())
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.commandProperty)
		assertEquals("userValue", project.ext.userProperty)
		assertEquals("homeValue", project.ext.homeProperty)
		assertEquals("projectEnvironmentValue", project.ext.environmentProperty)
		assertEquals("projectValue", project.ext.projectProperty)
		assertEquals(5, project.ext.filterTokens.size())
		assertEquals("commandValue", project.ext.filterTokens["command.property"])
		assertEquals("userValue", project.ext.filterTokens["user.property"])
		assertEquals("homeValue", project.ext.filterTokens["home.property"])
		assertEquals("projectEnvironmentValue", project.ext.filterTokens["environment.property"])
		assertEquals("projectValue", project.ext.filterTokens["project.property"])
	}

	/**
	 * Test what happens when we have no environment file, but we specify an
	 * environment file.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")
		project.ext.setProperty("projectProperty", "projectValue")

		project.ext.setProperty("gradleUserName", "user")
		project.ext.environmentName = "dummy"
		try {
			project.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( FileNotFoundException e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no project property file.  This is no error.
	 */
	public void testApplyMissingProjectFile() {
		// simulate the properties that would be set if the properties files were
		// actually read by Gradle doing a build. Gradle only handles properties in
		// the command line, and the 2 gradle.properties files, which should result
		// in the values below.
		def commandProps = project.gradle.startParameter.projectProperties
		commandProps.commandProperty = "commandValue"
		project.ext.setProperty("userProperty", "homeUserValue")
		project.ext.setProperty("homeProperty", "homeValue")
		project.ext.setProperty("environmentProperty", "projectEnvironmentValue")

		// Unset the project property setUp set.
		project.properties.remove('projectProperty')
		project.ext.setProperty("gradleUserName", "user")
		def propFile = new File("${project.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete project file", propFile.exists())
		project.apply plugin: 'properties'
		assertEquals("commandValue", project.ext.commandProperty)
		assertEquals("userValue", project.ext.userProperty)
		assertEquals("homeValue", project.ext.homeProperty)
		assertEquals("localEnvironmentValue", project.ext.environmentProperty)
		assertFalse("We shouldn't have a project property", project.hasProperty("projectProperty"))
		assertEquals(4, project.ext.filterTokens.size())
		assertEquals("commandValue", project.ext.filterTokens["command.property"])
		assertEquals("userValue", project.ext.filterTokens["user.property"])
		assertEquals("homeValue", project.ext.filterTokens["home.property"])
		assertEquals("localEnvironmentValue", project.ext.filterTokens["environment.property"])
	}
}
