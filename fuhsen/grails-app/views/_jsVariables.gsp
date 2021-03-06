<%--
Copyright (C) 2014 FIZ Karlsruhe
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<div id="globalJsVariables" class="off" 
    data-js-context-path="${request.contextPath}"
    data-js-language="<g:message encodeAs="html" code="ddbnext.language"/>"
    data-js-longitude="${selectedOrgXML?.locations?.location?.geocode?.longitude}" 
    data-js-latitude="${selectedOrgXML?.locations?.location?.geocode?.latitude}"
    data-js-loggedin="<ddbcommon:isLoggedIn>true</ddbcommon:isLoggedIn><ddbcommon:isNotLoggedIn>false</ddbcommon:isNotLoggedIn>">
</div>