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