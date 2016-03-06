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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware
import org.slf4j.LoggerFactory

/**
 * This is the main class for the properties plugin. When the properties
 * plugin is applied to a project, it uses the values of the
 * {@code environmentName} and {@code gradleUserName} properties to reload all
 * the project properties in the following order:<br>
 * <ol>
 * <li>
 * gradle.properties in the parent project's directory, if the project
 * is a module of a multi-project build.
 * </li>
 * <li>
 * gradle-${environmentName}.properties in the parent project's directory,
 * if the project is a module of a multi-project build. If no
 * environment name is specified, the default is "local".
 * </li>
 * <li>
 * gradle.properties in the project directory
 * </li>
 * <li>
 * gradle-${environmentName}.properties in the project directory. If no
 * environment name is specified, the default is "local".
 * </li>
 * <li>
 * gradle.properties in the user's ${gradleUserHomeDir} directory.
 * </li>
 * <li>
 * If the ${gradleUserName} property is set, gradle-${gradleUserName}.properties
 * in the user's ${gradleUserHomeDir} directory.
 * </li>
 * <li>
 * Environment variables starting with {@code ORG_GRADLE_PROJECT_}.
 * </li>
 * <li>
 * System properties starting with {@code org.gradle.project.}.
 * </li>
 * <li>
 * properties defined on the command line with the -P option.
 * </li>
 * </ol>
 * <p>
 * When the plugin is applied to a {@code Settings} object, it uses the
 * {@code environmentName} and {@code gradleUserName} properties to reload all
 * the settings properties in the following order:<br>
 *  * <ol>
 * <li>
 * gradle.properties in the directory where settings.gradle is located.
 * </li>
 * <li>
 * gradle-${environmentName}.properties in the directory where settings.gradle
 * is located. If no environment name is specified, the default is "local".
 * </li>
 * <li>
 * gradle.properties in the user's ${gradleUserHomeDir} directory.
 * </li>
 * <li>
 * If the ${gradleUserName} property is set, gradle-${gradleUserName}.properties
 * in the user's ${gradleUserHomeDir} directory.
 * </li>
 * <li>
 * Environment variables starting with {@code ORG_GRADLE_PROJECT_}.
 * </li>
 * <li>
 * System properties starting with {@code org.gradle.project.}.
 * </li>
 * <li>
 * properties defined on the command line with the -P option.
 * </li>
 * </ol>
 * <p>
 * As mentioned, this plugin looks for the gradle-${environmentName}.properties
 * file in the project directory for projects, or the directory with
 * settings.gradle if the plugin is applied to a Settings object.  You can
 * change this behavior by specifying a value for the {@code environmentFileDir}
 * property.  It is strongly recommended to use a subdirectory of the project
 * or settings directory when using this option.  This property only effects
 * where the environment specific files are located, it does not change where
 * the plugin looks for the other files, such as the user files or default
 * gradle files.
 * <p>
 * {@code environmentFileDir}, {@code environmentName}, and
 * {@code gradleUserName} are the properties that this plugin use by default to
 * get the location of the environment files and the names of the environment
 * and gradle user to use.  If you don't like these names, or if there is a
 * clash with properties you already use in your build, you can configure the
 * properties the plugin will use.  You can tell the plugin what property to
 * use for the environment file directory by setting a value for the
 * {@code propertiesPluginEnvironmentFileDirProperty} property. You can tell
 * the plugin what property to use for the name of the environment by setting
 * the {@code propertiesPluginEnvironmentNameProperty}, and you can tell the
 * plugin what property to use to set the user name by setting a value for the
 * {@code propertiesPluginGradleUserNameProperty} property. Those properties
 * have to be set before this plugin is applied. That means you can put them in
 * the standard property file locations supported by Gradle itself, in
 * environment variables, system properties, -P options or in the build.gradle
 * file itself before applying this plugin.
 * <p>
 * The last thing to set a property wins.  All files are optional unless an
 * environment or user is specified, in which case the file belonging to the
 * specified environment or user must exist.  If an environment file directory
 * is specified, that directory must exist, be a directory, and be readable.
 * <p>
 * As properties are set, they are also placed in a filterTokens property.
 * the filterTokens property is a map of tokens that can be used when doing a
 * filtered file copy. There will be a token for each property defined by one
 * of the listed methods. The name of each token will be the name of the
 * property from the properties file, after converting the camel case property
 * name to dot notation. For example, if you have a myPropertyName property in
 * the file, the plugin will create a my.property.name filter token, whose
 * value is the property's value. The original camel case name will also be
 * added as token.
 * <p>
 * Finally, the properties plugin also adds some properties to every task in
 * your build:
 * <p>{@code requiredProperty} and {@code requiredProperties} can be used to
 * define what properties must be present for that task to work.  Required
 * properties will be checked after configuration is complete, but before any
 * tasks are executed.
 * <p>
 * {@code recommendedProperty} and {@code recommendedProperties} can be used
 * to define properties that the task can work without, but it (or the deployed
 * application) will use a default value for the property. The value of this
 * property is that we can prompt new developers to either provide a property,
 * or make sure default config files are set up correctly.
 * <p>
 * Special thanks to Hans Dockter at Gradleware for showing me how to attach
 * to the right place in the Gradle build lifecycle.
 *
 * @author Steven C. Saliman
 */
