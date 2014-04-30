package net.saliman.gradle.plugin.properties

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.GradleException

/**
 * Test class for parent projects that apply Properties plugin.  In a multi
 * project build, parent and child projects use slightly different files.  A
 * child project will inherit from the parent project's property files, but
 * the parent project does not use the child project's files.
 * <p>
 * Note that we set properties in each test because we can't seem to clear a
 * property once it is set, and Gradle itself reads some of the properties
 * before we apply the plugin.
 *
 * @author Steven C. Saliman
 */
class PropertiesPluginParentProjectTest extends GroovyTestCase {
	def plugin = new PropertiesPlugin()
	def parentProject = null;
	def childProject = null;

	/**
	 * Set up the test data.  This calls a helper method to create the projects
	 * because we need to repeat the setup in one of the tests.
	 */
	public void setUp() {
		createProjects(true)
		copyFiles()
	}
	/**
	 * Set up for each test. This will create a parent project with one child
	 * project, and it will set the properties that would be set before the plugin
	 * is applied.  These properties would be set by Gradle itself as it creates
	 * projects from build.gradle files.  Gradle only handles properties in the
	 * command line and the 3 gradle.properties file.
	 */
	public void createProjects(includeProjectProperties) {
		// Create the parent project.
		def parentProjectDir = new File("build/test/parentProject")
		parentProject = ProjectBuilder
						.builder()
						.withName("parentProject")
						.withProjectDir(parentProjectDir)
						.build();
		if ( includeProjectProperties ) {
			parentProject.ext.parentProjectProperty = "ParentProject.parentProjectValue"
		}
		parentProject.ext.parentEnvironmentProperty = "ParentProject.parentEnvironmentValue"
		parentProject.ext.childProjectProperty = "ParentProject.childProjectValue"
		parentProject.ext.childEnvironmentProperty = "ParentProject.childEnvironmentValue"
		parentProject.ext.homeProperty = "Home.homeValue"
		parentProject.ext.userProperty = "Home.userValue"

		// Create the child project.
		def childProjectDir = new File("build/test/parentProject/childProject")
		childProject = ProjectBuilder
						.builder()
						.withName("childProject")
						.withParent(parentProject)
						.withProjectDir(childProjectDir)
						.build();
		if ( includeProjectProperties ) {
			childProject.ext.parentProjectProperty = "ParentProject.parentProjectValue"
		}
		childProject.ext.parentEnvironmentProperty = "ParentProject.parentEnvironmentValue"
		childProject.ext.childProjectProperty = "ChildProject.childProjectValue"
		childProject.ext.childEnvironmentProperty = "ChildProject.childEnvironmentValue"
		childProject.ext.homeProperty = "Home.homeValue"
		childProject.ext.userProperty = "Home.userValue"

		// Add the child to the parent.
		parentProject.childProjects.put("child", childProject)
	}

	/**
	 * Do the actual work of copying the test property files to the correct
	 * locations within the project hierarchy
	 */
	def copyFiles() {
		// Copy the files for the parent project.
		def parentUserDir = parentProject.gradle.gradleUserHomeDir
		def builder = new AntBuilder()
		builder.copy(file:'src/test/resources/parent-project-gradle.properties',
						tofile : "${parentProject.projectDir}/gradle.properties")
		builder.copy(file:'src/test/resources/parent-env-test.properties',
						tofile : "${parentProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/parent-env-local.properties',
						tofile : "${parentProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${parentUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${parentUserDir}/gradle-user.properties")

		// copy the files for the child project.
		def childUserDir = childProject.gradle.gradleUserHomeDir
		builder.copy(file:'src/test/resources/child-project-gradle.properties',
						tofile : "${childProject.projectDir}/gradle.properties")
		builder.copy(file:'src/test/resources/child-env-test.properties',
						tofile : "${childProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/child-env-local.properties',
						tofile : "${childProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${childUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${childUserDir}/gradle-user.properties")
	}

