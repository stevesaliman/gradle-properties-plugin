package net.saliman.gradle.plugin.properties

import org.gradle.testfixtures.ProjectBuilder

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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Command.commandValue", parentProject.commandProperty)
		def testFilter = parentProject.filterTokens["command.property"]
		assertEquals("Command.commandValue", testFilter)
	}

	/**
	 * Verify that when a property is set everywhere but the command line, the
	 * the user value wins
	 */
	public void testApplyUserProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("User.userValue", parentProject.userProperty)
		def testFilter = parentProject.filterTokens["user.property"]
		assertEquals("User.userValue", testFilter)
	}

	/**
	 * Verify that when a property is set everywhere but the command line, but
	 * no user is given, we get the value from the home file.
	 */
	public void testApplyUserPropertyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue" command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Home.userValue", parentProject.userProperty)
		def testFilter = parentProject.filterTokens["user.property"]
		assertEquals("Home.userValue", testFilter)
	}

	/**
	 * Verify that when a property is set everywhere but the command line and
	 * user file, the home file wins.
	 */
	public void testApplyHomeProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.ext.gradleUserName = "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("Home.homeValue", parentProject.homeProperty)
		def testFilter = parentProject.filterTokens["home.property"]
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.ext.gradleUserName =  "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		def testFilter = parentProject.filterTokens["child.environment.property"]
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		// simulate the -PenvironmentName=test and -PgradleUserName=user option
		parentProject.ext.gradleUserName =  'user'
		parentProject.ext.environmentName = 'test'
		parentProject.apply plugin: 'properties'
		assertEquals("test", parentProject.environmentName)

		assertEquals("ParentEnvironmentTest.childEnvironmentValue", parentProject.childEnvironmentProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", parentProject.parentEnvironmentProperty)
		def testFilter = parentProject.filterTokens["child.environment.property"]
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
		parentProject.ext.gradleUserName =  "user"
		parentProject.apply plugin: 'properties'
		assertEquals("local", parentProject.environmentName)

		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.childProjectProperty)
		assertEquals("ParentProject.parentProjectValue", parentProject.parentProjectProperty)
		def testFilter = parentProject.filterTokens["child.project.property"]
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"

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
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(7, parentProject.filterTokens.size())
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("Home.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=dummy" command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
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
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(7, parentProject.filterTokens.size())
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("ParentEnvironmentLocal.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no environment file, and we're using the
	 * default "local" file.  This should not be an error.
	 */
	public void testApplyMissingUnspecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
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
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(7, parentProject.filterTokens.size())
		assertEquals("ParentProject.parentProjectValue", parentProject.filterTokens["parent.project.property"])
		assertEquals("ParentProject.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentProject.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentProject.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no environment file, but we specify an
	 * environment file.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=dummy -PgradleUserName=user" command line
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
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
		def commandProps = parentProject.gradle.startParameter.projectProperties
		commandProps.commandProperty = "Command.commandValue"
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
		assertEquals("Command.commandValue", parentProject.commandProperty)
		assertEquals(6, parentProject.filterTokens.size())
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", parentProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", parentProject.filterTokens["child.project.property"])
		assertEquals("ParentEnvironmentLocal.childEnvironmentValue", parentProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", parentProject.filterTokens["home.property"])
		assertEquals("User.userValue", parentProject.filterTokens["user.property"])
		assertEquals("Command.commandValue", parentProject.filterTokens["command.property"])
	}
}