class PropertiesPlugin implements Plugin<PluginAware> {
	def logger = LoggerFactory.getLogger getClass()

	/**
	 * This method is called then the properties-plugin is applied.  It can
	 * be applied to both {@code Project} and {@code Settings} objects.
	 * @param the object to which this plugin should be applied.  Currently,
	 * this must be either a {@code Project} or {@code Settings} object.
	 */
	void apply(PluginAware pluginAware) {
		if (pluginAware instanceof Settings) {
			doApply pluginAware, this.&buildPropertyFileListFromSettings
		} else if (pluginAware instanceof Project) {
			doApply pluginAware, this.&buildPropertyFileListFromProject
			// Register a task listener that adds the property checking helper methods.
			registerTaskListener(pluginAware)
		} else {
			throw new IllegalArgumentException("${pluginAware.getClass()} is currently not supported as apply target, please report if you need it")
		}
	}

	/**
	 * Private method that does the actual work of applying the plugin.
	 * @param pluginAware the project or settings object to which we are applying
	 * the plugin.
	 * @param buildPropertyFileList a function that takes a PluginAware object and
	 * an environment name, and returns a list of property files to evaluate
	 * when the plugin is applied.
	 */
	private doApply(pluginAware, propertyFileListBuilder) {
		if ( !pluginAware.hasProperty('propertiesPluginEnvironmentFileDirProperty' ) ) {
			pluginAware.ext.propertiesPluginEnvironmentFileDirProperty = 'environmentFileDir'
		}
		if ( !pluginAware.hasProperty('propertiesPluginEnvironmentNameProperty' ) ) {
			pluginAware.ext.propertiesPluginEnvironmentNameProperty = 'environmentName'
		}
		if ( !pluginAware.hasProperty('propertiesPluginGradleUserNameProperty' ) ) {
			pluginAware.ext.propertiesPluginGradleUserNameProperty = 'gradleUserName'
		}

		// If the user hasn't set a property file directory, assume the project
		// directory.
		if ( !pluginAware.hasProperty(pluginAware.propertiesPluginEnvironmentFileDirProperty ) ) {
			pluginAware.ext."$pluginAware.propertiesPluginEnvironmentFileDirProperty" = '.'
		}
		def envFileDir = pluginAware."$pluginAware.propertiesPluginEnvironmentFileDirProperty"

		// If the user hasn't set an environment, assume "local"
		if ( !pluginAware.hasProperty(pluginAware.propertiesPluginEnvironmentNameProperty ) ) {
			pluginAware.ext."$pluginAware.propertiesPluginEnvironmentNameProperty" = 'local'
		}
		def envName = pluginAware."$pluginAware.propertiesPluginEnvironmentNameProperty"
		pluginAware.ext.filterTokens = [:]

		// process files from least significant to most significant. With gradle
		// properties, Last one in wins.
		def foundEnvFile = false
		def propertyFiles = propertyFileListBuilder(pluginAware, envFileDir, envName)
		propertyFiles.each { PropertyFile file ->
			def success = processPropertyFile(pluginAware, file)
			// Fail right away if we're missing a required file.
			if ( file.fileType == FileType.REQUIRED && !success ) {
				throw new FileNotFoundException("could not process required file ${file.filename} ")
			}

			// If we found an environment file, make note of it.
			if ( file.fileType == FileType.ENVIRONMENT && success ) {
				foundEnvFile = true;
			}
		}

		processEnvironmentProperties(pluginAware)
		processSystemProperties(pluginAware)
		processCommandProperties(pluginAware)
		// Make sure we got at least one environment file if we are not in the local environment.
		if ( envName != 'local' && !foundEnvFile ) {
			throw new FileNotFoundException("No environment files were found for the '$envName' environment")
		}
	}

