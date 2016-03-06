/*
 * Copyright 2012-2016 Steven C. Saliman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.saliman.gradle.plugin.properties

/**
 * Simple little wrapper for a gradle file that has the filename and whether
 * or not the file is required.
 * @author Steven C. Saliman
 */
class PropertyFile {
	String filename
	FileType fileType
	boolean containsSystemProperties

	/**
	 * Populating constructor
	 * @param filename the fully qualified name of the file
	 * @param required whether or not the file is required
	 * @param containsSystemProperties whether or not this file can contain
	 *        properties that should be converted into System properties.
	 */
	PropertyFile(String filename, FileType fileType, containsSystemProperties) {
		this.filename = filename
		this.fileType = fileType
		this.containsSystemProperties = containsSystemProperties
	}

	@Override
	String toString() {
		return "${filename} (${fileType})"
	}
}