	/**
	 * Helper method to set the properties that Gradle would have set for us
	 * via the command line, system properties, and environment variables.
	 * @param setEnvironmentProperties whether or not to set the environment
	 *        properties
	 * @param setSystemProperties whether or not to set the system properties
	 * @param setCommandProperties whether or not to set the command property.
	 */
	public void setNonFileProperties(boolean setEnvironmentProperties,
	                                 boolean setSystemProperties,
	                                 boolean setCommandProperties) {
		if ( setEnvironmentProperties ) {
			SetEnv.setEnv([ 'ORG_GRADLE_PROJECT_environmentProperty' : 'Environment.environmentValue',
							'ORG_GRADLE_PROJECT_systemProperty' : 'Environment.systemValue',
							'ORG_GRADLE_PROJECT_commandProperty' :'Environment.commandValue'])
			// Make sure the utility worked.
			assertEquals("Failed to set ORG_GRADLE_PROJECT_environmentProperty",
							'Environment.environmentValue',
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertEquals("Failed to set ORG_GRADLE_PROJECT_systemProperty",
							'Environment.systemValue',
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertEquals("Failed to set ORG_GRADLE_PROJECT_commandProperty",
							'Environment.commandValue',
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))
		} else {
			SetEnv.unsetEnv(['ORG_GRADLE_PROJECT_environmentProperty',
							'ORG_GRADLE_PROJECT_systemProperty',
							'ORG_GRADLE_PROJECT_commandProperty'])
			// Make sure the utility worked.
			assertNull("Failed to clear ORG_GRADLE_PROJECT_environmentProperty",
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertNull("Failed to clear ORG_GRADLE_PROJECT_systemProperty",
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertNull("Failed to clear ORG_GRADLE_PROJECT_commandProperty",
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))

		}
		if ( setSystemProperties ) {
			System.setProperty('org.gradle.project.systemProperty', 'System.systemValue')
			System.setProperty('org.gradle.project.commandProperty', 'System.commandValue')
		} else {
			System.clearProperty('org.gradle.project.systemProperty')
			System.clearProperty('org.gradle.project.commandProperty')
		}
		if ( setCommandProperties) {
			def commandProps = parentProject.gradle.startParameter.projectProperties
			commandProps.commandProperty = "Command.commandValue"
		}

	}

	/**
	 * Test the CheckProperty method when the property is missing.
	 */
	public void testCheckPropertyPresent() {
		parentProject.ext.someProperty = "someValue"
		// we succeed if we don't get an exception.
		plugin.checkProperty(parentProject, "someProperty")

	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyMissing() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(parentProject, "someProperty")
		}

	}

	// These tests are split up into multiples so that if one part works but
	// another doesn't we have an easier time finding things.
	/**
	 * Verify that a command line value overrides everything else.
	 */
	public void testApplyCommandProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Command.commandValue", parentProject.commandProperty)
		def testFilter = parentProject.filterTokens["commandProperty"]
		assertEquals("Command.commandValue", testFilter)
		testFilter = parentProject.filterTokens["command.property"]
		assertEquals("Command.commandValue", testFilter)
	}