	/**
	 * Build a list of property files to process for a project, in the order in
	 * which they need to be processed.
	 * @param project the project applying the plugin.
	 * @param envFileDir the directory to search for environment files.
	 * @param envName the name of the environment to load.
	 * @return a List of {@link PropertyFile}s
	 */
	private buildPropertyFileListFromProject(project, envFileDir, envName) {
		def p = project
		def files = []
		while ( p != null ) {
			// We'll need to process the files from the top down, so build the list
			// backwards.  Note that only the environment file can come from a special
			// location.  gradle.properties must come from the project itself.
			def fileDir = p.projectDir
			if ( envFileDir != '.') {
				fileDir = "${fileDir}/${envFileDir}"
				// Make sure the directory actually exists...
				File d = new File(fileDir)
				def exists = d.exists()
				def isDirectory = d.isDirectory()
				def isReadable = d.canRead()
				if ( !exists || !isDirectory || !isReadable ) {
					throw new FileNotFoundException("Environment File directory '$envFileDir' does not exist, or is not a readable directory")
				}
				logger.info("PropertiesPlugin:apply Using ${envFileDir} as the source of environment specific files.")
			}

			// Whether or not the project property files can contain system
			// properties is entirely dependent on whether the project whose
			// files we're currently processing is the root project.
			def processSystemProperties = (p == project.rootProject)
			files.add(0, new PropertyFile("${fileDir}/gradle-${envName}.properties", FileType.ENVIRONMENT, processSystemProperties))
			files.add(0, new PropertyFile("${p.projectDir}/gradle.properties", FileType.OPTIONAL, processSystemProperties))
			p = p.parent
		}
		return addCommonPropertyFileList(project, files)
	}

	/**
	 * Build a list of property files to process for a settings object, in the
	 * order in which they need to be processed.
	 * @param settings the settings applying the plugin.
	 * @param envFileDir the directory to search for environment files.
	 * @param envName the name of the environment to load.
	 * @return a List of {@link PropertyFile}s
	 */
	private buildPropertyFileListFromSettings(settings, envFileDir, envName) {
		def fileDir = settings.settingsDir
		if ( envFileDir != '.') {
			fileDir = "${fileDir}/${envFileDir}"
			// Make sure the directory actually exists...
			File d = new File(fileDir)
			def exists = d.exists()
			def isDirectory = d.isDirectory()
			def isReadable = d.canRead()
			if ( !exists || !isDirectory || !isReadable ) {
				throw new FileNotFoundException("Environment File directory '$envFileDir' does not exist, or is not a readable directory")
			}
			logger.info("PropertiesPlugin:apply Using ${envFileDir} as the source of environment specific files.")
		}
		def files = []
		files.add(new PropertyFile("${settings.settingsDir}/gradle.properties", FileType.OPTIONAL, true))
		files.add(new PropertyFile("${fileDir}/gradle-${envName}.properties", FileType.ENVIRONMENT, true))
		return addCommonPropertyFileList(settings, files)
	}

	/**
	 * Helper method that adds the files common to both projects and settings to
	 * the end of the property file list.
	 * @param pluginAware the project or settings object we're dealing with.
	 * @param files the files we have so far.
	 * @return a List of {@link PropertyFile}s.
	 */
	private addCommonPropertyFileList(pluginAware, files) {
		// Add the rest of the files to the end.  The user property file is
		// optional...
		def userHome = pluginAware.getGradle().getGradleUserHomeDir();
		files.add(new PropertyFile("${userHome}/gradle.properties", FileType.OPTIONAL, true))
		if ( pluginAware.hasProperty(pluginAware.propertiesPluginGradleUserNameProperty) ) {
			//... the gradleUserName file, if specified, is not.
			def userName = pluginAware."$pluginAware.propertiesPluginGradleUserNameProperty"
			files.add(new PropertyFile("${userHome}/gradle-${userName}.properties", FileType.REQUIRED, true))
		}
		return files
	}

