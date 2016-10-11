Changes for 1.4.6
=================
- Fixed a bug when applying the plugin in multi module builds.  Gradle's 
  ```hasProperty``` method was finding properties in the parent project, so we
  were not setting project.ext properties in child projects. (Issue #24)

Changes for 1.4.5
=================
- Added support for setting system properties from properties in "environment" 
  files that start with "systemProp.", just like gradle itself does with the
  gradle.properties file. (Issue #23)

Changes for 1.4.4
=================
- Previously, all properties were added to filter tokens in their original form
  and dot notation.  The plugin now does this only if the property starts with
  a lower case letter (Issue #22)
  
Changes for 1.4.3
=================
- Added code to help Gradle figure out that things are not up-to-date when a 
  property changes (Issue #20)
  
- Added the ability to change the location of the environment specific files
  (issue #15)
  
Changes for 1.4.2
=================
- Changed the plugin id to "net.saliman.properties" and added it to the new 
  Gradle Plugin repository.

Changes for 1.4.1
=================
- Fixed a bug that could occur when applying to a settings object.

Changes for 1.4.0
=================
Thank you to Björn Kautler for his ideas and assistance with the changes made
for version 1.4.0.
- The plugin can now be applied to settings via settings.gradle. (Issue #13)

- Errors and omissions from the README have been corrected.

- The names of the properties where the plugin will get the environment name
  and the user name are now configurable (Issue #9).

- The maven central credentials are now only required if I'm actually uploading
  to maven central, so other users can now build the plugin. (Issue #10).

Changes for 1.3.1
=================
- Filter tokens used to contain property names in dot notation (some.var.name),
  they are now also added in original camel case (someVarName) to resolve
  issue #5

Changes for 1.3.0
=================
- Added support for setting a property via environment variables or system 
  properties, as specified in the Gradle user guide, section 14.2. (Issue #4)
