package net.saliman.gradle.plugin.properties

/**
 * This enum defines the 3 kinds of files that the property plugin uses.
 * <p>
 * REQUIRED files must be present, or the build will fail immediately.<br>
 * OPTIONAL files don't need to be present at any level.<br>
 * ENVIRONMENT files are a special case.  Only one needs to exist in the project
 * hierarchy, and only if the environment name is "local".  This lets us scan
 * files to make sure at least one was present.
 * @author Steven C. Saliman
 */
public enum FileType {
	REQUIRED,
	OPTIONAL,
	ENVIRONMENT
}