	/**
	 * Process a file, loading properties from it, and adding tokens.
	 * @param pluginAware the enclosing pluginAware.
	 * @param file the file to process
	 * @return whether or not we found the file requested.
	 */
	private boolean processPropertyFile(pluginAware, PropertyFile file) {
		def loaded = 0
		def systemProperties = 0
		def propFile = new File(file.filename)
		if ( !propFile.exists() ) {
			logger.info("PropertiesPlugin:apply Skipping ${file.filename} because it does not exist")
			return false
		}
		new File(file.filename).withReader {reader ->
			def userProps= new Properties()
			userProps.load(reader)
			userProps.each { String key, String value ->
				pluginAware.ext."$key" = value
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				pluginAware.ext.filterTokens[key] = value;
				def dotKey = camelCaseToDotNotation(key)
				pluginAware.ext.filterTokens[dotKey] = value
				loaded++
				if ( file.containsSystemProperties && key.startsWith("systemProp.") ) {
					def propName = key.substring(11)
					System.setProperty(propName, value)
					systemProperties++
				}
			}
		}
		logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from ${file.filename}")
		if ( systemProperties > 0 ) {
			logger.info("PropertiesPlugin:apply Set ${systemProperties} system properties from ${file.filename}")
		}
		return true
	}

	/**
	 * Process the environment properties, setting pluginAware properties and
	 * adding tokens for any environment variable starting with
	 * {@code ORG_GRADLE_PROJECT_}, per the Gradle specification.
	 * @param pluginAware the enclosing pluginAware.
	 */
	private processEnvironmentProperties(pluginAware) {
		def loaded = 0
		System.getenv().each { key, value ->
			if ( key.startsWith("ORG_GRADLE_PROJECT_") ) {
				pluginAware.ext."${key.substring(19)}" = value
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				pluginAware.ext.filterTokens[key.substring(19)] = value;
				def dotKey = camelCaseToDotNotation(key.substring(19))
				pluginAware.ext.filterTokens[dotKey] = value
				loaded++
			}
		}
		logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from environment variables")
	}

	/**
	 * Process the system properties, setting properties and adding tokens for
	 * any system property starting with {@code org.gradle.project.}, per the
	 * Gradle specification.
	 * @param pluginAware the enclosing pluginAware.
	 */
	private processSystemProperties(pluginAware) {
		def loaded = 0
		System.properties.each { key, value ->
			if ( key.startsWith("org.gradle.project.") ) {
				pluginAware.ext."${key.substring(19)}" = value
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				pluginAware.ext.filterTokens[key.substring(19)] = value;
				def dotKey = camelCaseToDotNotation(key.substring(19))
				pluginAware.ext.filterTokens[dotKey] = value
				loaded++
			}
		}
		logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from system properties")
	}

	/**
	 * Process the command line properties, setting properties and adding tokens.
	 * @param pluginAware the enclosing pluginAware.
	 */
	private processCommandProperties(pluginAware) {
		def loaded = 0
		def commandProperties = pluginAware.gradle.startParameter.projectProperties
		commandProperties.each { key, value ->
			pluginAware.ext."$key" = value
			// add the property to the filter tokens, both in camel case and dot
			// notation.
			pluginAware.ext.filterTokens[key] = value;
			def dotKey = camelCaseToDotNotation(key)
			pluginAware.ext.filterTokens[dotKey] = value
			loaded++
		}
		logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from the command line")
	}

