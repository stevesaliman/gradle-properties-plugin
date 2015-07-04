/*
 * Copyright 2012-2015 Steven C. Saliman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
	def parentTask = null;
	def childTask = null;
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

		// Add a task we can check for properties.
		parentTask = parentProject.task('myTest')

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

		// Add a task we can check for properties.
		childTask = childProject.task('myTest')

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
		builder.copy(file:'src/test/resources/parent-env-local-sub.properties',
						tofile : "${parentProject.projectDir}/gradle-properties/gradle-local.properties")

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
		builder.copy(file:'src/test/resources/child-env-local-sub.properties',
						tofile : "${childProject.projectDir}/gradle-properties/gradle-local.properties")
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

	/**
	 * Test the CheckProperty method when the property is missing.
	 */
	public void testCheckPropertyPresent() {
		parentProject.ext.someProperty = 'someValue'
		// we succeed if we don't get an exception.
		plugin.checkProperty(parentProject, 'someProperty', parentTask, 'someMethod')
		def inputValue = parentTask.inputs.properties['someProperty']
		assertEquals('Failed to register the task input', 'someValue', inputValue)
	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyMissing() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(parentProject, 'someProperty', parentTask, 'someMethod')
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
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)

		assertEquals('Command.commandValue', parentProject.commandProperty)
		def testFilter = tokens['commandProperty']
		assertEquals('Command.commandValue', testFilter)
		testFilter = tokens['command.property']
		assertEquals('Command.commandValue', testFilter)
	}

	/**
	 * Verify that when a property is set in all the files, but not the command
	 * line, system properties, or environment variables, the user value wins,
	 * since it is the highest priority file.
	 */
	public void testApplyUserProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('User.userValue', parentProject.userProperty)

		def testFilter = tokens['userProperty']
		assertEquals('User.userValue', testFilter)
		testFilter = tokens['user.property']
		assertEquals('User.userValue', testFilter)
	}

	/**
	 * Verify that when a file-based property is set in all files, but no user is
	 * given, we get the value from the home file.
	 */
	public void testApplyUserPropertyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		assertEquals('local', parentProject.environmentName)
		assertEquals('Home.userValue', parentProject.userProperty)
		def testFilter = tokens['userProperty']
		assertEquals('Home.userValue', testFilter)
		testFilter = tokens['user.property']
		assertEquals('Home.userValue', testFilter)
	}

	/**
	 * Verify that when a file-based property is set everywhere but the user file,
	 * the home file wins.
	 */
	public void testApplyHomeProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		assertEquals('local', parentProject.environmentName)

		assertEquals('Home.homeValue', parentProject.homeProperty)
		def testFilter = tokens['homeProperty']
		assertEquals('Home.homeValue', testFilter)
		testFilter = tokens['home.property']
		assertEquals('Home.homeValue', testFilter)
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
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local' , parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		def testFilter = tokens['childEnvironmentProperty']
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', testFilter)
		testFilter = tokens['parentEnvironmentProperty']
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', testFilter)
		testFilter = tokens['child.environment.property']
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', testFilter)
		testFilter = tokens['parent.environment.property']
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', testFilter)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * we don't specify an environment, but we do specify a property directory,
	 * the local environment file wins, but that we use the file in the directory
	 * and not the one at the project level. This also makes sure the files in
	 * the child project are ignored when we apply at a parent level.
	 */
	public void testApplyUseDefaultFileInDirectory() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user
		// -PenvironmentFileName=gradle-properties" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local' , parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		def testFilter = tokens['childEnvironmentProperty']
		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', testFilter)
		testFilter = tokens['parentEnvironmentProperty']
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', testFilter)
		testFilter = tokens['child.environment.property']
		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', testFilter)
		testFilter = tokens['parent.environment.property']
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', testFilter)
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we do specify an environment, the specified environment file wins.
	 * This also verifies that properties in child project files are ignored.
	 */
	public void testApplyUseAlternateFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user',
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('test', parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentEnvironmentTest.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		def testFilter = tokens['childEnvironmentProperty']
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', testFilter)
		testFilter = tokens['parentEnvironmentProperty']
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', testFilter)
		testFilter = tokens['child.environment.property']
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', testFilter)
		testFilter = tokens['parent.environment.property']
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', testFilter)
	}

	/**
	 * Verify that when we only specify a property in the project property file,
	 * it still gets set and is in the filters.  This also verifies that we
	 * ignore properties set in child project files.
	 */
	public void testApplyProjectProperties() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentEnvironmentLocal.childProjectValue', parentProject.childProjectProperty)
		assertEquals('ParentProject.parentProjectValue', parentProject.parentProjectProperty)
		def testFilter = tokens['childProjectProperty']
		assertEquals('ParentEnvironmentLocal.childProjectValue', testFilter)
		testFilter = tokens['parentProjectProperty']
		assertEquals('ParentProject.parentProjectValue', testFilter)
		testFilter = tokens['child.project.property']
		assertEquals('ParentEnvironmentLocal.childProjectValue', testFilter)
		testFilter = tokens['parent.project.property']
		assertEquals('ParentProject.parentProjectValue', testFilter)
	}

	// This set of tests tests what happens when certain files are missing.
	// To be thorough, we'll test all the properties and tokens.

	/**
	 * Test applying the plugin when we have no user file, but we didn't specify
	 * a user.  This is not an error.  Remember that child files should not be
	 * processed.
	 */
	public void testApplyMissingUnspecifiedUserFile() {
		def propFile = new File("${parentProject.gradle.gradleUserHomeDir}/gradle-user.properties")
		propFile.delete()
		assertFalse('Failed to delete user file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertFalse(parentProject.hasProperty('gradleUserName'))

		assertEquals('ParentProject.parentProjectValue', parentProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.childProjectValue', parentProject.childProjectProperty)
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', parentProject.homeProperty)
		assertEquals('Home.userValue', parentProject.userProperty)
		assertEquals('Environment.environmentValue', parentProject.environmentProperty)
		assertEquals('System.systemValue', parentProject.systemProperty)
		assertEquals('Command.commandValue', parentProject.commandProperty)

		assertEquals(18, tokens.size())
		// camel case notation
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('Home.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['child.project.property'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('Home.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PgradleUserName=dummy" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing a user file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no home file.  This should not produce an
	 * error.
	 */
	public void testApplyMissingHomeFile() {
		// delete the home file
		def propFile = new File("${parentProject.gradle.gradleUserHomeDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete home file', propFile.exists())

		// Fix the properties setUp set when it assumed the home file existed.
		parentProject.ext.userProperty = 'ParentProject.userValue'
		parentProject.ext.homeProperty = 'ParentProject.homeValue'

		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', parentProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.childProjectValue', parentProject.childProjectProperty)
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.homeValue', parentProject.homeProperty)
		assertEquals('User.userValue', parentProject.userProperty)
		assertEquals('Environment.environmentValue', parentProject.environmentProperty)
		assertEquals('System.systemValue', parentProject.systemProperty)
		assertEquals('Command.commandValue', parentProject.commandProperty)

		assertEquals(20, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('ParentEnvironmentLocal.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['child.project.property'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('ParentEnvironmentLocal.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])
	}

	/**
	 * Test what happens when we have no environment file, and we're using the
	 * default "local" file.  This should not be an error.
	 */
	public void testApplyMissingUnspecifiedEnvFile() {
		// delete the local file.
		def propFile = new File("${parentProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse('Failed to delete local file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', parentProject.parentProjectProperty)
		assertEquals('ParentProject.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertEquals('ParentProject.childProjectValue', parentProject.childProjectProperty)
		assertEquals('ParentProject.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', parentProject.homeProperty)
		assertEquals('User.userValue', parentProject.userProperty)
		assertEquals('Environment.environmentValue', parentProject.environmentProperty)
		assertEquals('System.systemValue', parentProject.systemProperty)
		assertEquals('Command.commandValue', parentProject.commandProperty)

		assertEquals(20, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ParentProject.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ParentProject.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

	}

	/**
	 * Test what happens when we have no environment file, but we specify an
	 * environment file.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=dummy -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'dummy',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no project property file.  This is no error.
	 */
	public void testApplyMissingProjectFile() {
		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete project file', propFile.exists())

		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects(false)
		assertFalse("We shouldn't have a parent project property", parentProject.hasProperty('parentProjectProperty'))

		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('user', parentProject.gradleUserName)

		assertFalse("We shouldn't have a parent project property", parentProject.hasProperty('parentProjectProperty'))
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.childProjectValue', parentProject.childProjectProperty)
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', parentProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', parentProject.homeProperty)
		assertEquals('User.userValue', parentProject.userProperty)
		assertEquals('Environment.environmentValue', parentProject.environmentProperty)
		assertEquals('System.systemValue', parentProject.systemProperty)
		assertEquals('Command.commandValue', parentProject.commandProperty)
		assertEquals(18, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['child.project.property'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])
	}
}
