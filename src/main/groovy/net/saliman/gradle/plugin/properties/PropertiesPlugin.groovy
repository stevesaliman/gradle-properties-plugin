package net.saliman.gradle.plugin.properties

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This is the main class for the properties plugin. When the properties
 * plugin is applied, it reloads all the project properties in the following
 * order:<br>
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
 * The property names for {@code environmentName} and {@code gradleUserName}
 * can be configured if you don't like their name or there is a clash with
 * properties you already use in your build. The property name for
 * {@code environmentName} can be configured with the property
 * {@code propertiesPluginEnvironmentNameProperty} and the property name for
 * {@code gradleUserName} can be configured with the property
 * {@code propertiesPluginGradleUserNameProperty}. Those properties have
 * to be set before this plugin is applied. That means you can put them in the
 * standard property file locations supported by Gradle itself, in environment
 * variables, system properties, -P options or in the build.gradle file itself
 * before applying this plugin.
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
class PropertiesPlugin implements Plugin<Project> {
	/**
	 * This method is called then the properties-plugin is applied.
	 * @param project Gradle will pass in the project instance we're dealing
	 * with.
	 */
	void apply(Project project) {
		if ( !project.hasProperty('propertiesPluginEnvironmentNameProperty' ) ) {
			project.ext.propertiesPluginEnvironmentNameProperty = 'environmentName'
		}
		if ( !project.hasProperty('propertiesPluginGradleUserNameProperty' ) ) {
			project.ext.propertiesPluginGradleUserNameProperty = 'gradleUserName'
		}

		// If the user hasn't set an environment, assume "local"
		if ( !project.hasProperty(project.propertiesPluginEnvironmentNameProperty ) ) {
			project.ext."$project.propertiesPluginEnvironmentNameProperty" = 'local'
		}
		def envName = project."$project.propertiesPluginEnvironmentNameProperty"
		project.ext.filterTokens = [:]

		// process files from least significant to most significant. With gradle
		// properties, Last one in wins.
		def foundEnvFile = false
		def propertyFiles = buildPropertyFileList(project, envName)
		propertyFiles.each { PropertyFile file ->
			def success = processPropertyFile(project, file)
			// Fail right away if we're missing a required file.
			if ( file.fileType == FileType.REQUIRED && !success ) {
				throw new FileNotFoundException("could not process required file ${file.filename} ")
			}

			// If we found an environment file, make note of it.
			if ( file.fileType == FileType.ENVIRONMENT && success ) {
				foundEnvFile = true;
			}
		}

		processEnvironmentProperties(project)
		processSystemProperties(project)
		processCommandProperties(project)
		// Make sure we got at least one environment file if we are not in the local environment.
		if ( envName != 'local' && !foundEnvFile ) {
			throw new FileNotFoundException("No environment files were found for the '$envName' environment")
		}

		// Register a task listener that adds the property checking helper methods.
		registerTaskListener(project)
	}

	/**
	 * Build a list of property files to process, in the order in which they
	 * need to be processed.
	 * @param project the project applying the plugin.
	 * @return a List of {@link PropertyFile}s
	 */
	def buildPropertyFileList(Project project, String envName) {
		def p = project
		def files = []
		while ( p != null ) {
			// We'll need to process the files from the top down, so build the list
			// backwards.
			files.add(0, new PropertyFile("${p.projectDir}/gradle-${envName}.properties", FileType.ENVIRONMENT))
			files.add(0, new PropertyFile("${p.projectDir}/gradle.properties", FileType.OPTIONAL))
			p = p.parent
		}
		// Add the rest of the files to the end.
		def userHome = project.getGradle().getGradleUserHomeDir();
		files.add(new PropertyFile("${userHome}/gradle.properties", FileType.OPTIONAL))
		// The user properties file is optional
		if ( project.hasProperty(project.propertiesPluginGradleUserNameProperty) ) {
			files.add(new PropertyFile("${userHome}/gradle-${project."$project.propertiesPluginGradleUserNameProperty"}.properties", FileType.REQUIRED))
		}
		return files
	}