	/**
	 * Verify that when a property is set in all the files, but not the command
	 * line, system properties, or environment variables, the user value wins,
	 * since it is the highest priority file.
	 */
	public void testApplyUserProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("User.userValue", parentProject.userProperty)
		def testFilter = parentProject.filterTokens["userProperty"]
		assertEquals("User.userValue", testFilter)
		testFilter = parentProject.filterTokens["user.property"]
		assertEquals("User.userValue", testFilter)
	}

	/**
	 * Verify that when a file-based property is set in all files, but no user is
	 * given, we get the value from the home file.
	 */
	public void testApplyUserPropertyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue" command line
		setNonFileProperties(true, true, true)
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Home.userValue", parentProject.userProperty)
		def testFilter = parentProject.filterTokens["userProperty"]
		assertEquals("Home.userValue", testFilter)
		testFilter = parentProject.filterTokens["user.property"]
		assertEquals("Home.userValue", testFilter)
	}

	/**
	 * Verify that when a file-based property is set everywhere but the user file,
	 * the home file wins.
	 */
	public void testApplyHomeProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Home.homeValue", parentProject.homeProperty)
		def testFilter = parentProject.filterTokens["homeProperty"]
		assertEquals("Home.homeValue", testFilter)
		testFilter = parentProject.filterTokens["home.property"]
		assertEquals("Home.homeValue", testFilter)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we don't specify an environment, the local environment file wins.
	 * This also makes sure the files in the child project are ignored when we
	 * apply at a parent level.
	 */
	public void testApplyUseDefaultFile() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName =  "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		def testFilter = parentProject.filterTokens["childEnvironmentProperty"]
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["parentEnvironmentProperty"]
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["child.environment.property"]
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["parent.environment.property"]
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", testFilter)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we do specify an environment, the specified environment file wins.
	 * This also verifies that properties in child project files are ignored.
	 */
	public void testApplyUseAlternateFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		// simulate the -PenvironmentName=test and -PgradleUserName=user option
		parentProject.ext.gradleUserName =  'user'
		parentProject.ext.environmentName = 'test'
		parentProject.apply plugin: 'properties'
		assertEquals("test", parentProject.environmentName)

		assertEquals("ParentEnvironmentTest.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		def testFilter = parentProject.filterTokens["childEnvironmentProperty"]
		assertEquals("ParentEnvironmentTest.childEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["parentEnvironmentProperty"]
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["child.environment.property"]
		assertEquals("ParentEnvironmentTest.childEnvironmentValue", testFilter)
		testFilter = parentProject.filterTokens["parent.environment.property"]
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", testFilter)
	}

	/**
	 * Verify that when we only specify a property in the project property file,
	 * it still gets set and is in the filters.  This also verifies that we
	 * ignore properties set in child project files.
	 */
	public void testApplyProjectProperties() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName =  "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentProject.parentProjectValue", parentProject.parentProjectProperty)
		def testFilter = parentProject.filterTokens["childProjectProperty"]
		assertEquals("ParentEnvironmentLocal.childProjectValue", testFilter)
		testFilter = parentProject.filterTokens["parentProjectProperty"]
		assertEquals("ParentProject.parentProjectValue", testFilter)
		testFilter = parentProject.filterTokens["child.project.property"]
		assertEquals("ParentEnvironmentLocal.childProjectValue", testFilter)
		testFilter = parentProject.filterTokens["parent.project.property"]
		assertEquals("ParentProject.parentProjectValue", testFilter)
	}

	// This set of tests tests what happens when certain files are missing.
	// To be thorough, we'll test all the properties and tokens.

	/**
	 * Test applying the plugin when we have no user file, but we didn't specify
	 * a user.  This is not an error.  Remember that child files should not be
	 * processed.
	 */
	public void testApplyMissingUnspecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue" command line
		setNonFileProperties(true, true, true)
		def propFile = new File("${parentProject.gradle.gradleUserHomeDir}/gradle-user.properties")
		propFile.delete()
		assertFalse("Failed to delete user file", propFile.exists())

		parentProject.apply plugin: 'properties'
		assertEquals("ParentProject.parentProjectValue", parentProject.parentProjectProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", parentProject.homeProperty)
		assertEquals("Home.userValue", parentProject.userProperty)
		assertEquals("Environment.environmentValue", parentProject.environmentProperty)
		assertEquals("System.systemValue", parentProject.systemProperty)
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(18, parentProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["childProjectProperty"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", parentProject.filterTokens["homeProperty"])
		assertEquals("Home.userValue", parentProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", parentProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", parentProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("Home.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", parentProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=dummy" command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "dummy"

		try {
			parentProject.apply plugin: 'properties'
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
		// Fix the properties setUp set when it assumed the home file existed.
		parentProject.ext.userProperty = "ParentProject.userValue"
		parentProject.ext.homeProperty = "ParentProject.homeValue"

		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName =  "user"

		def propFile = new File("${parentProject.gradle.gradleUserHomeDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete home file", propFile.exists())

		parentProject.apply plugin: 'properties'
		assertEquals("ParentProject.parentProjectValue", parentProject.parentProjectProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.homeValue", parentProject.homeProperty)
		assertEquals("User.userValue", parentProject.userProperty)
		assertEquals("Environment.environmentValue", parentProject.environmentProperty)
		assertEquals("System.systemValue", parentProject.systemProperty)
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(18, parentProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["childProjectProperty"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["childEnvironmentProperty"])
		assertEquals("ParentEnvironmentLocal.homeValue", parentProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", parentProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", parentProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", parentProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("ParentEnvironmentLocal.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", parentProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no environment file, and we're using the
	 * default "local" file.  This should not be an error.
	 */
	public void testApplyMissingUnspecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"

		def propFile = new File("${parentProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse("Failed to delete local file", propFile.exists())

		parentProject.apply plugin: 'properties'
		assertEquals("ParentProject.parentProjectValue", parentProject.parentProjectProperty)
		assertEquals("ParentProject.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		assertEquals("ParentProject.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentProject.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", parentProject.homeProperty)
		assertEquals("User.userValue", parentProject.userProperty)
		assertEquals("Environment.environmentValue", parentProject.environmentProperty)
		assertEquals("System.systemValue", parentProject.systemProperty)
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(18, parentProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentProject.parentEnvironmentValue", parentProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentProject.childProjectValue", parentProject.filterTokens["childProjectProperty"])
		assertEquals("ParentProject.childEnvironmentValue", parentProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", parentProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", parentProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", parentProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", parentProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentProject.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentProject.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentProject.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", parentProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])

	}

	/**
	 * Test what happens when we have no environment file, but we specify an
	 * environment file.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=dummy -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"
		parentProject.ext.environmentName = "dummy"
		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( FileNotFoundException e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no project property file.  This is no error.
	 */
	public void testApplyMissingProjectFile() {
		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects(false)
		assertFalse("We shouldn't have a parent project property", parentProject.hasProperty("parentProjectProperty"))
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		parentProject.ext.gradleUserName = "user"

		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete project file", propFile.exists())

		parentProject.apply plugin: 'properties'
		assertFalse("We shouldn't have a parent project property", parentProject.hasProperty("parentProjectProperty"))
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", parentProject.homeProperty)
		assertEquals("User.userValue", parentProject.userProperty)
		assertEquals("Environment.environmentValue", parentProject.environmentProperty)
		assertEquals("System.systemValue", parentProject.systemProperty)
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(16, parentProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["childProjectProperty"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", parentProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", parentProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", parentProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", parentProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", parentProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", parentProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	public void testChangeEnvironmentNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangeGradleUserNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'
		parentProject.ext.gradleUserName = 'user'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testSetGradleUserNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testReSetEnvironmentNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = bad"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}

	public void testChangePropertiesPluginEnvironmentNameProperty() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty =  'dummyEnvironmentName'
		parentProject.apply plugin: 'properties'

		assertFalse(parentProject.hasProperty('environmentName'))
		assertTrue(parentProject.hasProperty('dummyEnvironmentName'))
		assertEquals('local', parentProject.dummyEnvironmentName)
		assertTrue(parentProject.hasProperty('parentEnvironmentProperty'))
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertFalse('ParentEnvironmentTest.parentEnvironmentValue'.equals(parentProject.parentEnvironmentProperty))
	}

	public void testSetPropertiesPluginEnvironmentNamePropertyValue() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty =  'dummyEnvironmentName'
		parentProject.ext.dummyEnvironmentName =  'test'
		parentProject.apply plugin: 'properties'

		assertFalse(parentProject.hasProperty('environmentName'))
		assertTrue(parentProject.hasProperty('dummyEnvironmentName'))
		assertEquals('test', parentProject.dummyEnvironmentName)
		assertTrue(parentProject.hasProperty('parentEnvironmentProperty'))
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertFalse('ParentEnvironmentLocal.parentEnvironmentValue'.equals(parentProject.parentEnvironmentProperty))
	}

	public void testChangePropertiesPluginGradleUserNameProperty() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.apply plugin: 'properties'

		assertFalse('User.userValue'.equals(parentProject.userProperty))
	}

	public void testSetPropertiesPluginGradleUserNamePropertyValue() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.ext.dummyGradleUserName =  'user'
		parentProject.apply plugin: 'properties'

		assertEquals('user', parentProject.dummyGradleUserName)
		assertTrue(parentProject.hasProperty('userProperty'))
		assertEquals('User.userValue', parentProject.userProperty)
	}

	public void testChangePropertiesPluginGradleUserNamePropertyValueWithMissingFile() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.ext.dummyGradleUserName =  'dummy'

		shouldFail(FileNotFoundException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangePropertiesPluginEnvironmentNamePropertyValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "propertiesPluginEnvironmentNameProperty = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangePropertiesPluginEnvironmentNamePropertyValueValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "dummyEnvironmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty = 'dummyEnvironmentName'
		parentProject.ext.dummyEnvironmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangeEnvironmentNameValueWithChangedPropertiesPluginEnvironmentNameProperty() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty = 'dummyEnvironmentName'
		parentProject.ext.dummyEnvironmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}

	public void testChangePropertiesPluginGradleUserNamePropertyValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "propertiesPluginGradleUserNameProperty = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangePropertiesPluginGradleUserNamePropertyValueValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "dummyGradleUserName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty = 'dummyGradleUserName'
		parentProject.ext.environmentName = 'bad'
		parentProject.ext.dummyGradleUserName = 'user'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	public void testChangeGradleUserNameValueWithChangedPropertiesPluginGradleUserNameProperty() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = user"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty = 'dummyGradleUserName'
		parentProject.ext.environmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}
}
