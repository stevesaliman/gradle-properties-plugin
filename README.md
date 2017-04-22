# Gradle Properties Plugin #
The Properties plugin is a useful plugin that changes the way Gradle loads
properties from the various properties files.  See the [CHANGELOG](http://github.com/stevesaliman/gradle-properties-plugin/blob/master/CHANGELOG.md)
for recent changes.

The Properties plugin is designed to make it easier to work with properties that
change from environment to environment, or client to client. It makes life
easier for developers who are new to a project to configure and build,
because properties for multiple environments can be stored with the project
itself, reducing the number of external magic that needs to happen for a
project to run. It makes life easier for experienced developers to create
different configurations for different scenarios on their boxes.

Gradle can add properties to your project in several ways, as documented in the
Gradle [User Guide](https://docs.gradle.org/current/userguide/build_environment.html).
Gradle uses these ways in a particular order, and the value of a property in 
your project will be the value from the last thing that set the property.
Gradle's order of processing is:

1. The `gradle.properties` file in the parent project's directory, if the project
is a module of a multi-project build.

2. The `gradle.properties` file in the project directory

3. The `gradle.properties` file in the user's ${gradleUserHomeDir}/.gradle
directory.

4. Environment variables starting with ```ORG_GRADLE_PROJECT_```. For example,
   ```myProperty``` would be set if there is an environment variable named
   ```ORG_GRADLE_PROJECT_myProperty```. Case counts.

5. System properties starting with ```-Dorg.gradle.project.```. For example,
   ```myProperty``` would be set if Gradle was invoked with
   ```-Dorg.gradle.project.myProperty```.  Again, Case counts.

6. The command line properties set with -P arguments.

The properties plugin enhances this sequence by adding two additional types of
property files. One is for properties that change from environment to
environment, and the other is for properties that are common to a user (or
client), but change from user to user (or client to client).  The order of
execution for the properties plugin is:

1. The `gradle.properties` file in the parent project's directory, if the project
is a module of a multi-project build.

2. The `gradle-${environmentName}.properties` file in the parent project's
directory, if the project is a module of a multi-project build. If no
environment is specified, the plugin will assume an environment name of "local".
We *strongly* recommend adding `gradle-local.properties` to the `.gitignore` file of
the project so that developers' local configurations don't interfere with each
other.

3. The `gradle.properties` file in the project directory

4. The `gradle-${environmentName}.properties` file in the project directory.
if no environment is specified, the plugin will assume an environment name of
"local".  We *strongly* recommend adding `gradle-local.properties` to the `.gitignore`
file of the project so that developers' local configurations don't interfere
with each other.

5. The `gradle.properties` file in the user's ${gradleUserHomeDir}
directory. Properties in this file are generally things that span projects, or
shouldn't be checked into a repository, such as user credentials, etc.

6. If the ${gradleUserName} property is set, the properties plugin will load
properties from ${gradleUserHomeDir}/`gradle-${gradleUserName}.properties`.  This
file is useful for cases when you need to build for a different user or client
and you have properties that span projects.  For example you might have several
projects that have pages with a customized banner.  The contents of the banner
change from client to client.  The `gradle-${gradleUserName}.properties` file
is a great way to put the client's custom text into a single file per client,
and specify at build time which client's banners should be used.

7. Environment variables starting with ```ORG_GRADLE_PROJECT_```. For example,
   ```myProperty``` would be set if there is an environment variable named
   ```ORG_GRADLE_PROJECT_myProperty```. Case counts.

8. System properties starting with ```org.gradle.project.```. For example,
   ```myProperty``` would be set if Gradle was invoked with
   ```-Dorg.gradle.project.myProperty```.  Again, Case counts.

9. The command line properties set with -P arguments.

If the project was three levels deep in a project hierarchy, The steps 1 and 2
would apply to the root project, then the plugin would check the files in the
parent project before continuing on with steps 3 and 4.  More formally, the
plugin applies project and environment files, when found, from the root project
down to the project that applies the plugin.

The property names for ```environmentName``` and ```gradleUserName```
can be configured if you don't like their name or there is a clash with
properties you already use in your build. The property name for
```environmentName``` can be configured with the property
```propertiesPluginEnvironmentNameProperty``` and the property name for
```gradleUserName``` can be configured with the property
```propertiesPluginGradleUserNameProperty```. Those properties have
to be set before this plugin is applied. That means you can put them in the
standard property file locations supported by Gradle itself, in environment
variables, system properties, -P options or in the build.gradle file itself
before applying this plugin.

You can also change the directory where the gradle-${environmentName}.properties
files live.  The ```environmentFileDir``` property can be set to the name of a
directory where the environment files live.  Note that this property has no
effect on the main ```gradle.properties``` file, because that file's location
is specified by Gradle itself.  If the name ```environmentFileDir``` causes a
problem in your environment, you can change it via the 
```propertiesPluginEnvironmentFileDirProperty``` property.

As with standard Gradle property processing, the last one in wins. The
properties plugin also creates a "filterTokens" property that can be used to
do token replacement in files, allowing Gradle to edit configuration files
for you.  See Gradle's documentation for the ```copy``` task for more
information on filtering.

Gradle can also set a Java system property from the properties it sees in the 
various property files.  The Gradle [User Guide](https://docs.gradle.org/current/userguide/build_environment.html)
describes the
process in more detail, but basically, properties that start with "systemProp."
will be converted into Java system properties, but only if they are in the 
```gradle.properties``` file in either the root project's directory or the 
user's home directory.  The Gradle Properties plugin adds the 
gradle-${environmentName}.properties in the project's root directory, and the
${gradleUserHomeDir}/gradle-${gradleUserName}.properties file to the list of
files that can contain properties from which Java system properties can be set.

Gradle plugins can also be applied to a Settings object by applying the plugin 
in the settings.gradle file.  Gradle handles properties a little differently 
when processing a settings.gradle file, so the plugin's behavior changes as 
well.  When applied in a settings.gradle file, the plugin processes files in the
following order:

1. The `gradle.properties` file in the directory where `settings.gradle` is
   located.

2. The `gradle-${environmentName}.properties` file in the directory where
   `settings.gradle` is located. If no environment is specified, the plugin will
   assume an environment name of "local".  We strongly recommend adding
   `gradle-local.properties` to the `.gitignore` file of the project so that
   developers' local configurations don't interfere with each other.

3. The files described in steps 5-9 for applying to a project.

Gradle technically allows plugins to be applied in an init.gradle file, but 
Gradle recommends against it, because the init.gradle file is designed for 
configuration and not plugin application.  If you really want to apply this plugin in the init.gradle file, you can do 
it by wrapping it in an ```allprojects``` block like this:
```gradle
initscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "net.saliman:gradle-properties-plugin:1.4.6"
    }
}
allprojects {
  apply plugin: net.saliman.gradle.plugin.properties.PropertiesPlugin
}
```
Note that this example uses a class name instead of a plugin id.  This is to
workaround a bug in Gradle that prevents plugins from being found by id inside
an init.gradle file.

# Why should I use it? #
One of the challenges to building a project is that it often contains things
that should be changed from environment to environment. This includes things
like the log file directory, or the JNDI data source name.  The values of these
properties are often complicated by platform differences between the OSX and
Windows environments most developers use and the Unix environments used for
deployments.  It gets even messier when you consider that these values are
often scattered amongst several files buried in the project.

This can be a real hindrance to new developers to a project, who don't know all
the touch points of project configuration, or experienced developers who don't
remember all the things that need to be changed to spin up a new environment.

The solution is to extract all the environment specific information to a single
file for each environment, with everything the project needs in that single
file.  Files that need these values, like persistence.xml or log4j.properties,
become template files with tokens that can be replaced by the build.

This provides an added benefit; new developers see some of the things that
need to be setup external to the project.  For example, if the project mentions
a tomcat home, the developer knows they need to install tomcat.  If they see a
JNDI data source, they know they need to set one up inside their container.

# How do I use it? #
The initial setup of a project is a little involved, but once done, this process
greatly simplifies things for the developers who follow.

**Step 1: Extract environment sensitive files to a template directory**

Make a directory to hold the template files and copy files that change, like
log4j.properties, into it.

Edit the files and replace the environment specific values with tokens.  For
example, if the file had a property like "log.dir=/opt/tomcat/logs", replace it
with "log.dir=@application.log.dir@".

**Step 2: Create property files**

Create a file for each named environment you use for deployment.  For example,
if you have "dev", "qa", and "prod" environments.  You would create
gradle-dev.properties, gradle-qa.properties and gradle-prod.properties files.
There is no limitation on environment names, they are just labels to tell the
plugin which file to read at build time.  If no environment is specified, the
properties plugin uses the gradle-local.properties file, which should not be
checked in to source control, because this is the file where developers setup
the properties for their own machines.

In our example, the gradle-local.properties file might contain
"applicationLogDir=c:/opt/tomcat" or
"applicationLogDir=/Users/steve/projects/myproject/logs" to define the log
directory needed by the log4j.properties template.

**Step 3: Include and apply the plugin**

To use the plugin with Gradle 2.1 or later, add the following to the 
build.gradle file:

```groovy
plugins {
  id 'net.saliman.properties' version '1.4.6'
}
```

To use the plugin with Gradle 2.0 or older, add the following to build.gradle:

```groovy
// Pull the plugin from Maven Central
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'net.saliman:gradle-properties-plugin:1.4.6'
	}
}

// invoke the plugin
apply plugin: 'net.saliman.properties'
```

Note that this only applies the plugin to the current project. To apply the
plugin to all projects in a multi-project build, use the following instead of
```apply plugin: 'net.saliman.properties'```:
```groovy
allprojects {
  apply plugin: 'net.saliman.properties'
}
```

If you also use your properties during the initialization phase in settings.gradle,
you can also apply the plugin there. In this case the properties files of the root
project and the properties files beneath the settings.gradle file are read, in that order.
After those, as usual the property files in ${gradleUserHomeDir}, the environment variables,
the system properties and the command line properties are handled.
Due to a bug in Gradle, you currently (Gradle 1.11) cannot use 
```apply plugin: 'net.saliman.properties'``` in the settings.gradle file, but 
you have to use the full class name
```apply plugin: net.saliman.gradle.plugin.properties.PropertiesPlugin```.

When the properties plugin is applied, three things happen. First, the plugin
processes the various property files and property location as described above.

Next, the plugin creates a property named ```filterTokens```. The filter
tokens is a map of name-value pairs that can be used when doing a filtered file
copy.  There will be token for each property defined in the given properties
file. The name of each token will be the name of the property from the
properties file. For convenience, The plugin will also create a filter with the
property name in dot notation to allow re-use of file templates created for Ant
builds.  The plugin only creates dot notation tokens for properties that start 
with a lower case letter.  This means that in our example, the 
"applicationLogDir=/opt/tomcat/logs" entry in the property file will create 2 
tokens.  One will be named "applicationLogDir", and the other will be named 
"application.log.dir".  They will both have a value of /opt/tomcat/logs.  If the
property had been named APPLICATION_LOG_DIR, or _applicationLogDir, only the 
original property would be in the filter tokens.  No copy would be made under 
any other names.

Finally, the properties plugin - if applied to a project rather than a
settings instance - adds some property closures to every task in the
build (and causes them to be added to new tasks defined later). These properties
can be used in the configuration of a task define which properties must (or
should) be present for that task to work.  Properties will be checked between
the Gradle configuration and execution phases.

**Step 4: Define prep tasks**

Before the build can happen, tokenized files need to be copied to the right
locations in the project.  This can be done with a task like the following:

```groovy
task prep() {
    requiredProperties "applicationLogDir", "logfileName", "defaultLogLevel"
    outputs.file new File(resourceDir, 'log4j.properties')
    doFirst {
        copy {
            from templateDir
            include 'log4j.properties'
            into resourceDir
            filter(org.apache.tools.ant.filters.ReplaceTokens, tokens: project.filterTokens)
        }
    }
}
```

Note that this ```prep``` task adds a file to the ```outputs```.  We recommend
setting outputs so that the task can take advantage of Gradles Up-To-Date
detection.  The properties plugin will add all the properties from the 
```requiredProperties``` and ```recommendedProperties``` methods to the list of
task inputs, but unless a task declares outputs, Gradle will always assume that
the task is out of date.

Once a ```prep``` task is defined, other tasks in the project can then depend on
this it to make sure they don't run unless the tokenized files are in the right
place.  For example, a Java project might define the following in the build 
script:

```groovy
compileJava.dependsOn prep
```

The reason we copy into the source resource directory instead of the build
directory is to keep IDEs happy.  In general, you'll want to run the prep (or
a prepTest) task whenever properties or template files change, or to switch
environments.

# Properties added to each task #
This plugin adds some methods to each task, which can be used to check for the
presence of properties after configuration, but before any tasks are executed.
These methods will also add properties to the task's inputs that can be used
to determine when a task is up to date.  A task is never up to date if there 
are no outputs, but if the task has defined any outputs, Gradle will consider
the task up to date if the other inputs and outputs haven't changed, and the
properties haven't either.

**requiredProperty**

 ```groovy
 requiredProperty "somePropertyName"
 ```
This method throws a MissingPropertyException if the named property is not defined.

**requiredProperties**

```groovy
requiredProperties "property1", "property2", ...
```

This method throws a MissingPropertyException if any of the named properties are not defined

**recommendedProperty**

```groovy
recommendedProperty "somePropertyName", "default File Text"
```
This method is handy when there are properties that have defaults somewhere else.
For example, the build file might define it, or the application might be able to
get it from a system file. It is most useful in alerting newer developers that
something must be configured somewhere on their systems.

The method checks to see if the given property is defined. If it is not, a warning
message is displayed alerting the user that a default will be used, and if the
defaultFile has been given, the message will include it so that the developer
knows which file will be providing the default value.

**recommendedProperties**

```groovy
recommendedProperties names: ["property1", "property2", ...], defaultFile: "default File Text"
```

This method checks all the given property names, and prints a message if we're missing any.

# Notes #
If a property is set in the build.gradle file before the properties plugin is
applied, and it happens to match a property in one of the standard locations
defined earlier, the build.gradle property's value will be overwritten.  If
you need to set a property in build.gradle, it is best to do it after the
properties plugin is defined.  Keep in mind that properties set in the
build.gradle file will not be in the filterTokens.

There are a few ways to change the Gradle user home directory. The properties
plugin uses the Gradle user home directory that also Gradle itself uses, but
I haven't done much testing of that yet.

It is not required to have a gradle-local.properties file, but if you specify
an environment with the ```-PenvironmentName=x``` flag, the environment file
for that environment must exist at least once in the project hierarchy.

# Acknowledgements #
A special thank you to to Hans Dockter at Gradleware for showing me how to
dynamically define the requiredProperty method and attach it to the right place
in the Gradle build lifecycle.

