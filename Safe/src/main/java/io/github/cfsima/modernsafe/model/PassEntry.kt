/* $
 *
 * Copyright 2007-2008 Steven Osborn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cfsima.modernsafe.model

import java.util.ArrayList

/**
 * @author Steven Osborn - http://steven.bitsetters.com
 */
class PassEntry {
    var id: Long = -1
    var needsDecryptDescription: Boolean = false
    var needsDecrypt: Boolean = false
    var needsEncrypt: Boolean = true
    var password: String = ""
    var category: Long = 0
    var categoryName: String? = null
    var description: String = ""
    var username: String? = null
    var website: String? = null
    var uniqueName: String? = null
    var packageAccess: ArrayList<String>? = null
    var note: String? = null
    var plainPassword: String? = null
    var plainDescription: String = ""
    var plainUsername: String? = null
    var plainWebsite: String? = null
    var plainNote: String? = null
    var plainUniqueName: String? = null
    var lastEdited: String? = null

    companion object {
        fun checkPackageAccess(packageAccess: ArrayList<String>, packageName: String): Boolean {
            return packageAccess.contains(packageName)
        }
    }
}
