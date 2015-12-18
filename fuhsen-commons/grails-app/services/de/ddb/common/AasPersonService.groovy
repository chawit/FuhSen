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

import static groovyx.net.http.ContentType.*
import groovy.json.*

import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.jackson.map.ObjectMapper

import de.ddb.common.beans.aas.AasCredential
import de.ddb.common.beans.aas.OrganizationTreeObject
import de.ddb.common.beans.aas.Person
import de.ddb.common.beans.aas.PersonSearchResult
import de.ddb.common.beans.aas.Privilege

/**
 * Set of Methods that encapsulate REST-calls to the persons endpoint of AASWebService
 *
 * @author boz
 */
class AasPersonService {

    def configurationService
    def aasOrganizationService
    def sessionService
    def transactional = false

    private static final log = LogFactory.getLog(this)
    private static final String PERSON_PATH = "persons/"
    private static final String PRIVILEGE_PATH = "privileges/"
    private static final String ORGANIZATION_PATH = "organizations/"
    private static final String AUTHENTICATION_PATH = "authentication/"
    private static final String RESET_PASSWORD = "resetpassword/"

    public static final String UNKNOWN_SURNAME = "surname is unknown"
    private static final String PASSWORD = "password"
    private static final String GLOBAL_ADMIN = "ADMIN"
    private static final String ADMIN_ORG = "ADMIN_ORG"

