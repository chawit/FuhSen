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
package de.ddb.common

import de.ddb.common.beans.User

class IsNotLoggedInTagLib {

    static namespace = "ddbcommon"

    def userService
    def sessionService

    def isNotLoggedIn = { attrs, body ->
        
        //TODO
        //This method has to check only person object in the future.
        //But now DDBnext still uses User object and because of this is hybrid.
        
        def isLoggedIn = userService.isUserLoggedIn()
        
        if (!isLoggedIn) {
            isLoggedIn = sessionService.isPersonLoggedIn()
        }

        if(!isLoggedIn){
            out << body()
        }else{
            out << ""
        }
    }
}
