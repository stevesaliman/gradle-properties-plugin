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
class PropertiesPluginParentProjectTest extends BasePluginTest {

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
	 * Prior to fixing GitHub issue #22, the plugin would attempt to convert all
	 * camel case properties to dot notation (propertyName -> property.name), in
	 * the filterTokens, but this made some strange, unnecessary tokens when we
	 * have a property that is all upper case (PROPERTY_NAME became
	 * .p.r.o.p.e.r.t.y_.n.a.m.e).  This test makes sure we don't do that any
	 * more.
	 */
	public void testApplyUpperCommandProperty() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user"
		// command line
		def commandArgs = [
						UPPER_PROPERTY: 'Command.upperValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)

		assertEquals('Command.upperValue', parentProject.UPPER_PROPERTY)
		def testFilter = tokens['UPPER_PROPERTY']
		assertEquals('Command.upperValue', testFilter)
		// Prior to fixing Github issue #22, the plugin would create this strange,
		// unneeded property.
		testFilter = tokens['.u.p.p.e.r_.p.r.o.p.e.r.t.y']
		assertNull(testFilter)
	}

	/**
	 * Verify that a command line value overrides everything else, and that we
	 * don't set system properties from it.
	 */
	public void testApplyCommandProperty() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user" command
		// line
		def commandArgs = [
						'commandProperty': 'Command.commandValue',
				        'systemProp.commandProp': 'Command.commandValue',
						'gradleUserName': 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)

		assertEquals('Command.commandValue', parentProject.commandProperty)
		def testFilter = tokens['commandProperty']
		assertEquals('Command.commandValue', testFilter)
		testFilter = tokens['command.property']
		assertEquals('Command.commandValue', testFilter)

		// check the property that looks like it should set a system property.
		// The build property should be set, but per gradle docs, it should be
		// ignored when setting a command property.
		assertEquals('Command.commandValue', parentProject.'systemProp.commandProp')
		assertEquals('User.commandValue', System.properties['commandProp'])
	}

	/**
	 * Verify that when a property is set in all the files, but not the command
	 * line, system properties, or environment variables, the user value wins,
	 * since it is the highest priority file.
	 */
	public void testApplyUserProperty() {
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
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
		assertEquals('User.userValue', parentProject.userProperty)

		def testFilter = tokens['userProperty']
		assertEquals('User.userValue', testFilter)
		testFilter = tokens['user.property']
		assertEquals('User.userValue', testFilter)

		// Check the system property
		assertEquals('User.userValue', parentProject.'systemProp.userProp')
		assertEquals('User.userValue', System.properties['userProp'])
	}

	/**
	 * Verify that when a file-based property is set in all files, but no user is
	 * given, we get the value from the home file.
	 */
	public void testApplyUserPropertyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
		assertEquals('Home.userValue', parentProject.userProperty)
		def testFilter = tokens['userProperty']
		assertEquals('Home.userValue', testFilter)
		testFilter = tokens['user.property']
		assertEquals('Home.userValue', testFilter)

		// Check the system property
		assertEquals('Home.userValue', parentProject.'systemProp.userProp')
		assertEquals('Home.userValue', System.properties['userProp'])
	}

	/**
	 * Verify that when a file-based property is set everywhere but the user
	 * file, the home file wins.
	 */
	public void testApplyHomeProperty() {
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
		def tokens = parentProject.filterTokens

		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)

		assertEquals('Home.homeValue', parentProject.homeProperty)
		def testFilter = tokens['homeProperty']
		assertEquals('Home.homeValue', testFilter)
		testFilter = tokens['home.property']
		assertEquals('Home.homeValue', testFilter)

		// Check the system property
		assertEquals('Home.homeValue', parentProject.'systemProp.homeProp')
		assertEquals('Home.homeValue', System.properties['homeProp'])
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we don't specify an environment, the local environment file wins.
	 * This also makes sure the files in the child project are ignored when we
	 * apply at a parent level.
	 */
	public void testApplyUseDefaultFile() {
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
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
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

		// Check the system properties
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', parentProject.'systemProp.childEnvironmentProp')
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.'systemProp.parentEnvironmentProp')
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * we don't specify an environment, but we do specify a property directory,
	 * the local environment file wins, but that we use the file in the directory
	 * and not the one at the project level. This also makes sure the files in
	 * the child project are ignored when we apply at a parent level.
	 */
	public void testApplyUseDefaultFileInDirectory() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PgradleUserName=user
		// -PenvironmentFileName=gradle-properties" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						gradleUserName: 'user',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
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

		// Check the system properties
		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', parentProject.'systemProp.childEnvironmentProp')
		assertEquals('ParentEnvironmentSubLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', parentProject.'systemProp.parentEnvironmentProp')
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Verify that when a property is set in the environment and project files,
	 * and we do specify an environment, the specified environment file wins.
	 * This also verifies that properties in child project files are ignored.
	 */
	public void testApplyUseAlternateFile() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PsystemProp.commandProp=commandValue -PenvironmentName=test
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user',
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('test', parentProject.environmentName)
		assertEquals('test', parentProject.ext.environmentName)
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

		// Check the system properties
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', parentProject.'systemProp.childEnvironmentProp')
		assertEquals('ParentEnvironmentTest.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', parentProject.'systemProp.parentEnvironmentProp')
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Verify that when we only specify a property in the project property file,
	 * it still gets set and is in the filters.  This also verifies that we
	 * ignore properties set in child project files.
	 */
	public void testApplyProjectProperties() {
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
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
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

		// Check the system properties
		assertEquals('ParentEnvironmentLocal.childProjectValue', parentProject.'systemProp.childProjectProp')
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentProject.parentProjectValue', parentProject.'systemProp.parentProjectProp')
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
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
						commandProperty: 'Command.commandValue',
						'systemProp.commandProp': 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
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

		assertEquals(32, tokens.size()) // 18 we're checking + 14 systemProp
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

		// Check the system properties. Remember, system properties are not
		// set from command line properties.
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
		assertEquals('local', parentProject.ext.environmentName)
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

		// 20 that we're checking and 14 from system properties
		assertEquals(34, tokens.size())
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

		// Check the system properties. Remember, system properties are not
		// set from command line properties.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
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
		assertEquals('local', parentProject.ext.environmentName)
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

		// 20 that we're checking and 14 from system properties
		assertEquals(34, tokens.size())
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

		// Check the system properties. Remember, system properties are not
		// set from command line properties.
		assertEquals('ParentProject.parentProjectValue', System.properties['parentProjectProp'])
		assertEquals('ParentProject.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentProject.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentProject.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
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
		createProjects()
		setFileProperties(false, false)
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
		assertEquals('local', parentProject.ext.environmentName)
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

		// 18 that we're checking and 12 from system properties, since we'll be
		// missing the properties that were only set in the parent project file.
		assertEquals(30, tokens.size())
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

		// Check the system properties. Remember, system properties are not
		// set from command line properties.
		assertNull(System.properties['parentProjectProp'])
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
		assertEquals('ParentEnvironmentLocal.childProjectValue', System.properties['childProjectProp'])
		assertEquals('ParentEnvironmentLocal.childEnvironmentValue', System.properties['childEnvironmentProp'])
		assertEquals('Home.homeValue', System.properties['homeProp'])
		assertEquals('User.userValue', System.properties['userProp'])
		assertEquals('User.commandValue', System.properties['commandProp'])
	}
}
