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
 * {@code environmentName} and {@code gradleUserName} are the properties that
 * this plugin uses by default to get the names of the environment and gradle
 * user to use.  If you don't like these names, or if there is a clash with
 * properties you already use in your build, you can configure the properties
 * the plugin will use.  You can tell the plugin what property to use for the
 * environment name by setting a value for the
 * {@code propertiesPluginEnvironmentNameProperty} property, and you can tell
 * the plugin what property to use for the user name by setting a value for the
 * {@code propertiesPluginGradleUserNameProperty} property. Those properties
 * have to be set before this plugin is applied. That means you can put them in
 * the standard property file locations supported by Gradle itself, in
 * environment variables, system properties, -P options or in the build.gradle
 * file itself before applying this plugin.
 * <p>
 * The last thing to set a property wins.  All files are optional unless an
 * environment or user is specified, in which case the file belonging to the
 * specified environment or user must exist.
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
	private doApply(pluginAware, buildPropertyFileList) {
		if ( !pluginAware.hasProperty('propertiesPluginEnvironmentNameProperty' ) ) {
			pluginAware.ext.propertiesPluginEnvironmentNameProperty = 'environmentName'
		}
		if ( !pluginAware.hasProperty('propertiesPluginGradleUserNameProperty' ) ) {
			pluginAware.ext.propertiesPluginGradleUserNameProperty = 'gradleUserName'
		}

		// If the user hasn't set an environment, assume "local"
		if ( !pluginAware.hasProperty(pluginAware.propertiesPluginEnvironmentNameProperty ) ) {
			pluginAware.ext."$pluginAware.propertiesPluginEnvironmentNameProperty" = 'local'
		}
		def envName = pluginAware."$pluginAware.propertiesPluginEnvironmentNameProperty"
		
		if ( !pluginAware.hasProperty('filterTokens' ) ) {
			pluginAware.ext.filterTokens = [:]
		} else {
			pluginAware.ext.filterTokens = pluginAware.filterTokens
		}

		// process files from least significant to most significant. With gradle
		// properties, Last one in wins.
		def foundEnvFile = false
		def propertyFiles = buildPropertyFileList(pluginAware, envName)
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
	 * @return a List of {@link PropertyFile}s
	 */
	private buildPropertyFileListFromProject(project, envName) {
		def p = project
		def files = []
		while ( p != null ) {
			// We'll need to process the files from the top down, so build the list
			// backwards.
			files.add(0, new PropertyFile("${p.projectDir}/gradle-${envName}.properties", FileType.ENVIRONMENT))
			files.add(0, new PropertyFile("${p.projectDir}/gradle.properties", FileType.OPTIONAL))
			p = p.parent
		}
		return addCommonPropertyFileList(project, files)
	}

	/**
	 * Build a list of property files to process for a settings object, in the
	 * order in which they need to be processed.
	 * @param settings the settings applying the plugin.
	 * @return a List of {@link PropertyFile}s
	 */
	private buildPropertyFileListFromSettings(settings, envName) {
		def files = []
		files.add(new PropertyFile("${settings.settingsDir}/gradle.properties", FileType.OPTIONAL))
		files.add(new PropertyFile("${settings.settingsDir}/gradle-${envName}.properties", FileType.ENVIRONMENT))
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
		files.add(new PropertyFile("${userHome}/gradle.properties", FileType.OPTIONAL))
		if ( pluginAware.hasProperty(pluginAware.propertiesPluginGradleUserNameProperty) ) {
			//... the gradleUserName file, if specified, is not.
			def userName = pluginAware."$pluginAware.propertiesPluginGradleUserNameProperty"
			files.add(new PropertyFile("${userHome}/gradle-${userName}.properties", FileType.REQUIRED))
		}
		return files
	}

	/**
	 * Process a file, loading properties from it, and adding tokens.
	 * @param filename the name of the file to process
	 * @param pluginAware the enclosing pluginAware.
	 * @param required whether or not processing this file is required.  Required
	 *        files that are missing will cause an error.
	 * @return whether or not we found the file requested.
	 */
	private boolean processPropertyFile(pluginAware, PropertyFile file) {
		def loaded = 0
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
				def token = propertyToToken(key)
				pluginAware.ext.filterTokens[token] = value
				loaded++
			}
		}
		logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from ${file.filename}")
		return true
	}

	/**
	 * Process the environment properties, setting pluginAware properties and adding
	 * tokens for any environment variable starting with
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
				def token = propertyToToken(key.substring(19))
				pluginAware.ext.filterTokens[token] = value
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
				def token = propertyToToken(key.substring(19))
				pluginAware.ext.filterTokens[token] = value
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
			def token = propertyToToken(key)
			pluginAware.ext.filterTokens[token] = value
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
						checkProperty(project, propertyName, task.path)
					}
				}
			}

			// now add the one that takes a list...
			task.ext.requiredProperties = { String[] propertyNames ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						for ( propertyName in propertyNames ) {
							checkProperty(project, propertyName, task.path)
						}
					}
				}
			}

			// add the recommendedProperty property
			task.ext.recommendedProperty = { String propertyName, String defaultFile=null ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						checkRecommendedProperty(project, propertyName, task.path, defaultFile)
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
							checkRecommendedProperty(project, propertyName, task.path, defaultFile)
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
	 * @throws MissingPropertyException if the named property is not in the
	 * project.
	 */
	private checkProperty(project, propertyName, taskName) {
		if ( !project.hasProperty(propertyName) ) {
			throw new MissingPropertyException("You must set the '${propertyName}' property for the '$taskName' task")
		}
	}

	/**
	 * Helper method to check a recommended property and print a warning if it is
	 * missing.
	 * @param project the project we're dealing with
	 * @param propertyName the name of the property we want to check
	 * @param defaultFile an optional description of where the project will get
	 *        the value if it isn't specified during the build.
	 */
	private checkRecommendedProperty(project, propertyName, taskName, defaultFile) {
		if ( !project.hasProperty(propertyName) ) {
			def message = "WARNING: '${propertyName}', required by '$taskName' task, has no value, using default"
			if ( defaultFile != null ) {
				message = message + " from '${defaultFile}'"
			}
			println message
		}
	}

	/**
	 * helper method to convert a camel case property name to a dot notated
	 * token for filtering.  For example, myPropertyName would become
	 * my.property.name as a token.
	 * @param propertyName the name of the property to convert
	 * @return the converted property name
	 */
	private propertyToToken(String propertyName) {
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
