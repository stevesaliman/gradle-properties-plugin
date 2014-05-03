package net.saliman.gradle.plugin.properties

import org.gradle.testfixtures.ProjectBuilder

/**
 * Test class for child projects that apply Properties plugin.  In a multi
 * project build, parent and child projects use slightly different files.  A
 * child project will inherit from the parent project's property files, but
 * the parent project does not use the child project's files.  This test has
 * some more to it than the parent test because the parent test doesn't deal
 * with child properties or project inheritance.
 * <p>
 * Note that we set properties in each test because we can't seem to clear a
 * property once it is set, and Gradle itself reads some of the properties
 * before we apply the plugin.
 * <p>
 * The property files are set up so that there is a property representing each
 * file.  Each file sets the property for that file, plus all the files below
 * it (with more precedence) in the sequence.  That way, when we don't have
 * (or don't use) a file, the values from that file should be set by the file
 * one level up.
 * @author Steven C. Saliman
 */
class PropertiesPluginChildProjectTest extends GroovyTestCase {
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
		parentProject.ext.userProperty = "Home.userValue"
		parentProject.ext.homeProperty = "Home.homeValue"
		parentProject.ext.childEnvironmentProperty = "ParentProject.childEnvironmentValue"
		parentProject.ext.childProjectProperty = "ParentProject.childProjectValue"
		parentProject.ext.parentEnvironmentProperty = "ParentProject.parentEnvironmentValue"
		if ( includeProjectProperties ) {
			parentProject.ext.parentProjectProperty = "ParentProject.parentProjectValue"
		}

		// Create the child project.
		def childProjectDir = new File("build/test/parentProject/childProject")
		childProject = ProjectBuilder
						.builder()
						.withName("childProject")
						.withParent(parentProject)
						.withProjectDir(childProjectDir)
						.build();
		childProject.ext.userProperty = "Home.userValue"
		childProject.ext.homeProperty = "Home.homeValue"
		childProject.ext.childEnvironmentProperty = "ChildProject.childEnvironmentValue"
		childProject.ext.childProjectProperty = "ChildProject.childProjectValue"
		childProject.ext.parentEnvironmentProperty = "ParentProject.parentEnvironmentValue"
		if ( includeProjectProperties ) {
			childProject.ext.parentProjectProperty = "ParentProject.parentProjectValue"
		}

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
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${parentUserDir}/gradle-user.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${parentUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/parent-env-local.properties',
						tofile : "${parentProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/parent-env-test.properties',
						tofile : "${parentProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/parent-project-gradle.properties',
						tofile : "${parentProject.projectDir}/gradle.properties")

