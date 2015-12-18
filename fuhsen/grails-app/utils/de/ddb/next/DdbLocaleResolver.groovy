/*
 * Copyright (C) 2014 FIZ Karlsruhe
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
package de.ddb.next

import javax.servlet.http.HttpServletRequest

import net.jawr.web.resource.bundle.locale.LocaleResolver

import org.springframework.web.servlet.support.RequestContextUtils

/**
 * Tell JAWR plugin the current locale.
 */
class DdbLocaleResolver implements LocaleResolver {

    def languageService

    /**
     * Ensure that there is always a locale value != null returned.
     */
    public String resolveLocaleCode(HttpServletRequest request) {
        Locale result = null
        if (request) {
            result = RequestContextUtils.getLocale(request)
        }
        if (!result) {
            result = languageService.getDefaultLocale()
        }
        return result
    }
}