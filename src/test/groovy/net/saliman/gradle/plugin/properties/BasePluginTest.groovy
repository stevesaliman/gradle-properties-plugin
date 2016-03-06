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
 * <p>
 * Also note that we deal with system properties in 2 different ways; reading
 * and writing.  Properties ending with "Property" are properties we read.
 * "read" properties are used to see if the have the right order of precedence
 * when we set the values of project properties, some of which can come from
 * system properties.  "write" properties are used to see if we set the right
 * value of system properties when we read a build property with the right
 * prefix.
 *
 * @author Steven C. Saliman
 */
class BasePluginTest extends GroovyTestCase {
	def plugin = new PropertiesPlugin()
	def parentProject = null;
	def childProject = null;
	def parentTask = null;
	def childTask = null;
	def parentCommandProperties = null;
	def childCommandProperties = null;

	/**
	 * helper method to create the projects and tasks used in the unit tests.
	 * This will create a parent project with one child project. It also sets
	 * up a spot for command properties so we can change them later.
	 */
	def createProjects() {
		// Create the parent project.
		def parentProjectDir = new File('build/test/parentProject')
		parentProject = ProjectBuilder
						.builder()
						.withName('parentProject')
						.withProjectDir(parentProjectDir)
						.build();

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

		childCommandProperties = parentProject.gradle.startParameter.projectProperties

		// Add a task we can check for properties.
		childTask = childProject.task('myTest')

		// Add the child to the parent.
		parentProject.childProjects.put('child', childProject)
	}

	/**
	 * Helper method to set the properties that would be set before the plugin
	 * is applied.  These properties would be set by Gradle itself as it creates
	 * projects from build.gradle files.  Gradle only handles properties in the
	 * command line and the 3 gradle.properties file, and it only sets system
	 * properties from the home and parent project files.
	 * @param includeParentFile whether or not to assume the parent
	 *        gradle.properties file was present.
	 * @param includeChildFile whether or not to assume the child
	 *        gradle.properties file was present.
	 */
	def setFileProperties(includeParentFile, includeChildFile) {
		if ( includeParentFile ) {
			parentProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
			parentProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
			parentProject.ext.childProjectProperty = 'ParentProject.childProjectValue'
			parentProject.ext.childEnvironmentProperty = 'ParentProject.childEnvironmentValue'

			parentProject.ext.'systemProp.parentProjectProp' =  'ParentProject.parentProjectValue'
			parentProject.ext.'systemProp.parentEnvironmentProp' =  'ParentProject.parentEnvironmentValue'
			parentProject.ext.'systemProp.childProjectProp' =  'ParentProject.childProjectValue'
			parentProject.ext.'systemProp.childEnvironmentProp' =  'ParentProject.childEnvironmentValue'
		}

		parentProject.ext.homeProperty = 'Home.homeValue'
		parentProject.ext.userProperty = 'Home.userValue'
		parentProject.ext.environmentProperty = 'Home.environmentValue'
		parentProject.ext.systemProperty = 'Home.systemValue'
		parentProject.ext.commandProperty = 'Home.commandValue'

		parentProject.ext.'systemProp.homeProp' =  'Home.homeValue'
		parentProject.ext.'systemProp.userProp' =  'Home.userValue'
		parentProject.ext.'systemProp.environmentPropProp' =  'Home.environmentValue'
		parentProject.ext.'systemProp.commandProp' =  'Home.commandValue'

		if ( includeParentFile ) {
			childProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
			childProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
			childProject.ext.childProjectProperty = 'ParentProject.childProjectValue'
			childProject.ext.childEnvironmentProperty = 'ParentProject.childEnvironmentValue'

			childProject.ext.'systemProp.parentProjectProp' = 'ParentProject.parentProjectValue'
			childProject.ext.'systemProp.parentEnvironmentProp' = 'ParentProject.parentEnvironmentValue'
			childProject.ext.'systemProp.childProjectProp' = 'ParentProject.childProjectValue'
			childProject.ext.'systemProp.childEnvironmentProp' = 'ParentProject.childEnvironmentValue'
		}
		// child file overrides parent file.
		if ( includeChildFile ) {
			childProject.ext.childProjectProperty = 'ChildProject.childProjectValue'
			childProject.ext.childEnvironmentProperty = 'ChildProject.childEnvironmentValue'

			childProject.ext.'systemProp.childProjectProp' =  'ChildProject.childProjectValue'
			childProject.ext.'systemProp.childEnvironmentProp' =  'ChildProject.childEnvironmentValue'

		}
		childProject.ext.homeProperty = 'Home.homeValue'
		childProject.ext.userProperty = 'Home.userValue'
		childProject.ext.environmentProperty = 'Home.environmentValue'
		childProject.ext.systemProperty = 'Home.systemValue'
		childProject.ext.commandProperty = 'Home.commandValue'

		childProject.ext.'systemProp.homeProp' =  'Home.homeValue'
		childProject.ext.'systemProp.userProp' =  'Home.userValue'

		// Set the Java system properties.  Gradle would have only processed
		// the parent project file and the home file.
		if ( includeParentFile ) {
			System.setProperty('parentProjectProp', 'ParentProject.parentProjectValue')
			System.setProperty('parentEnvironmentProp', 'ParentProject.parentEnvironmentValue')
			System.setProperty('childProjectProp', 'ParentProject.childProjectValue')
			System.setProperty('childEnvironmentProp', 'ParentProject.childEnvironmentValue')
		} else {
			System.clearProperty('parentProjectProp')
			System.clearProperty('parentEnvironmentProp')
			System.clearProperty('childProjectProp')
			System.clearProperty('childEnvironmentProp')
		}
		System.setProperty('homeProp', 'Home.homeValue')
		System.setProperty('userProp', 'Home.userValue')
		System.setProperty('environmentProp', 'Home.environmentValue')
		System.setProperty('commandProp', 'Home.commandValue')
	}

