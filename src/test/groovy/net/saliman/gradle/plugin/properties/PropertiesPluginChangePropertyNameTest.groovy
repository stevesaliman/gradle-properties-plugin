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
 * The {@link PropertiesPluginParentProjectTest} class tests applying the
 * plugin with the standard {@code environmentName} and {@code gradleUserName}
 * properties driving the environment name and gradle user.
 * <p>
 * This class tests changing these properties via
 * {@code propertiesPluginEnvironmentNameProperty} and
 * {@code propertiesPluginGradleUserNameProperty} properties before applying
 * the plugin to make sure we still do the right thing when the user wants to
 * reconfigure what the plugin uses during apply time.
 *
 * @author Steven C. Saliman
 */
class PropertiesPluginChangePropertyNameTest extends BasePluginTest {
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
	 * Typically, a plugin is applied by name, but Gradle supports applying by
	 * type.  Prove that it works.  We don't need to get fancy here, the default
	 * environment works fine.  We only need to check that one property was read
	 * from the local file.
	 */
	public void testApplyPluginByType() {
		parentProject.apply plugin: net.saliman.gradle.plugin.properties.PropertiesPlugin
		assertEquals('local', parentProject.environmentName)
		assertEquals('local', parentProject.ext.environmentName)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, but we don't specify a value for that variable.
	 * The plugin should assume the project directory.  We'll try to confuse the
	 * plugin by setting the standard environmentFileDir property to a different
	 * value to make sure the plugin is using the right property.  We don't need
	 * to check every property from the file - we already know that works.
	 */
	public void testApplyChangedPropFileDirPropertyNoValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myDir
		// -PenvironmentFileDir=gradle-properties" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						environmentFileDir: 'gradle-properties' // this is a good dir.
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		//the old environmentFileDir property should still be set ...
		assertEquals('gradle-properties', parentProject.environmentFileDir)
		// ... but the property values should come from the local file in the
		// project directory.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])

		// Check System properties.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * doesn't exist.  The plugin should fail, even if the "environmentFileDir"
	 * is still set to a valid directory.
	 */
	public void testApplyChangedPropFileDirPropertyBadValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myEnvironment
		// -PenvironmentFileDir=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'dummy',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're using an invalid property file directory.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * points to a file.  The plugin should fail, even if the
	 * "environmentFileDir" is still set to a valid directory.
	 */
	public void testApplyChangedPropFileDirPropertyFileValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myEnvironment
		// -PenvironmentFileDir=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'gradle-local.properties',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're using an invalid property file directory.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * points to a valid directory.  The plugin should succeed, even if
	 * "environmentFileDir" is invalid.
	 */
	public void testApplyChangedEnvironmentFileDirPropertyGoodValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myDir
		// -PmyDir=test -PenvironmentFileDir=dummy" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'gradle-properties',
						environmentFileDir: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the subdirectory to load properties,...
		assertEquals('gradle-properties', parentProject.myDir)
		// ... the environmentFileDir should still be set ...
		assertEquals('dummy', parentProject.environmentFileDir)
		// ... but the property values should come from the file in the subdirectory.
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parent.environment.property'])

		// Check System properties.
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * environment name, but we don't specify a value for that variable.  The
	 * plugin should assume "local".  We'll try to confuse the plugin by setting
	 * the standard environment property to a different value to make sure the
	 * plugin is using the right property.  We don't need to check every property
	 * from the file - we already know that works.
	 */
	public void testApplyChangedEnvNamePropertyNoValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						environmentName: 'test'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		// the plugin should be using the local environment,...
		assertEquals('local', parentProject.myEnvironment)
		// ... the environmentName should still be set ...
		assertEquals('test', parentProject.environmentName)
		assertEquals('test', parentProject.ext.environmentName)
		// ... but the property values should come from the local file.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])

		// Check System properties.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * environment name, and we specify a value for that variable that doesn't
	 * have a file.  The plugin should fail, even if the "environmentName" is
	 * still set to a valid file.
	 */
	public void testApplyChangedEnvNamePropertyBadValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PmyEnvironment=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						myEnvironment: 'dummy',
						environmentName: 'test'
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
	 * Try applying the plugin when we specify a different property for the
	 * environment name, and we specify a value for that variable that does have
	 * have a file.  The plugin should succeed, even if "envornmentName" is
	 * invalid.
	 */
	public void testApplyChangedEnvNamePropertyGoodValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PmyEnvironment=test -PenvironmentName=dummy" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						myEnvironment: 'test',
						environmentName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the test environment,...
		assertEquals('test', parentProject.myEnvironment)
		// ... the environmentName should still be set ...
		assertEquals('dummy', parentProject.environmentName)
		assertEquals('dummy', parentProject.ext.environmentName)
		// ... but the property values should come from the local file.
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])

		// Check System properties.
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', System.properties['parentEnvironmentProp'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, but we don't specify a value for that variable.  The
	 * plugin should assume no value, and we should get the "user" property from
	 * the "home" file.  We'll try to confuse the plugin by setting the standard
	 * user property to a different value to make sure the plugin is using the
	 * right property.  We don't need to check every property from the file - we
	 * already know that works.
	 */
	public void testApplyChangedUserNamePropertyNoValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUSer
		// -PgradleUserName=dummy" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		// the plugin should be ignoring users,...
		assertFalse(parentProject.hasProperty('myUser'))
		// ... the gradleUserName should still be set ...
		assertEquals('dummy', parentProject.gradleUserName)
		// ... but the plugin shouldn't be using it.
		assertEquals('Home.userValue', parentProject.userProperty)

		// camel case notation
		assertEquals('Home.userValue', tokens['userProperty'])
		// dot notation
		assertEquals('Home.userValue', tokens['user.property'])

		// Check System properties.
		assertEquals('Home.userValue', System.properties['userProp'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, and we specify a value for that variable that doesn't have a
	 * file.  The plugin should fail, even if the "gradleUserName" is still set
	 * to a valid file.
	 */
	public void testApplyChangedUserNamePropertyBadValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUser
		// -PmyUser=dummy -PgradleUserName=user" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						myUser: 'dummy',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing user file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, and we specify a value for that variable that does have
	 * have a file.  The plugin should succeed, even if "gradleUserNAme" is
	 * invalid.
	 */
	public void testApplyChangedUserNamePropertyGoodValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUser
		// -PmyUser=user -PgradleUserName=dummy" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						myUser: 'user',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the 'user' user,...
		assertEquals('user', parentProject.myUser)
		// ... the gradleUserName should still be set ...
		assertEquals('dummy', parentProject.gradleUserName)
		// ... but the property values should come from the User file.
		assertEquals('User.userValue', parentProject.userProperty)

		// camel case notation
		assertEquals('User.userValue', tokens['userProperty'])
		// dot notation
		assertEquals('User.userValue', tokens['user.property'])

		// Check System properties.
		assertEquals('User.userValue', System.properties['userProp'])
	}
}
