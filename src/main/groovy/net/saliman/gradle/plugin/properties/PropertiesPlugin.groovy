package net.saliman.gradle.plugin.properties

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This is the main class for the properties plugin. When the properties
 * plugin is applied, it reloads all the project properties in the following
 * order:<br>
 * <ol>
 * <li>
 * gradle.properties in the project directory
 * </li>
 * <li>
 * gradle-${environmentName}.properties in the project directory.  If no
 * environment name is specified, the default is "local".
 * </li>
 * <li>
 * gradle.properties in the user's ${home}/.gradle directory.
 * </li>
 * <li>
 * gradle-${gradleUserName}.properties in the user's ${home}/.gradle directory.
 * </li>
 * <li>
 * properties defined on the command line with the -P option.
 * </li>
 * </ol>
 * The last thing to set a property wins.  All files are optional unless an
 * environment or user is specified, in which case the file belonging to the
 * specified environment or user must exist.
 * <p>
 * As properties are set, they are also placed in a filterTokens property.
 * the filterTokens property is a map of tokens that can be used when doing a
 * filtered file copy.  There will be a token for each property defined by one
 * of the 5 listed methods.  The name of each token will be the name of the
 * property from the properties file, after converting the camel case property
 * name to dot notation. For example, if you have a myPropertyName property in
 * the file, the plugin will create a my.property.name filter token, whose
 * value is the property's value.
 * <p>
 * Finally, the properties plugin also adds some properties to every task in
 * your build:
 * <p>{@code requiredProperty} and {@code requiredProperties} can be used to
 * define what properties must be present for that task to work.  Required
 * properties will be checked after configuration, is complete, but before any
 * tasks are executed.
 * <p>
 * {@code recommendedProperty} and {@code recommendedProperties} can be used
 * to define properties that the task can work without, but it (or the deployed
 * application) will use a default value for the property.  The value of this
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
		def userHome = project.getGradle().getGradleUserHomeDir();
		// If the user hasn't set an environment, assume "local"
		if ( !project.hasProperty('environmentName' ) ){
			project.ext.setProperty('environmentName', 'local')
		}
		project.ext.filterTokens = [:]

		// process files in reverse order.  Last one in wins.
		processFile("gradle.properties", project, false)
		processFile("gradle-${project.ext.environmentName}.properties", project, project.ext.environmentName != "local")
		processFile("${userHome}/gradle.properties", project, false)
		// The user properties file is optional
		if ( project.hasProperty('gradleUserName') ) {
			processFile("${userHome}/gradle-${project.ext.gradleUserName}.properties", project, true)

		}
		processCommandProperties(project)

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
	 * Gelper method to check a recommended property and print a warning if it is
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

	/**
	 * Process a file, loading properties from it, and adding tokens.
	 * @param filename the name of the file to process
	 * @param project the enclosing project.
	 * @param required whether or not processing this file is required.  Required
	 *        files that are missing will cause an error.
	 */
	def processFile(String filename, Project project, Boolean required) {
		def loaded = 0
		def propFile = project.file(filename)
		if ( propFile.exists() || required ) {
			project.file("${filename}").withReader {reader ->
				def userProps= new Properties()
				userProps.load(reader)
				userProps.each { String key, String value ->
					project.ext.setProperty(key, value)
					def token = propertyToToken(key)
					project.ext.filterTokens[token] = value
					loaded++
				}
			}
			println "PropertiesPlugin:apply Loaded ${loaded} properties from ${filename}"
		}
	}

	/**
	 * Process the command line properties, setting properties and adding tokens.
	 * @param project the enclosing project.
	 */
	def processCommandProperties(project) {
		def loaded = 0
		def commandProperties = project.gradle.startParameter.projectProperties
		commandProperties.each { key, value ->
			project.setProperty(key, value)
			def token = propertyToToken(key)
			project.ext.filterTokens[token] = project.getProperties()[key]
			loaded++
		}
		println "PropertiesPlugin:apply Loaded ${loaded} properties from the command line"
	}
}
