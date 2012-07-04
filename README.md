# Gradle Properties Plugin #
The Properties plugin is designed to make it easier to work with properties that change from environment to environment. It is intended to make life easier for developers who are new to a project to configure and build a project.  It it also intended to make it easier for experienced developers to create different configurations for different scenarios on their boxes.

# Why should I use it? #
One of the challenges to building a project is that it often contains things that should be changed from environment to environment. This includes things like the log file directory, or the JNDI data source name.  The values of these properties are often complicated by platform differences between the OSX and Windows environments most developers use and the Unix environments used for deployments.  It gets even messier when you consider that these values are often scattered amongst several files buried in the project.

This can be a real hindrance to new developers to a project, who don't know all the touch points of project configuration, or experienced developers who don't remember all the things that need to be changed to spin up a new environment.

The solution is to extract all the environment specific information to a single file for each environment, with everything the project needs in that single file.  Files that need these values, like persistence.xml or log4j.properties, become template files with tokens that can be replaced by the build.

This provides an added benefit; new developers see some of the things that need to be setup external to the project.  For example, if the project mentions a tomcat home, the developer knows they need to install tomcat.  If they see a JNDI data source, they know they need to set one up inside their container.

# How do I use it? #
The initial setup of a project is a little involved, but once done, this process greatly simplifies things for the developers who follow.

**Step 1: Extract environment sensitive files to a template directorty**

Make a directory to hold the template files and copy files that change, like log4j.properties, into it.

Edit the files and replace the environment specific values with tokens.  For example, if the file had a property like "log.dir=/opt/tomcat/logs", replace it with "log.dir=@application.log.dir@".

**Step 2: Create property files**

Create a file for each named environment you use for deployment.  For example, if you have "dev", "qa", and "prod" environments.  You would create gradle-dev.properties, gradle-qa.properties and gradle-prod.properties files.  There is no limitation on environment names, they are just labels to tell the plugin which file to read at build time.  If no environment is specified, the properties plugin uses the gradle-local.properties file, which should not be checked in to source control, because this is the file where developers setup the properties for their own machines.

In our example, the gradle-local.properties file might contain "applicationLogDir=c:/opt/tomcat" or "applicationLogDir=/Users/steve/projects/myproject/logs" to define the log directory needed by the log4j.properties template.

**Step 3: Include and apply the plugin**

Add the following to the build.gradle file:

```groovy

// Pull the plugin from Maven Central
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'com.saliman:gradle-properties-plugin:1.0.0'
	}
}

// invoke the plugin
apply plugin: 'properties'
```
When the properties plugin is applied, three things happen. First, the plugin looks for and reads the gradle-<environmentName>.properties file in the root directory of the project.  If no environmentName is given when Gradle is invoked, the default environment "local" will be used. Properties already defined using the -P option on the command line will be be preserved.  In other words, command line properties override file properties.

Next, the plugin creates a project property named "filterTokens".  The filter tokens is an array of name-value pairs that can be used when doing a filtered file copy.  There will be token for each property defined in the given properties file. The name of each token will be the name of the property from the properties file, after converting the camel case property name to dot notation. In our example, the "applicationLogDir=/opt/tomcat/logs" entry in the property file will create a token named "application.log.dir" with the value of /opt/tomcat/logs

Finally, the properties plugin adds the {@code requiredProperty}
and {@code requiredProperties} properties to every task in the build (and causes it to be added to new tasks defined later). These properties can be used in the configuration of a task define which properties must be present for that task to work.  Required properties will be checked between the Gradle configuration and execution phases.

**Step 4: Define prep tasks**

Before the build can happen, tokenized files need to be copied to the right locations in the project.  Thisorg.apache.tools.ant.filters.ReplaceTokens, tokens: can be done with a task like the following:

```groovy
task prep() {
    requiredProperties "applicationLogDir", "logfileName", "defaultLogLevel"
    doFirst {
        copy {
            from templateDir
            include "log4j.properties"
            into srcResourceDir
            filter(org.apache.tools.ant.filters.ReplaceTokens, tokens:  project.ext.filterTokens)
        }
    }
}

compileJava.dependsOn << "prep"
```

The reason we copy into the source resource directory instead of the build directory is to keep IDEs happy.  In general, you'll want to run the prep (or prepTest) task whenever properties or template files change, or to switch environments.

# Acknowledgements #
A special thank you to to Hans Dockter at Gradleware for showing me how to dynamically define the requiredProperty method and attach it to the right place in the Gradle build lifecycle.