	/**
	 * Process a file, loading properties from it, and adding tokens.
	 * @param filename the name of the file to process
	 * @param project the enclosing project.
	 * @param required whether or not processing this file is required.  Required
	 *        files that are missing will cause an error.
	 * @return whether or not we found the file requested.
	 */
	boolean processPropertyFile(Project project, PropertyFile file) {
		def loaded = 0
		def propFile = project.file(file.filename)
		if ( !propFile.exists() ) {
			project.logger.info("PropertiesPlugin:apply Skipping ${file.filename} because it does not exist")
			return false
		}
		project.file("${file.filename}").withReader {reader ->
			def userProps= new Properties()
			userProps.load(reader)
			userProps.each { String key, String value ->
				[
					'propertiesPluginEnvironmentNameProperty',
					project.propertiesPluginEnvironmentNameProperty,
					'propertiesPluginGradleUserNameProperty',
					project.propertiesPluginGradleUserNameProperty
				].each {
					if ((key == it) && (project.hasProperty(it) ? value != project."$it" : value)) {
						throw new GradleException("The property '$it' must not occur with a different value in the property files. " +
						                          "Current value: '${project.hasProperty(it) ? project."$it" : ''}'; New value: '$value'; Property file: '$file.filename'")
					}
				}

				project.ext.set(key, value)
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				project.ext.filterTokens[key] = value;
				def token = propertyToToken(key)
				project.ext.filterTokens[token] = value
				loaded++
			}
		}
		project.logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from ${file.filename}")
		return true
	}

	/**
	 * Process the environment properties, setting project properties and adding
	 * tokens for any environment variable starting with
	 * {@code ORG_GRADLE_PROJECT_}, per the Gradle specification.
	 * @param project the enclosing project.
	 */
	def processEnvironmentProperties(project) {
		def loaded = 0
		System.getenv().each { key, value ->
			if ( key.startsWith("ORG_GRADLE_PROJECT_") ) {
				project.ext.set(key.substring(19), value)
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				project.ext.filterTokens[key.substring(19)] = value;
				def token = propertyToToken(key.substring(19))
				project.ext.filterTokens[token] = value
				loaded++
			}
		}
		project.logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from environment variables")
	}

	/**
	 * Process the system properties, setting properties and adding tokens for
	 * any system property starting with {@code org.gradle.project.}, per the
	 * Gradle specification.
	 * @param project the enclosing project.
	 */
	def processSystemProperties(project) {
		def loaded = 0
		System.properties.each { key, value ->
			if ( key.startsWith("org.gradle.project.") ) {
				project.ext.set(key.substring(19), value)
				// add the property to the filter tokens, both in camel case and dot
				// notation.
				project.ext.filterTokens[key.substring(19)] = value;
				def token = propertyToToken(key.substring(19))
				project.ext.filterTokens[token] = value
				loaded++
			}
		}
		project.logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from system properties")
	}

	/**
	 * Process the command line properties, setting properties and adding tokens.
	 * @param project the enclosing project.
	 */
	def processCommandProperties(project) {
		def loaded = 0
		def commandProperties = project.gradle.startParameter.projectProperties
		commandProperties.each { key, value ->
			project.ext.set(key, value)
			// add the property to the filter tokens, both in camel case and dot
			// notation.
			project.ext.filterTokens[key] = value;
			def token = propertyToToken(key)
			project.ext.filterTokens[token] = value
			loaded++
		}
		project.logger.info("PropertiesPlugin:apply Loaded ${loaded} properties from the command line")
	}

	/**
	 * Register a task listener to add the property checking methods to all
	 * current tasks as well as tasks that are added to the project after the
	 * plugin is applied.
	 *
	 * @param project the project applying the plugin
	 */
	def registerTaskListener(project) {
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
						checkProperty(project, propertyName)
					}
				}
			}

			// now add the one that takes a list...
			task.ext.requiredProperties = { String[] propertyNames ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						for ( propertyName in propertyNames ) {
							checkProperty(project, propertyName)
						}
					}
				}
			}

			// add the recommendedProperty property
			task.ext.recommendedProperty = { String propertyName, String defaultFile=null ->
				project.gradle.taskGraph.whenReady { graph ->
					if (graph.hasTask(task.path)) {
						checkRecommendedProperty(project, propertyName, defaultFile)
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
							checkRecommendedProperty(project, propertyName, defaultFile)
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
	def checkProperty(project, propertyName) {
		if ( !project.hasProperty(propertyName) ) {
			throw new MissingPropertyException("You must set the '${propertyName}' property")
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
	def checkRecommendedProperty(project, propertyName, defaultFile) {
		if ( !project.hasProperty(propertyName) ) {
			def message = "WARNING: ${propertyName} has no value, using default"
			if ( defaultFile != null ) {
				message = message + " from ${defaultFile}"
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
	def propertyToToken(String propertyName) {
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