		// copy the files for the child project.
		def childUserDir = childProject.gradle.gradleUserHomeDir
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${childUserDir}/gradle-user.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${childUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/child-env-local.properties',
						tofile : "${childProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/child-env-test.properties',
						tofile : "${childProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/child-project-gradle.properties',
						tofile : "${childProject.projectDir}/gradle.properties")
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
			def commandProps = childProject.gradle.startParameter.projectProperties
			commandProps.commandProperty = "Command.commandValue"
		}

	}

	/**
	 * Test the CheckProperty method when the property is missing.
	 */
	public void testCheckPropertyMissing() {
		childProject.ext.someProperty = "someValue"
		// we succeed if we don't get an exception.
		plugin.checkProperty(childProject, "someProperty", "someTask")

	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyPresent() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(childProject, "someProperty", "someTask")
		}

	}

	// This set of tests focuses on the different ways the user can invoke the
	// properties plugin.

	/**
	 * Verify that each property came from the correct file.  In this case,
	 * all files exist, and we specify a user and environment, environment
	 * variables are set, system properties are set, and we have a command line
	 * value
	 */
	public void testApplyPluginWithAll() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = 'user'
		childProject.ext.environmentName = 'test'
		childProject.apply plugin: 'properties'
		assertEquals("test", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation.
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Apply the plugin with everything except for a command line property.  This
	 * is the same as the last test, except that the command property should
	 * come from the system properties.
	 */
	public void testApplyPluginNoCommandLine() {
		// simulate a "-PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, false)
		childProject.ext.gradleUserName = 'user'
		childProject.ext.environmentName = 'test'
		childProject.apply plugin: 'properties'
		assertEquals("test", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("System.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("System.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("System.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Apply the plugin with everything except for system properties.  In this
	 * case, the property normally set from system properties will inherit the
	 * value from the environment variables.
	 */
	public void testApplyPluginNoSystemProperties() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, false, true)
		childProject.ext.gradleUserName = 'user'
		childProject.ext.environmentName = 'test'
		childProject.apply plugin: 'properties'
		assertEquals("test", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("Environment.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("Environment.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("Environment.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Apply the plugin with everything except for environment variables.  In this
	 * case, the property that is normally set by environment variables will
	 * inherit the value from the User file.
	 */
	public void testApplyPluginNoEnvironmentVariables() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(false, true, true)
		childProject.ext.gradleUserName = 'user'
		childProject.ext.environmentName = 'test'
		childProject.apply plugin: 'properties'
		assertEquals("test", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("User.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("User.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("User.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when no user is given. In this case, the property
	 * that usually comes from the user file will inherit the value from the
	 * home file.
	 */
	public void testApplyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue -PenvironmentName=test"
		// command line
		setNonFileProperties(true, true, true)
		childProject.ext.environmentName = 'test'

		childProject.apply plugin: 'properties'
		assertEquals("test", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("Home.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("Home.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("Home.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Apply the plugin when no environment is specified.  This should apply
	 * environment properties from the Local file instead of the Test file.
	 * This will also verify that the child environment files override the
	 * parent environment files.
	 */
	public void testApplyUseDefaultEnvironmentFile() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName =  "user"
		childProject.apply plugin: 'properties'
		assertEquals("local", childProject.environmentName)

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	// This set of tests tests what happens when certain files are missing.

	/**
	 * Test applying the plugin when we have no user file, but we didn't specify
	 * a user.  This is not an error.
	 */
	public void testApplyMissingUnspecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue" command line
		setNonFileProperties(true, true, true)

		def propFile = new File("${childProject.gradle.gradleUserHomeDir}/gradle-user.properties")
		propFile.delete()
		assertFalse("Failed to delete user file", propFile.exists())
		childProject.properties.remove("gradleUserName")
		childProject.apply plugin: 'properties'

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("Home.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("Home.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("Home.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=dummy" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "dummy"
		try {
			childProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing a user file.")
		} catch ( FileNotFoundException e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no home file.  This should not produce an
	 * error, but the home property should be inherited from the child environment
	 * file.
	 */
	public void testApplyMissingHomeFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName =  "user"

		def propFile = new File("${childProject.gradle.gradleUserHomeDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete home file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("ChildEnvironmentLocal.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("ChildEnvironmentLocal.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("ChildEnvironmentLocal.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we are missing the child environment file,
	 * but we have the parent environment file.  In this case, we should still
	 * get the property set in the parent environment file, but the property
	 * usually set in the child environment file should come from the child
	 * project file, which has precedence.
	 */
	public void testApplyMissingChildEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"
		childProject.ext.environmentName = 'test'

		def propFile = new File("${childProject.projectDir}/gradle-test.properties")
		propFile.delete()
		assertFalse("Failed to delete child test file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildProject.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildProject.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildProject.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we are missing the parent environment file,
	 * but we have the child environment file.  In this case, the property that
	 * would have been set in the parent environment file will have the value from
	 * the parent project file.
	 */
	public void testApplyMissingParentEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"
		childProject.ext.environmentName = 'test'

		def propFile = new File("${parentProject.projectDir}/gradle-test.properties")
		propFile.delete()
		assertFalse("Failed to delete parent test file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentProject.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentProject.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentProject.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no environment file in either project, but
	 * we specify an environment file - in other words, we specified an invalid
	 * environment.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=dummy -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"
		childProject.ext.environmentName = "dummy"
		try {
			childProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( FileNotFoundException e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no environment file in either project,
	 * but we didn't specify an environment.  This should not be an error, but
	 * the values that would have come from environment files will come instead
	 * from the project files..
	 */
	public void testApplyMissingUnspecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"
		def propFile = new File("${parentProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse("Failed to delete parent local file", propFile.exists())
		propFile = new File("${childProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse("Failed to delete child local file", propFile.exists())
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentProject.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildProject.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentProject.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildProject.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentProject.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildProject.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no gradle.properties in the parent project,
	 * but we do have one in the child project.  In this case, we won't ever get
	 * the property that should be set in the parent project file, but we'll get
	 * the rest of them.
	 */
	public void testApplyMissingParentProjectFile() {
		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects(false)
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"

		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete parent project file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertFalse("We shouldn't have a parent project property", childProject.hasProperty("parentProjectProperty"))
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ChildProject.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(16, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ChildProject.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test applying the plugin when we are missing the gradle.properties file
	 * in the child project, but we have one in the parent project.  In this case,
	 * the property that usually gets set in the child project file will come
	 * from the parent project's environment file.
	 */
	public void testApplyMissingChildProjectFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"
		childProject.ext.environmentName = 'test'

		def propFile = new File("${childProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete child project file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertEquals("ParentProject.parentProjectValue", childProject.parentProjectProperty)
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ParentEnvironmentTest.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(18, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parentProjectProperty"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentEnvironmentTest.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentProject.parentProjectValue", childProject.filterTokens["parent.project.property"])
		assertEquals("ParentEnvironmentTest.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentTest.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentTest.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}

	/**
	 * Test what happens when we have no gradle.properties in the either project.
	 * In this case, we won't ever get the property that should be set in the
	 * parent project file, but we'll still get the child project property from
	 * the parent environment file.
	 */
	public void testApplyMissingBothProjectFiles() {
		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects(false)
		setNonFileProperties(true, true, true)
		childProject.ext.gradleUserName = "user"

		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete parent project file", propFile.exists())
		 propFile = new File("${childProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse("Failed to delete child project file", propFile.exists())
		childProject.apply plugin: 'properties'

		assertFalse("We shouldn't have a parent project property", childProject.hasProperty("parentProjectProperty"))
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.parentEnvironmentProperty)
		assertEquals("ParentEnvironmentLocal.childProjectValue", childProject.childProjectProperty)
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.childEnvironmentProperty)
		assertEquals("Home.homeValue", childProject.homeProperty)
		assertEquals("User.userValue", childProject.userProperty)
		assertEquals("Environment.environmentValue", childProject.environmentProperty)
		assertEquals("System.systemValue", childProject.systemProperty)
		assertEquals("Command.commandValue", childProject.commandProperty)
		assertEquals(16, childProject.filterTokens.size())
		// camel case notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parentEnvironmentProperty"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", childProject.filterTokens["childProjectProperty"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["childEnvironmentProperty"])
		assertEquals("Home.homeValue", childProject.filterTokens["homeProperty"])
		assertEquals("User.userValue", childProject.filterTokens["userProperty"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environmentProperty"])
		assertEquals("System.systemValue", childProject.filterTokens["systemProperty"])
		assertEquals("Command.commandValue", childProject.filterTokens["commandProperty"])
		// dot notation
		assertEquals("ParentEnvironmentLocal.parentEnvironmentValue", childProject.filterTokens["parent.environment.property"])
		assertEquals("ParentEnvironmentLocal.childProjectValue", childProject.filterTokens["child.project.property"])
		assertEquals("ChildEnvironmentLocal.childEnvironmentValue", childProject.filterTokens["child.environment.property"])
		assertEquals("Home.homeValue", childProject.filterTokens["home.property"])
		assertEquals("User.userValue", childProject.filterTokens["user.property"])
		assertEquals("Environment.environmentValue", childProject.filterTokens["environment.property"])
		assertEquals("System.systemValue", childProject.filterTokens["system.property"])
		assertEquals("Command.commandValue", childProject.filterTokens["command.property"])
	}
}
