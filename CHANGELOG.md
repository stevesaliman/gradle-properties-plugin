Changes for 1.6.0
=================

- Updated the gradle wrapper to version 7.6.1

- The plugin must now be applied by its qualified name.

Changes for 1.5.2
=================

- Fixed a bug with the `recommendedProperty` method, which was throwing exceptions when the property
  was missing, which defeated the purpose of the method.

- Added an `additionalInfo` parameter to the `recommendedProperty` and `recommendedProperties`
  methods that get added to each task in the build.

Changes for 1.5.1
=================

- Re-published the plugin using a newer version of the plugin-publish-plugin.  The old version had
  some known bugs, which were discussed in 
  https://discuss.gradle.org/t/weird-fail-on-plugins-gradle-org-enunciate-2-10-0/24573.  This should
  resolve Issue #31
  
Changes for 1.5.0
=================
- Added a `propertiesPluginIgnoreMissingEnvFile` to override the plugin's default behavior of
  failing the build when an environment specific file can't be found.  This is most when you only 
  need to override properties in some, but not all, environments, and you don't want to define a 
  bunch of empty files.  It is also handy when you are using Spring Boot for project properties, but
  still use Gradle for support tasks, and you want to use the `requiredProperties` method.
 
- Dropped support for Java 6 and Java 7.

- Updated the Gradle wrapper to 4.10.3.  Gradle 5 is coming soon.
  
Changes for 1.4.6
=================
- Fixed a bug when applying the plugin in multi module builds.  Gradle's `hasProperty` method was
  finding properties in the parent project, so we were not setting project.ext properties in child
  projects. (Issue #24)

Changes for 1.4.5
=================
- Added support for setting system properties from properties in "environment" files that start with
  "systemProp.", just like gradle itself does with the gradle.properties file. (Issue #23)

Changes for 1.4.4
=================
- Previously, all properties were added to filter tokens in their original form and dot notation.
  The plugin now does this only if the property starts with a lower case letter (Issue #22)
  
Changes for 1.4.3
=================
- Added code to help Gradle figure out that things are not up-to-date when a property changes 
  (Issue #20)
  
- Added the ability to change the location of the environment specific files (issue #15)
  
Changes for 1.4.2
=================
- Changed the plugin id to "net.saliman.properties" and added it to the new Gradle Plugin repository.

Changes for 1.4.1
=================
- Fixed a bug that could occur when applying to a settings object.

Changes for 1.4.0
=================
Thank you to Björn Kautler for his ideas and assistance with the changes made for version 1.4.0.

- The plugin can now be applied to settings via settings.gradle. (Issue #13)

- Errors and omissions from the README have been corrected.

- The names of the properties where the plugin will get the environment name and the user name are
  now configurable (Issue #9).

- The maven central credentials are now only required if I'm actually uploading to maven central, so
  other users can now build the plugin. (Issue #10).

Changes for 1.3.1
=================
- Filter tokens used to contain property names in dot notation (some.var.name), they are now also
  added in original camel case (someVarName) to resolve issue #5

Changes for 1.3.0
=================
- Added support for setting a property via environment variables or system properties, as specified
  in the Gradle user guide, section 14.2. (Issue #4)