	/**
	 * Register a task listener to add the property checking methods to all
	 * current tasks as well as tasks that are added to the project after the
	 * plugin is applied.
	 *
	 * @param project the project applying the plugin
	 */
	private registerTaskListener(project) {
		// "all" executes the closure against all tasks in the project, and any
		// new tasks added in the future.  This closure defines a requireProperty
		// method for every task.
		project.tasks.all { task ->
			// This is the requireProperty method, which executes as soon as it is
			// called in the build file...
			task.ext.requiredProperty = { String propertyName ->
				// ... but we don't want to execute at configuration time.  We want to
				// record that the property is needed for the task, and check it
				// when the graph is ready, between configuration and execution.
				// the whenReady method allows us to pass in a closure to be executed...
				project.gradle.taskGraph.whenReady { graph ->
					// ... But we only want to actually do it if the task needing the
					// property is actually going to be executed.
					if (graph.hasTask(task.path)) {
						checkProperty(project, propertyName, task, "requiredProperty")
					}
				}
			}

			// now add the one that takes a list...
			task.ext.requiredProperties = { String[] propertyNames ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						for ( propertyName in propertyNames ) {
							checkProperty(project, propertyName, task, "requiredProperties")
						}
					}
				}
			}

			// add the recommendedProperty property
			task.ext.recommendedProperty = { String propertyName, String defaultFile=null ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						checkRecommendedProperty(project, propertyName, task, "recommendedProperty", defaultFile)
					}
				}
			}

			// now add the one that takes a list...
			task.ext.recommendedProperties = { hash ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						def propertyNames = hash['names']
						def defaultFile = hash['defaultFile']
						for ( propertyName in propertyNames ) {
							checkRecommendedProperty(project, propertyName, task, "recommendedProperties", defaultFile)
						}
					}
				}
			}
		}
	}

	/**
	 * Helper method to make sure a given property exists
	 * @param project the project we're dealing with
	 * @param propertyName the name of the property we want to check
	 * @param task the task checking the property.
	 * @param caller the name of the method calling this one.  Used to log
	 *        who is doing the work.
	 * @throws MissingPropertyException if the named property is not in the
	 * project.
	 */
	private checkProperty(project, propertyName, task, caller) {
		def taskName = task.path
		if ( !project.hasProperty(propertyName) ) {
			throw new MissingPropertyException("You must set the '${propertyName}' property for the '$taskName' task")
		}
		// Now register the property as an input for the task.
		def propertyValue = project.property(propertyName)
		logger.debug("PropertiesPlugin:${caller} Setting $propertyName as an input to ${taskName} with a value of '$propertyValue'")
		task.inputs.property(propertyName, propertyValue)
	}

	/**
	 * Helper method to check a recommended property and print a warning if it is
	 * missing.
	 * @param project the project we're dealing with
	 * @param propertyName the name of the property we want to check
	 * @param task the task checking the property.
	 * @param caller the name of the method calling this one.  Used to log
	 *        who is doing the work.
	 * @param defaultFile an optional description of where the project will get
	 *        the value if it isn't specified during the build.
	 */
	private checkRecommendedProperty(project, propertyName, task, caller, defaultFile) {
		def taskName = task.path
		if ( !project.hasProperty(propertyName) ) {
			def message = "WARNING: '${propertyName}', required by '$taskName' task, has no value, using default"
			if ( defaultFile != null ) {
				message = message + " from '${defaultFile}'"
			}
			println message
		}
		def propertyValue = project.property(propertyName)
		logger.debug("PropertiesPlugin:${caller} Setting $propertyName as an input to ${taskName} with a value of '$propertyValue'")
		task.inputs.property(propertyName, propertyValue)
	}


	/**
	 * helper method to convert a camel case property name to a dot notated
	 * one.  This is used by the plugin for filtering.  For example,
	 * myPropertyName would become my.property.name as a token.  Property names
	 * that don't start with a lower case letter are assumed to not be camel case
	 * and are returned as is.  This means that callers will add the same property
	 * twice, which should be fine.
	 * @param propertyName the name of the property to convert
	 * @return the converted property name
	 */
	private camelCaseToDotNotation(String propertyName) {
		if ( !propertyName.charAt(0).isLowerCase() ) {
			return propertyName
		}

		StringBuilder sb = new StringBuilder();
		for ( char c : propertyName.getChars() ) {
			if ( c.upperCase ) {
				sb.append(".")
				sb.append(c.toLowerCase())
			} else {
				sb.append(c)
			}
		}
		return sb.toString()
	}

}
