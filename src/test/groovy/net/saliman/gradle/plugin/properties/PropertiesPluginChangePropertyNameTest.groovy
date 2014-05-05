package net.saliman.gradle.plugin.properties

import org.gradle.api.GradleException
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
class PropertiesPluginChangePropertyNameTest extends GroovyTestCase {
	def plugin = new PropertiesPlugin()
	def parentProject = null;
	def childProject = null;
	def parentCommandProperties = null;
	def childCommandProperties = null;

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
		def parentProjectDir = new File('build/test/parentProject')
		parentProject = ProjectBuilder
						.builder()
						.withName('parentProject')
						.withProjectDir(parentProjectDir)
						.build();
		if ( includeProjectProperties ) {
			parentProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
		parentProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		parentProject.ext.childProjectProperty = 'ParentProject.childProjectValue'
		parentProject.ext.childEnvironmentProperty = 'ParentProject.childEnvironmentValue'
		parentProject.ext.homeProperty = 'Home.homeValue'
		parentProject.ext.userProperty = 'Home.userValue'
		parentCommandProperties = parentProject.gradle.startParameter.projectProperties

		// Create the child project.
		def childProjectDir = new File('build/test/parentProject/childProject')
		childProject = ProjectBuilder
						.builder()
						.withName('childProject')
						.withParent(parentProject)
						.withProjectDir(childProjectDir)
						.build();
		if ( includeProjectProperties ) {
			childProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
		childProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		childProject.ext.childProjectProperty = 'ChildProject.childProjectValue'
		childProject.ext.childEnvironmentProperty = 'ChildProject.childEnvironmentValue'
		childProject.ext.homeProperty = 'Home.homeValue'
		childProject.ext.userProperty = 'Home.userValue'
		childCommandProperties = parentProject.gradle.startParameter.projectProperties

		// Add the child to the parent.
		parentProject.childProjects.put('child', childProject)
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
	 * @param commandProperties a map of properties that should be added to the
	 *        "command line" for our build.
	 */
	public void setNonFileProperties(boolean setEnvironmentProperties,
	                                 boolean setSystemProperties,
	                                 Map commandProperties) {
		if ( setEnvironmentProperties ) {
			SetEnv.setEnv([ 'ORG_GRADLE_PROJECT_environmentProperty' : 'Environment.environmentValue',
							'ORG_GRADLE_PROJECT_systemProperty' : 'Environment.systemValue',
							'ORG_GRADLE_PROJECT_commandProperty' :'Environment.commandValue'])
			// Make sure the utility worked.
			assertEquals('Failed to set ORG_GRADLE_PROJECT_environmentProperty',
							'Environment.environmentValue',
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertEquals('Failed to set ORG_GRADLE_PROJECT_systemProperty',
							'Environment.systemValue',
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertEquals('Failed to set ORG_GRADLE_PROJECT_commandProperty',
							'Environment.commandValue',
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))
		} else {
			SetEnv.unsetEnv(['ORG_GRADLE_PROJECT_environmentProperty',
							'ORG_GRADLE_PROJECT_systemProperty',
							'ORG_GRADLE_PROJECT_commandProperty'])
			// Make sure the utility worked.
			assertNull('Failed to clear ORG_GRADLE_PROJECT_environmentProperty',
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertNull('Failed to clear ORG_GRADLE_PROJECT_systemProperty',
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertNull('Failed to clear ORG_GRADLE_PROJECT_commandProperty',
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))

		}
		if ( setSystemProperties ) {
			System.setProperty('org.gradle.project.systemProperty', 'System.systemValue')
			System.setProperty('org.gradle.project.commandProperty', 'System.commandValue')
		} else {
			System.clearProperty('org.gradle.project.systemProperty')
			System.clearProperty('org.gradle.project.commandProperty')
		}

		// Apply command properties.  This means 2 things:
		// 1 - Add each property to Gradle's list of command line arguments so that
		//     the plugin will process them when the plugin is applied.
		// 2 - Add the property to the project's existing properties since Gradle
		//     would have already added them to the project before applying the
		//     plugin.
		if ( commandProperties != null ) {
			commandProperties.each { key, value ->
				parentProject
				parentCommandProperties[key] = value
				parentProject.ext[key] = value
				childCommandProperties[key] = value
				childProject.ext[key] = value
			}
		}
	}

	// with standard env name.
	// 1. only set in env file - fail

	// 2. only set in user file - fail

	// 3. set in env file and command line, no problem

	// 4. Set in standard file, same value in env file - no problem.

	// With new env name

	// 1. specify in command line, but with no value - use local?  envName="dummy", also fine

	// 2. specify in command line, bad value - fail?

	// 3. specify in command line with good value.  envName= dummy, ok?

	// 4. specify in standard file, value on command line - no problem.

	// 5. specify in standard file, value in special file - fail?

	// 6. specify name and var on command line, special overrides, but no problem.




	// -1 use standard name to set env, but override it in env-properties err?

	// -2 use standard name to set env, but change it in user properties. err?

	// 1. use alternative var to set env, but don't specify value - assume local?

	// 2. use alternative var to set env, specify bad value = err?

	// 3. use alternate var to set env, specify good value. have right one?  Can we use envName as a normal prop?

	// 4. use alternative var to set env, env file overrides it - err (override in normal file tested above.













  // this is #-1
	// project has 'bad', not from command line, property file sets to dummy, fail? - can't really happen.
	// unless we force it in a file....
	public void testChangeEnvironmentNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

  // set property via command line, have differet one in file, no problem.
	public void testChangeEnvironmentNameValueWithoutEffect() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.gradle.startParameter.projectProperties.environmentName = 'bad'
		// As Gradle is not "booted", the start parameter is not
		// transferred to a project property, so we do it manually here
		parentProject.ext.environmentName = 'bad'
		parentProject.apply plugin: 'properties'
		assertEquals('bad', parentProject.environmentName)
	}

	// has user, not from command line.  Override in file. Can't really happen, unless we force in file.
	public void testChangeGradleUserNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'
		parentProject.ext.gradleUserName = 'user'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// has username from special file, nowhere else, bad way to go
	public void testSetGradleUserNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// has value, not from command line, special file sets it to the same thing. no problem.
	public void testReSetEnvironmentNameValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = bad"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}

	// plugin gets new env var name, but no value specified.  Use local?
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

	// has new var for env, that var has value, file is found.  get from test file?
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

	// usr var name set, not on command line, no value given, this is fine.
	public void testChangePropertiesPluginGradleUserNameProperty() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.apply plugin: 'properties'

		assertFalse('User.userValue'.equals(parentProject.userProperty))
	}

	// new user var which has value, not from command line.  Name is good.  right file?
	public void testSetPropertiesPluginGradleUserNamePropertyValue() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.ext.dummyGradleUserName =  'user'
		parentProject.apply plugin: 'properties'

		assertEquals('user', parentProject.dummyGradleUserName)
		assertTrue(parentProject.hasProperty('userProperty'))
		assertEquals('User.userValue', parentProject.userProperty)
	}

	// new user var which has invalid value.  fail?
	public void testChangePropertiesPluginGradleUserNamePropertyValueWithMissingFile() {
		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty =  'dummyGradleUserName'
		parentProject.ext.dummyGradleUserName =  'dummy'

		shouldFail(FileNotFoundException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// standard name used at start, special file sets new var name.  should or should not fail?.
	public void testChangePropertiesPluginEnvironmentNamePropertyValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "propertiesPluginEnvironmentNameProperty = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// project has new env var which has value that points to file that sets new env value.  Fail.
	public void testChangePropertiesPluginEnvironmentNamePropertyValueValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "dummyEnvironmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty = 'dummyEnvironmentName'
		parentProject.ext.dummyEnvironmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// project has new  var for env var, it has good value, that file sets environmentName.  This should be fine.
	public void testChangeEnvironmentNameValueWithChangedPropertiesPluginEnvironmentNameProperty() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "environmentName = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginEnvironmentNameProperty = 'dummyEnvironmentName'
		parentProject.ext.dummyEnvironmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}

	// envName points to file that sets new user var - fail or not?
	public void testChangePropertiesPluginGradleUserNamePropertyValue() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "propertiesPluginGradleUserNameProperty = dummy"

		setNonFileProperties(true, true, true)
		parentProject.ext.environmentName = 'bad'

		shouldFail(GradleException) {
			parentProject.apply plugin: 'properties'
		}
	}

	// project has env pointing to file that overrides new user var.  Fail?
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

	// envName points to file that sets gradleUser, but we're not using that var.  Good.
	public void testChangeGradleUserNameValueWithChangedPropertiesPluginGradleUserNameProperty() {
		new File("${parentProject.projectDir}/gradle-bad.properties").text = "gradleUserName = user"

		setNonFileProperties(true, true, true)
		parentProject.ext.propertiesPluginGradleUserNameProperty = 'dummyGradleUserName'
		parentProject.ext.environmentName = 'bad'
		parentProject.apply plugin: 'properties'
	}
}
