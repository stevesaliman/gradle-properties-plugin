package net.saliman.gradle.plugin.properties

/**
 * Simple little wrapper for a gradle file that has the filename and whether
 * or not the file is required.
 * @author Steven C. Saliman
 */
class PropertyFile {
	String filename
	FileType fileType

	/**
	 * Populating constructor
	 * @param filename the fully qualified name of the file
	 * @param required whether or not the file is required
	 */
	PropertyFile(String filename, FileType fileType) {
		this.filename = filename
		this.fileType = fileType
	}

	@Override
	String toString() {
		return "${filename} (${fileType})"
	}
}