	/**
	 * Helper method to set the properties that Gradle would have set for us
	 * via the command line, system properties, and environment variables.
	 * Per the gradle docs, it does not set any system properties from the
	 * environment or the command line.
	 * @param setEnvironmentProperties whether or not to set the environment
	 *        properties
	 * @param setSystemProperties whether or not to set the system properties
	 * @param commandProperties a map of properties that should be added to the
	 *        "command line" for our build.
	 */
	def setNonFileProperties(setEnvironmentProperties, setSystemProperties,
	                         Map commandProperties) {
		// Set or clear environment variables.  We don't worry about java
		// properties here because you can't have a dot in an environment
		// variable, and gradle doesn't set system properties from the
		// environment.
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
		// Set system properties.  We don't set the "prop" properties, because
		// we don't read them in the plugin, we write them..
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
	 * Do the actual work of copying the test property files to the correct
	 * locations within the project hierarchy
	 */
	def copyFiles() {
		// Copy the files for the parent project.
		def parentUserDir = parentProject.gradle.gradleUserHomeDir
		def builder = new AntBuilder()
		builder.copy(file: 'src/test/resources/parent-project-gradle.properties',
				tofile: "${parentProject.projectDir}/gradle.properties")
		builder.copy(file: 'src/test/resources/parent-env-test.properties',
				tofile: "${parentProject.projectDir}/gradle-test.properties")
		builder.copy(file: 'src/test/resources/parent-env-local.properties',
				tofile: "${parentProject.projectDir}/gradle-local.properties")
		builder.copy(file: 'src/test/resources/home-gradle.properties',
				tofile: "${parentUserDir}/gradle.properties")
		builder.copy(file: 'src/test/resources/user-gradle.properties',
				tofile: "${parentUserDir}/gradle-user.properties")
		builder.copy(file: 'src/test/resources/parent-env-local-sub.properties',
				tofile: "${parentProject.projectDir}/gradle-properties/gradle-local.properties")

		// copy the files for the child project.
		def childUserDir = childProject.gradle.gradleUserHomeDir
		builder.copy(file: 'src/test/resources/child-project-gradle.properties',
				tofile: "${childProject.projectDir}/gradle.properties")
		builder.copy(file: 'src/test/resources/child-env-test.properties',
				tofile: "${childProject.projectDir}/gradle-test.properties")
		builder.copy(file: 'src/test/resources/child-env-local.properties',
				tofile: "${childProject.projectDir}/gradle-local.properties")
		builder.copy(file: 'src/test/resources/home-gradle.properties',
				tofile: "${childUserDir}/gradle.properties")
		builder.copy(file: 'src/test/resources/user-gradle.properties',
				tofile: "${childUserDir}/gradle-user.properties")
		builder.copy(file: 'src/test/resources/child-env-local-sub.properties',
				tofile: "${childProject.projectDir}/gradle-properties/gradle-local.properties")
	}

	/**
	 * If we don't have at least one test here, we'll get warnings, so define
	 * a test that always passes.
	 */
	public void testNothing() {

	}
}
