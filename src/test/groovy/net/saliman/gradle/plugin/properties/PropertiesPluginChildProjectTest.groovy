/*
 * Copyright 2012-2016 Steven C. Saliman
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
/**
 * Test class for child projects that apply Properties plugin.  In a multi
 * project build, parent and child projects use slightly different files.  A
 * child project will inherit from the parent project's property files, but
 * the parent project does not use the child project's files.  This test has
 * some more to it than the parent test because the parent test doesn't deal
 * with child properties or project inheritance.
 * <p>
 * Each test also applies the plugin to the parent project before applying it
 * to the child project.  This simulates what Gradle itself does, but also tests
 * a fix for a plugin bug that was caused by the fact that some Gradle API
 * methods inherit from parent projects, and others don't and we want to make
 * sure child extension properties are set even if they existed in the parent.
 * This is also why we test for {@code childProject.environmentName} (which
 * inherits), and {@code childProject.ext.environmentName} (which doesn't).
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
class PropertiesPluginChildProjectTest extends BasePluginTest {
	/**
	 * Set up the test data.  This calls a helper method to create the projects
	 * because we need to repeat the setup in one of the tests.
	 */
	public void setUp() {
		createProjects()
		setFileProperties(true, true)
		copyFiles()
	}

	/**
	 * Test the CheckProperty method when the property is present.
	 */
	public void testCheckPropertyPresent() {
		childProject.ext.someProperty = 'someValue'
		// we succeed if we don't get an exception.
		plugin.checkProperty(childProject, 'someProperty', childTask, 'someMethod')
		def inputValue = childTask.inputs.properties['someProperty']
		assertEquals('Failed to register the task input', 'someValue', inputValue)

	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyMissing() {
		// we succeed if we don't get an exception.
		shouldFail(MissingPropertyException) {
			plugin.checkProperty(childProject, 'someProperty', childTask, 'someMethod')
		}

	}

	// This set of tests focuses on the different ways the user can invoke the
	// properties plugin.

	/**
	 * Verify that each property came from the correct file.  In this case,
	 * all files exist, and we specify a user and environment, environment
	 * variables are set, system properties are set, and we have a command line
	 * value.
	 */
	public void testApplyPluginWithAll() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation.
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Apply the plugin with everything except for a command line property.  This
	 * is the same as the last test, except that the command property should
	 * come from the system properties.
	 */
	public void testApplyPluginNoCommandLine() {
		// simulate a "-PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('System.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('System.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('System.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Apply the plugin with everything except for system properties.  In this
	 * case, the property normally set from system properties will inherit the
	 * value from the environment variables.
	 */
	public void testApplyPluginNoSystemProperties() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue  -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, false, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('Environment.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('Environment.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('Environment.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Apply the plugin with everything except for environment variables.  In this
	 * case, the property that is normally set by environment variables will
	 * inherit the value from the User file.
	 */
	public void testApplyPluginNoEnvironmentVariables() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(false, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('User.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 12 from system properties.
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('User.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('User.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test applying the plugin when no user is given. In this case, the property
	 * that usually comes from the user file will inherit the value from the
	 * home file.
	 */
	public void testApplyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test" command
		// line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertFalse(childProject.hasProperty('gradleUserName'))

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('Home.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 20 we're checking + 14 from system properties.
		assertEquals(34, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('Home.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('Home.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('Home.userValue', System.properties['userProp'])
		assertEquals('Home.commandValue', System.properties['commandProp'])
	}

	/**
	 * Apply the plugin when no environment is specified.  This should apply
	 * environment properties from the Local file instead of the Test file.
	 * This will also verify that the child environment files override the
	 * parent environment files.
	 */
	public void testApplyUseDefaultEnvironmentFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user" command
		// line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// The 20 we're checking + 14 from system properties.
		assertEquals(34, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Apply the plugin when no environment is specified, but we do specify a
	 * directory.  This should apply environment properties from the Local file
	 * instead of the Test file, and it should use the one in the property
	 * directory.  This will also verify that the child environment files
	 * override the parent environment files.
	 */
	public void testApplyUseDefaultEnvironmentFileInDirectory() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user
		// -PenvironmentFileDir=gradle-properties" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentSubLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('gradle-properties', tokens['environmentFileDir'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentSubLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('gradle-properties', tokens['environment.file.dir'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentSubLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentSubLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	// This set of tests tests what happens when certain files are missing.

	/**
	 * Test applying the plugin when we have no user file, but we didn't specify
	 * a user.  This is not an error.
	 */
	public void testApplyMissingUnspecifiedUserFile() {
		def propFile = new File("${childProject.gradle.gradleUserHomeDir}/gradle-user.properties")
		propFile.delete()
		assertFalse('Failed to delete user file', propFile.exists())
		childProject.properties.remove('gradleUserName')

		// simulate a "-PcommandProperty=Command.commandValue"
		// -PsystemProp.commandProp=commandValue command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertFalse(childProject.hasProperty('gradleUserName'))

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('Home.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 18 we're looking for + 14 from system properties
		assertEquals(32, tokens.size())
		// camel case notation
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('Home.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('Home.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('Home.userValue', System.properties['userProp'])
		assertEquals('Home.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test applying the plugin when we have no user file for a specified user.
	 * This should produce an error
	 */
	public void testApplyMissingSpecifiedUserFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=dummy" command
		// line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			// Only apply the child because we want to make sure the error happens on
			// the child project.
			childProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing a user file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Test what happens when we have no home file.  This should not produce an
	 * error, but the home property should be inherited from the child environment
	 * file.
	 */
	public void testApplyMissingHomeFile() {
		def propFile = new File("${childProject.gradle.gradleUserHomeDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete home file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user" command
		// line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('ChildEnvironmentLocal.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 20 we're looking for + 14 from system properties.
		assertEquals(34, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('ChildEnvironmentLocal.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('ChildEnvironmentLocal.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test applying the plugin when we are missing the child environment file,
	 * but we have the parent environment file.  In this case, we should still
	 * get the property set in the parent environment file, but the property
	 * usually set in the child environment file should come from the child
	 * project file, which has precedence.
	 */
	public void testApplyMissingChildEnvFile() {
		def propFile = new File("${childProject.projectDir}/gradle-test.properties")
		propFile.delete()
		assertFalse('Failed to delete child test file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildProject.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're looking for + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildProject.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildProject.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test applying the plugin when we are missing the parent environment file,
	 * but we have the child environment file.  In this case, the property that
	 * would have been set in the parent environment file will have the value from
	 * the parent project file.
	 */
	public void testApplyMissingParentEnvFile() {
		def propFile = new File("${parentProject.projectDir}/gradle-test.properties")
		propFile.delete()
		assertFalse('Failed to delete parent test file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		// We shouldn't be able to apply to the parent, but the child should run
		// just fine.
		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing a parent environment file.")
		} catch ( Exception e) {
			// this was expected.
		}
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentProject.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties.
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentProject.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentProject.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentProject.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test what happens when we have no environment file in either project, but
	 * we specify an environment file - in other words, we specified an invalid
	 * environment.  This should be an error.
	 */
	public void testApplyMissingSpecifiedEnvFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=dummy -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			// Only apply the child because we want to make sure the error happens on
			// the child project.
			childProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( Exception e) {
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
		def propFile = new File("${parentProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse('Failed to delete parent local file', propFile.exists())
		propFile = new File("${childProject.projectDir}/gradle-local.properties")
		propFile.delete()
		assertFalse('Failed to delete child local file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentProject.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildProject.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		/// 20 we're checking + 14 from system properties
		assertEquals(34, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildProject.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentProject.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildProject.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentProject.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentProject.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentProject.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test what happens when we have no gradle.properties in the parent project,
	 * but we do have one in the child project.  In this case, we won't ever get
	 * the property that should be set in the parent project file, but we'll get
	 * the rest of them.
	 */
	public void testApplyMissingParentProjectFile() {
		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete parent project file', propFile.exists())

		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects()
		setFileProperties(false, true)
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertFalse("We shouldn't have a parent project property", childProject.hasProperty('parentProjectProperty'))
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 18 we're checking + 12 from system properties.
		assertEquals(30, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ChildProject.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ChildProject.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertNull(System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test applying the plugin when we are missing the gradle.properties file
	 * in the child project, but we have one in the parent project.  In this case,
	 * the property that usually gets set in the child project file will come
	 * from the parent project's environment file.
	 */
	public void testApplyMissingChildProjectFile() {
		def propFile = new File("${childProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete child project file', propFile.exists())

		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test', childProject.environmentName)
		assertEquals('test', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ParentEnvironmentTest.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 22 we're checking + 14 from system properties
		assertEquals(36, tokens.size())
		// camel case notation
		assertEquals('test', tokens['environmentName'])
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentProject.parentProjectValue', tokens['parentProjectProperty'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentEnvironmentTest.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('test', tokens['environment.name'])
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentProject.parentProjectValue', tokens['parent.project.property'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentEnvironmentTest.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentTest.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Test what happens when we have no gradle.properties in the either project.
	 * In this case, we won't ever get the property that should be set in the
	 * parent project file, but we'll still get the child project property from
	 * the parent environment file.
	 */
	public void testApplyMissingBothProjectFiles() {
		def propFile = new File("${parentProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete parent project file', propFile.exists())
		propFile = new File("${childProject.projectDir}/gradle.properties")
		propFile.delete()
		assertFalse('Failed to delete child project file', propFile.exists())

		// we can't unset a property once it has been set, so redo the setup,
		// skipping the project property since Gradle would not have set it when
		// the project file is missing.
		createProjects()
		setFileProperties(false, false)
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local', childProject.environmentName)
		assertEquals('local', childProject.ext.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertFalse("We shouldn't have a parent project property", childProject.hasProperty('parentProjectProperty'))
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ParentEnvironmentLocal.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)

		// 18 we're checking + 12 from system properties.
		assertEquals(30, tokens.size())
		// camel case notation
		assertEquals('user', tokens['gradleUserName'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['childProjectProperty'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['childEnvironmentProperty'])
		assertEquals('Home.homeValue', tokens['homeProperty'])
		assertEquals('User.userValue', tokens['userProperty'])
		assertEquals('Environment.environmentValue', tokens['environmentProperty'])
		assertEquals('System.systemValue', tokens['systemProperty'])
		assertEquals('Command.commandValue', tokens['commandProperty'])
		// dot notation
		assertEquals('user', tokens['gradle.user.name'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', tokens['child.project.property'])
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', tokens['child.environment.property'])
		assertEquals('Home.homeValue', tokens['home.property'])
		assertEquals('User.userValue', tokens['user.property'])
		assertEquals('Environment.environmentValue', tokens['environment.property'])
		assertEquals('System.systemValue', tokens['system.property'])
		assertEquals('Command.commandValue', tokens['command.property'])

		// Check the system properties. Remember, system properties are not
		// set from command line properties, and values from child project
		// files are ignored.
		assertNull(System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}


	// child standard property overrides parent property

	// parent used envName, child uses something else. - this will be ok when we're building the child from the child.
}
