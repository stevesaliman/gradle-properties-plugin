package net.saliman.gradle.plugin.properties

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This is the main class for the properties plugin. When the properties
 * plugin is applied, three things happen.
 * First, the plugin looks for a gradle-<b>environmentName</b>.properties
 * file in the root directory of the project containing properties for the
 * project.  If no environmentName is given, &quot;local&quot; will be used.
 * Properties already defined on the command line with the -P option will be
 * will not be overwritten by properties in the file.
 * <p>
 * Next, the plugin also creates filterTokens property containing an array
 * of tokens that can be used when doing a filtered file copy.  There will be
 * a token for each property defined in the given properties file. The name
 * of each token will be the name of the property from the properties file,
 * after converting the camel case property name to dot notation. For example,
 * if you have a myPropertyName property in the file, the plugin will create
 * a my.property.name filter token, whose value is the property's value.\
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
		if ( !project.hasProperty('environmentName' ) ){
			project.ext.setProperty('environmentName', 'local')
		}

		// Load the environment file
		def filename = "gradle-${project.ext.environmentName}.properties"
		def propertyCount = 0
		def tokenCount = 0
		project.file("${filename}").withReader {reader ->
			def userProps= new Properties()
			project.ext.filterTokens = [:]
			userProps.load(reader)
			userProps.each { String key, String value ->
				if ( !project.hasProperty(key) ) {
					project.ext.setProperty(key, value)
					propertyCount++
				} else {
					println "PropertiesPlugin:apply ${key} was defined on the command line"
				}
				def token = propertyToToken(key)
				project.ext.filterTokens[token] = project.getProperties()[key]
				tokenCount++
			}
			println "PropertiesPlugin:apply Loaded ${propertyCount} properties from ${filename} and created ${tokenCount} tokens"
		}

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