    /**
     * Retrieves a person with the given id from the aas
     * @param id the id of a person
     * @param Credentials object of the logged user 
     * 
     * @return a person with the given id from the aas
     */
    public Person getPerson(String id, AasCredential cred) {
        Person person
        ObjectMapper mapper = new ObjectMapper()
        ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:],
        getUserBasicAuth(cred.getId(), cred.getPassword()), false, false)

        if (apiResponse.isOk()) {
            JSONObject jsonObject = apiResponse.getResponse()
            person = mapper.readValue(jsonObject.toString(), Person.class)
        }
        return person
    }

    /**
     * Retrieves a person with the given id from the aas as admin
     * @param id the id of a person
     *
     * @return a person with the given id from the aas
     */
    public Person getPersonAsAdmin(String id) {
        Person person
        ObjectMapper mapper = new ObjectMapper()
        ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:],
        getAdminBasicAuth(), false, false)

        if (apiResponse.isOk()) {
            JSONObject jsonObject = apiResponse.getResponse()
            person = mapper.readValue(jsonObject.toString(), Person.class)
        }
        return person
    }

    /**
     * Retrieves all persons from the aas
     * 
     * @param searchQuery
     * @param Credentials object of the logged user
     *
     * @return a PersonSearchResult of all persons
     */
    public PersonSearchResult searchPersons(searchQuery=[:], AasCredential cred) {
        ObjectMapper mapper = new ObjectMapper()
        def optionalHeaders = getTextAndAuthHeader(cred)

        def apiResponse = ApiConsumer.getText(configurationService.getAasUrl(), PERSON_PATH, false, searchQuery, optionalHeaders, false, false, false)

        if (!apiResponse.isOk()) {
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        def textResponse = apiResponse.getResponse()
        return mapper.readValue(textResponse, de.ddb.common.beans.aas.PersonSearchResult.class)
    }

    /**
     * Creates a person with the given person object
     * @param id the id of a person
     */
    public void createPerson(Person person) {
        ObjectMapper mapper = new ObjectMapper()
        String personJSON = mapper.writeValueAsString(person)
        def apiResponse = ApiConsumer.postJson(configurationService.getAasUrl(), PERSON_PATH, true, new JSONObject(personJSON))

        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        //Get the autogenerated id from the JSON result
        person.setId(apiResponse.getResponse().id)
    }

    /**
     * Updates a person with the given person object values
     * @param id the id of a person
     * @param Credentials object of the logged user
     */
    public void updatePerson(Person person, AasCredential cred) {
        ObjectMapper mapper = new ObjectMapper()
        String personJSON = mapper.writeValueAsString(person)
        def apiResponse = ApiConsumer.putJson(configurationService.getAasUrl(), PERSON_PATH + person.id, false, personJSON, [:], getUserBasicAuth(cred.getId().toString(), cred.getPassword().toString()))
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }

    /**
     * Deletes a person with the given person object values
     * @param id the id of a person
     * @param Credentials object of the logged user
     */
    public void deletePerson(AasCredential cred) {
        def apiResponse = ApiConsumer.deleteJson(configurationService.getAasUrl(), PERSON_PATH + cred.getId(), false, [:], getUserBasicAuth(cred.getId(), cred.getPassword()))
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }

    /**
     * Updates a person with the given person object values as Admin
     * @param id the id of a person
     */
    public void updatePersonAsAdmin(Person person) {
        updatePerson(person, getAdminUser())
    }

    /**
     * Deletes a person with the given person object values as Admin
     * @param id the id of a person
     *
     */
    public void deletePersonAsAdmin(String id) {
        AasCredential cred =  getAdminUser()
        def apiResponse = ApiConsumer.deleteJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:], getUserBasicAuth(cred.getId(), cred.getPassword()))
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }

    /**
     * Retrieves all persons from the aas
     * @param searchQuery
     *
     * @return PersonSearchResult of all persons
     */
    public PersonSearchResult searchPersonsAsAdmin(searchQuery=[:]) {
        return searchPersons(searchQuery, getAdminUser())
    }

    // Not related with backend

    def getAdminBasicAuth(){
        String auth = configurationService.getAasAdminUserId() + ":" + configurationService.getAasAdminPassword()
        def basicAuth = ['Authorization':'Basic ' + auth.bytes.encodeBase64().toString()]

        return basicAuth
    }

    def getUserBasicAuth(String id, String pass){
        String auth = id + ":" + pass
        def basicAuth = ['Authorization':'Basic ' + auth.bytes.encodeBase64().toString()]
        return basicAuth
    }

    AasCredential getAdminUser(){
        AasCredential adminUser = new AasCredential(configurationService.getAasAdminUserId(), configurationService.getAasAdminPassword())
        return adminUser
    }

    def getUserBasicAuth(AasCredential cred){
        String auth = cred.getId() + ":" + cred.getPassword()
        def basicAuth = ['Authorization':'Basic ' + auth.bytes.encodeBase64().toString()]
        return basicAuth
    }

    public String getFirstnameAndLastnameOrNickname(Person person) {
        String result = person.foreName
        boolean hasLastName = person.surName && !person.surName.equals(UNKNOWN_SURNAME)

        if (person.foreName || hasLastName) {
            if (person.foreName && hasLastName) {
                result = person.foreName + " " + person.surName
            }
            else if (person.foreName) {
                result = person.foreName
            } else {
                result = person.surName
            }
        }
        return result
    }

    /**
     *
     * @param id id of person to enable status
     * @param token to enable status
     * @param reset reset-object
     * @return person as JSON object
     */
    public JSONObject confirm(String id, String token) {
        def apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id + "/confirm/"+ token, true)
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }

    // Utility method to check if attributes are consistent
    public boolean isConsistent(Person person) {
        if (StringUtils.isBlank(person.id) || StringUtils.isBlank(person.nickname) || StringUtils.isBlank(person.email)) {
            return false
        }
        return true
    }

    /**
     * /persons/<id>/organizations 
     * listing of organizations to which person is assigned by any privilege
     * @param id the id of the person
     * @param credentials
     *
     * @return a Organization tree with the children of the parent
     */
    public OrganizationTreeObject getPersonOrganizations(String personId, AasCredential cred) {
        return aasOrganizationService.getOrganizationTreeObject(cred, PERSON_PATH + personId + "/" + ORGANIZATION_PATH)
    }

    /**
     * /persons/<id>/privileges
     * read privileges set of person
     * @param id the id of the person
     * @param credentials
     *
     * @return Privilege List of the person
     */
    public List<Privilege> getPersonPrivileges(String personId, AasCredential cred) {
        ObjectMapper mapper = new ObjectMapper()
        Privilege[] privilegeList

        def optionalHeaders = getTextAndAuthHeader(cred)

        def apiResponse = ApiConsumer.getText(configurationService.getAasUrl(), PERSON_PATH + personId + "/" + PRIVILEGE_PATH , false, [:], optionalHeaders, false, false)
        if(apiResponse.isOk()){
            privilegeList = mapper.readValue(apiResponse.getResponse(), de.ddb.common.beans.aas.Privilege[].class)
        }
        return privilegeList
    }

    /**
     * /authentication/<user>/<passwd>
     * authenticate
     * @param user
     * @param passwd
     */
    public boolean authenticate(String user, String passwd) {
        def isAuthenticated = false
        AasCredential cred = new AasCredential(user, passwd)
        def apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), AUTHENTICATION_PATH ,  false, [:], getUserBasicAuth(cred.getId(), cred.getPassword()), false, false)
        if (apiResponse.isOk()) {
            isAuthenticated = true
        }
        return isAuthenticated
    }

    /**
     *  /persons/<id>/resetpassword
     * resetpassword
     * @param personId the id of the person
     * @param confirmationLink the link to confirm the password reset
     * @param confirmationTemplate the text of the e-mail
     * @param confirmationSubject the subject of the e-mail
     */
    public void resetPassword(String personId, String confirmationLink, String confirmationTemplate, String confirmationSubject) {
        def body = [:]
        body["confirmationLink"] = confirmationLink
        body["confirmationTemplate"] = confirmationTemplate
        body["confirmationSubject"] = confirmationSubject
        JSONObject jo = new JSONObject(body)
        def apiResponse = ApiConsumer.putJson(configurationService.getAasUrl(), PERSON_PATH + "/" + personId + "/" + RESET_PASSWORD ,  false, jo, [:], [:], false, false)
        if(!apiResponse.isOk()){
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }

    /**
     *  /persons/<id>/password
     * resetpassword
     * @param personId
     * @param confirmationLink
     */
    public void changePassword(String personId, String newPswd, AasCredential cred) {
        def body = [:]
        body["pswd"] = newPswd
        JSONObject jo = new JSONObject(body)
        def apiResponse = ApiConsumer.putJson(configurationService.getAasUrl(), PERSON_PATH + "/" + personId + "/" + PASSWORD ,  false, jo, [:], getUserBasicAuth(cred.getId(), cred.getPassword()), false, false)
        if(!apiResponse.isOk()){
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
    }


    /**
     * Check if logged user is global admin
     * @param credentials
     * 
     * @return boolean if is admin or not.
     */
    public boolean isGlobalAdmin(AasCredential cred) {
        boolean isGlobalAdmin = false
        // get privileges from session if available
        List<Privilege> personPrivileges = sessionService.getSessionAttributeIfAvailable(Privilege.SESSION_PRIVILEGES)
        // if not, get from backend
        if (cred != null && personPrivileges == null) {
            personPrivileges = getPersonPrivileges(cred.getId(), cred)
        }
        personPrivileges.each {
            if (it.getPrivilege().compareTo(GLOBAL_ADMIN)==0){
                isGlobalAdmin = true
            }
        }
        return isGlobalAdmin
    }

    /**
     * Check if logged user is org admin
     * @param credentials
     * 
     * @return boolean if is admin or not.
     */
    public boolean isOrgAdmin(AasCredential cred, String organizationId) {
        boolean isOrgAdmin = false
        //        Person person = getPerson(cred)
        List<Privilege> personPrivileges = getPersonPrivileges(cred.getId(), cred)
        personPrivileges.each {
            if (it.getPrivilege().compareTo(ADMIN_ORG)==0){
                //                it.getId().toArray().each {
                for (var in it.getId()) {
                    if (var.compareTo(organizationId)==0){
                        isOrgAdmin = true
                    }
                }
            }
        }
        return isOrgAdmin
    }

    public Object getTextAndAuthHeader(AasCredential cred){
        def optionalHeaders = [:]
        String auth = cred.getId() + ":" + cred.getPassword()
        optionalHeaders["Authorization"] = 'Basic ' + auth.bytes.encodeBase64().toString()
        optionalHeaders["Accept"] = 'application/json'

        return optionalHeaders
    }
}
