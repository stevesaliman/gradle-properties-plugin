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
	def parentCommandProperties = null;
	def childCommandProperties = null;
	def parentTask = null;
	def childTask = null;

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
		parentProject.ext.userProperty = 'Home.userValue'
		parentProject.ext.homeProperty = 'Home.homeValue'
		parentProject.ext.childEnvironmentProperty = 'ParentProject.childEnvironmentValue'
		parentProject.ext.childProjectProperty = 'ParentProject.childProjectValue'
		parentProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		if ( includeProjectProperties ) {
			parentProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
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
		childProject.ext.userProperty = 'Home.userValue'
		childProject.ext.homeProperty = 'Home.homeValue'
		childProject.ext.childEnvironmentProperty = 'ChildProject.childEnvironmentValue'
		childProject.ext.childProjectProperty = 'ChildProject.childProjectValue'
		childProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		if ( includeProjectProperties ) {
			childProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
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
		builder.copy(file:'src/test/resources/parent-env-local-sub.properties',
						tofile : "${parentProject.projectDir}/gradle-properties/gradle-local.properties")

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
	public void testCheckPropertyMissing() {
		childProject.ext.someProperty = 'someValue'
		// we succeed if we don't get an exception.
		plugin.checkProperty(childProject, 'someProperty', childTask, 'someMethod')
		def inputValue = childTask.inputs.properties['someProperty']
		assertEquals('Failed to register the task input', 'someValue', inputValue)

	}

	/**
	 * Test the checkProperty method when the property is missing.
	 */
	public void testCheckPropertyPresent() {
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
	 * value
	 */
	public void testApplyPluginWithAll() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
	}

	/**
	 * Apply the plugin with everything except for system properties.  In this
	 * case, the property normally set from system properties will inherit the
	 * value from the environment variables.
	 */
	public void testApplyPluginNoSystemProperties() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, false, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
	}

	/**
	 * Apply the plugin with everything except for environment variables.  In this
	 * case, the property that is normally set by environment variables will
	 * inherit the value from the User file.
	 */
	public void testApplyPluginNoEnvironmentVariables() {
		// simulate a "-PcommandProperty=Command.commandValue
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(false, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
	}

	/**
	 * Test applying the plugin when no user is given. In this case, the property
	 * that usually comes from the user file will inherit the value from the
	 * home file.
	 */
	public void testApplyNoUser() {
		// simulate a "-PcommandProperty=Command.commandValue -PenvironmentName=test"
		// command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(20, tokens.size())
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
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('local', childProject.environmentName)
		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)
		assertEquals(20, tokens.size())
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
	}

	/**
	 * Apply the plugin when no environment is specified, but we do specify a
	 * directory.  This should apply environment properties from the Local file
	 * instead of the Test file, and it should use the one in the property
	 * directory.  This will also verify that the child environment files
	 * override the parent environment files.
	 */
	public void testApplyUseDefaultEnvironmentFileInDirectory() {
		// simulate a "-PcommandProperty=Command.commandValue -PgradleUserName=user
		// -PenvironmentFileDir=gradle-properties" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
		assertEquals('user', childProject.gradleUserName)

		assertEquals('local', childProject.environmentName)
		assertEquals('ParentProject.parentProjectValue', childProject.parentProjectProperty)
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', childProject.parentEnvironmentProperty)
		assertEquals('ChildProject.childProjectValue', childProject.childProjectProperty)
		assertEquals('ChildEnvironmentSubLocal.childEnvironmentValue', childProject.childEnvironmentProperty)
		assertEquals('Home.homeValue', childProject.homeProperty)
		assertEquals('User.userValue', childProject.userProperty)
		assertEquals('Environment.environmentValue', childProject.environmentProperty)
		assertEquals('System.systemValue', childProject.systemProperty)
		assertEquals('Command.commandValue', childProject.commandProperty)
		assertEquals(22, tokens.size())
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

		// simulate a "-PcommandProperty=Command.commandValue" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
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
		assertEquals(18, tokens.size())
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
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
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
		assertEquals(20, tokens.size())
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
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
		// -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
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
		assertEquals(20, tokens.size())
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
		createProjects(false)
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
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
		assertEquals(18, tokens.size())
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
		// -PenvironmentName=test -PgradleUserName=user" command line
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						environmentName: 'test',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('test' , childProject.environmentName)
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
		assertEquals(22, tokens.size())
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
		createProjects(false)
		def commandArgs = [
						commandProperty: 'Command.commandValue',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		childProject.apply plugin: 'properties'
		def tokens = childProject.filterTokens;
		assertEquals('local' , childProject.environmentName)
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
		assertEquals(18, tokens.size())
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
	}


	// child standard property overrides parent property

	// parent used envName, child uses something else. - this will be ok when we're building the child from the child.
}
