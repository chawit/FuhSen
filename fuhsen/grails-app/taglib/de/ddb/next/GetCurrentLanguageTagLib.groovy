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

import org.springframework.web.servlet.support.RequestContextUtils

class GetCurrentLanguageTagLib {

    static namespace = "ddb"

    def messageSource
    def languageService

    /**
     * Prints out the currently selected language. The language itself is internationalized. The language must be
     * available as entry in the message.property files with the format "ddbnext.language_<ISO2-language>".
     */
    def getCurrentLanguage = { attrs, body ->
        def locale = languageService.getBestMatchingLocale(RequestContextUtils.getLocale(request))

        def localeLanguage = locale.getLanguage()
        def i18nLanguageString = messageSource.getMessage("ddbnext.language_"+localeLanguage, null, locale)

        out << i18nLanguageString
    }
}
