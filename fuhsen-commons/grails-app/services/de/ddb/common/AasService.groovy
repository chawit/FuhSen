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
import groovyjarjarcommonscli.MissingArgumentException
import groovyx.net.http.Method

import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.util.WebUtils

import de.ddb.common.ApiConsumer
import de.ddb.common.beans.User
import de.ddb.common.exception.BackendErrorException

/**
 * Set of Methods that encapsulate REST-calls to the AASWebService
 *
 * @author mih
 *
 */
@Deprecated
class AasService {

    def configurationService
    def transactional = false

    private static final log = LogFactory.getLog(this)

    private static final String PERSON_PATH = "persons/"

    private static final String APIKEY_PATH = "keys/"

    private static final String ID_FIELD = "id"

    private static final String NICKNAME_FIELD = "nickname"

    private static final String TITLE_FIELD = "title"

    private static final String SALUTATION_FIELD = "salutation"

    private static final String LASTNAME_FIELD = "surName"

    private static final String FIRSTNAME_FIELD = "foreName"

    private static final String TELEPHONE_FIELD = "telephoneNumber"

    private static final String FAX_FIELD = "faxNumber"

    private static final String EMAIL_FIELD = "email"

    private static final String PASSWORD_FIELD = "pswd"

    private static final String CONFIRMATION_LINK_FIELD = "confirmationLink"

    private static final String CONFIRMATION_TEMPLATE_FIELD = "confirmationTemplate"

    private static final String CONFIRMATION_SUBJECT_FIELD = "confirmationSubject"

    private static final String APIKEY_FIELD = "apiKey"

    /**
     *
     * @param id id of person to login
     * @param password password of person to login
     * @return person as JSON object
     */
    public User login(String id, String password) {
        String auth = id + ":" + password
        def apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:], ['Authorization':'Basic ' + auth.bytes.encodeBase64().toString()])
        if(apiResponse.isOk()){
            def aasResponse = apiResponse.getResponse()

            User user = new User()
            user.setId(aasResponse.id)
            user.setUsername(aasResponse.nickname)
            user.setStatus(aasResponse.status)
            user.setEmail(aasResponse.email)
            user.setFirstname(aasResponse.foreName)
            //workaround for aas default value
            if (aasResponse.surName != null && !aasResponse.surName.equals(User.UNKNOWN_SURNAME)) {
                user.setLastname(aasResponse.surName)
            }
            user.setPassword(password)
            user.setOpenIdUser(false)
            user.setApiKey(aasResponse.apiKey)

            return user
        }
        else {
            return null
        }
    }

    /**
     *
     * @param id id of person to retrieve
     * @return person as JSON object
     */
    public JSONObject getPerson(String id) {
        return request(PERSON_PATH + id)
    }

    /**
     * Get detailed information about a person using an admin account.
     *
     * @param id id of person to retrieve
     * @return person as Json object
     */
    public getPersonJsonAsAdmin(String id) {
        JSONObject result

        def apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:], getAdminBasicAuth())
        if (apiResponse.isOk()) {
            result = apiResponse.getResponse()

        }

        return result
    }

    /**
     * Get detailed information about a person using an admin account.
     *
     * @param id id of person to retrieve
     * @return person as User object
     */
    public User getPersonAsAdmin(String id) {
        User result

        def apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:], getAdminBasicAuth())
        if (apiResponse.isOk()) {
            JSONObject jsonObject = apiResponse.getResponse()
            result = new User(
                    username: jsonObject.nickname,
                    status: jsonObject.status,
                    lastname: jsonObject.surName,
                    firstname: jsonObject.foreName,
                    email: jsonObject.email,
                    apiKey: jsonObject.apiKey)
            result.setId(id)
        }
        else {
            result = new User(username: "username")
            result.setId(id)
        }
        return result
    }

    /**
     *
     * @param id id of person to retrieve
     * @return person as JSON object
     */
    public JSONObject createPerson(JSONObject person) {
        return request(PERSON_PATH, Method.POST, person)
    }

    /**
     * Create or update a person in AAS. This method is used to store OpenId users in AAS.
     *
     * @param user user object
     */
    public User createOrUpdatePersonAsAdmin(User user) {
        User result = user


        JSONObject jsonObject = new JSONObject()
        jsonObject.put(NICKNAME_FIELD, user.username)
        jsonObject.put(LASTNAME_FIELD, user.lastname)
        jsonObject.put(FIRSTNAME_FIELD, user.firstname)
        jsonObject.put(EMAIL_FIELD, user.email)

        User aasUser = getPersonAsAdmin(user.email)
        if (aasUser.email) {
            // user exists - update it
            jsonObject.put(ID_FIELD, aasUser.id)
            def apiResponse = ApiConsumer.putJson(configurationService.getAasUrl(), PERSON_PATH + aasUser.id, false,
                    jsonObject, [:], getAdminBasicAuth())
            if (!apiResponse.isOk()) {
                log.error "AAS update request failed: " + apiResponse
            }
            else {
                result.setId(apiResponse.getResponse().id)
            }
        }
        else {
            // user doesn't exist - create it
            jsonObject.put(PASSWORD_FIELD, RandomStringUtils.randomAlphanumeric(12))
            def apiResponse = ApiConsumer.postJson(configurationService.getAasUrl(), PERSON_PATH, false,
                    jsonObject, [:], getAdminBasicAuth())
            if (!apiResponse.isOk()) {
                log.error "AAS create request failed: " + apiResponse
            }
            else {
                result.setId(apiResponse.getResponse().id)
            }
        }
        return result
    }

    /**
     *
     * @param id id of person to update
     * @param user user-object
     * @return person as JSON object
     */
    public JSONObject updatePerson(String id, JSONObject person) {
        return request(PERSON_PATH + id, Method.PUT, person)
    }

    /**
     *
     * @param id id of person to delete
     */
    public void deletePerson(String id) {
        request(PERSON_PATH + id, Method.DELETE)
    }

    /**
     *
     * @param id id of person to delete
     */
    public void deletePersonAsAdmin(String id) {
        def apiResponse = ApiConsumer.deleteJson(configurationService.getAasUrl(), PERSON_PATH + id, false, [:], getAdminBasicAuth())

        if (!apiResponse.isOk()) {
            log.error "AAS create request failed: " + apiResponse
        }
    }



    /**
     *
     * @param id id of person change password for
     * @param password password-object
     */
    public void changePassword(String id, JSONObject password) {
        request(PERSON_PATH + id + "/password", Method.PUT, password)
    }

    /**
     *
     * @param id id of person to update email for
     * @param update update-object
     * @return person as JSON object
     */
    public JSONObject updateEmail(String id, JSONObject update) {
        return request(PERSON_PATH + id + "/email", Method.PUT, update)
    }

    /**
     *
     * @param id id of person to reset password
     * @param reset reset-object
     * @return person as JSON object
     */
    public JSONObject resetPassword(String id, JSONObject reset) {
        return request(PERSON_PATH + id + "/resetpassword", Method.PUT, reset)
    }

    /**
     *
     * @param id id of person to reset password
     * @param reset reset-object
     * @return person as JSON object
     */
    public JSONObject confirm(String id, String token) {
        return request(PERSON_PATH + id + "/confirm/" + token, Method.GET)
    }

    /**
     * get person JSON-Object
     *
     * @param nickname nickname
     * @param title title
     * @param salutation salutation
     * @param surName surName
     * @param foreName foreName
     * @param telephoneNumber telephoneNumber
     * @param faxNumber faxNumber
     * @param email email
     * @param pswd pswd
     * @param confirmationLink confirmationLink
     * @param confirmationTemplate confirmationTemplate
     * @param confirmationSubject confirmationSubject
     * @return person as JSON object
     */
    public JSONObject getPersonJson(
            String nickname,
            String title,
            String salutation,
            String surName,
            String foreName,
            String telephoneNumber,
            String faxNumber,
            String email,
            String pswd,
            String confirmationLink,
            String confirmationTemplate,
            String confirmationSubject,
            String apiKey) throws MissingArgumentException {

        JSONObject jsonObject = new JSONObject()
        if (!StringUtils.isBlank(nickname)) {
            jsonObject.put(NICKNAME_FIELD, nickname)
        }
        if (!StringUtils.isBlank(title)) {
            jsonObject.put(TITLE_FIELD, title)
        }
        if (!StringUtils.isBlank(salutation)) {
            jsonObject.put(SALUTATION_FIELD, salutation)
        }
        if (!StringUtils.isBlank(surName)) {
            jsonObject.put(LASTNAME_FIELD, surName)
        }
        if (!StringUtils.isBlank(foreName)) {
            jsonObject.put(FIRSTNAME_FIELD, foreName)
        }
        if (!StringUtils.isBlank(telephoneNumber)) {
            jsonObject.put(TELEPHONE_FIELD, telephoneNumber)
        }
        if (!StringUtils.isBlank(faxNumber)) {
            jsonObject.put(FAX_FIELD, faxNumber)
        }
        if (!StringUtils.isBlank(email)) {
            jsonObject.put(EMAIL_FIELD, email)
        }
        if (!StringUtils.isBlank(pswd)) {
            jsonObject.put(PASSWORD_FIELD, pswd)
        }
        if (!StringUtils.isBlank(confirmationLink)) {
            jsonObject.put(CONFIRMATION_LINK_FIELD, confirmationLink)
        }
        if (!StringUtils.isBlank(confirmationTemplate)) {
            jsonObject.put(CONFIRMATION_TEMPLATE_FIELD, confirmationTemplate)
        }
        if (!StringUtils.isBlank(confirmationSubject)) {
            jsonObject.put(CONFIRMATION_SUBJECT_FIELD, confirmationSubject)
        }
        if (!StringUtils.isBlank(apiKey)) {
            jsonObject.put(APIKEY_FIELD, apiKey)
        }
        return jsonObject
    }

    /**
     * get change password JSON-Object
     *
     * @param pswd pswd
     * @return change password as JSON object
     */
    public JSONObject getChangePasswordJson(
            String pswd) throws MissingArgumentException {

        JSONObject jsonObject = new JSONObject()
        if (!StringUtils.isBlank(pswd)) {
            jsonObject.put(PASSWORD_FIELD, pswd)
        }
        else {
            throw new MissingArgumentException(PASSWORD_FIELD + " may not be null")
        }
        return jsonObject
    }

    /**
     * get update email JSON-Object
     *
     * @param email email
     * @param confirmationLink confirmationLink
     * @param confirmationTemplate confirmationTemplate
     * @param confirmationSubject confirmationSubject
     * @return update email as JSON object
     */
    public JSONObject getUpdateEmailJson(
            String email,
            String confirmationLink,
            String confirmationTemplate,
            String confirmationSubject) throws MissingArgumentException {

        JSONObject jsonObject = new JSONObject()
        if (!StringUtils.isBlank(email)) {
            jsonObject.put(EMAIL_FIELD, email)
        }
        else {
            throw new MissingArgumentException(EMAIL_FIELD + " may not be null")
        }
        if (!StringUtils.isBlank(confirmationLink)) {
            jsonObject.put(CONFIRMATION_LINK_FIELD, confirmationLink)
        }
        else {
            throw new MissingArgumentException(CONFIRMATION_LINK_FIELD + " may not be null")
        }
        if (!StringUtils.isBlank(confirmationTemplate)) {
            jsonObject.put(CONFIRMATION_TEMPLATE_FIELD, confirmationTemplate)
        }
        if (!StringUtils.isBlank(confirmationSubject)) {
            jsonObject.put(CONFIRMATION_SUBJECT_FIELD, confirmationSubject)
        }
        return jsonObject
    }

    /**
     * get reset password JSON-Object
     *
     * @param confirmationLink confirmationLink
     * @param confirmationTemplate confirmationTemplate
     * @param confirmationSubject confirmationSubject
     * @return reset password as JSON object
     */
    public JSONObject getResetPasswordJson(
            String confirmationLink,
            String confirmationTemplate,
            String confirmationSubject) throws MissingArgumentException {

        JSONObject jsonObject = new JSONObject()
        if (!StringUtils.isBlank(confirmationLink)) {
            jsonObject.put(CONFIRMATION_LINK_FIELD, confirmationLink)
        }
        else {
            throw new MissingArgumentException(CONFIRMATION_LINK_FIELD + " may not be null")
        }
        if (!StringUtils.isBlank(confirmationTemplate)) {
            jsonObject.put(CONFIRMATION_TEMPLATE_FIELD, confirmationTemplate)
        }
        if (!StringUtils.isBlank(confirmationSubject)) {
            jsonObject.put(CONFIRMATION_SUBJECT_FIELD, confirmationSubject)
        }
        return jsonObject
    }

    /**
     * request AAS via ApiConsumer.
     *
     * @param url url of request
     * @return JSON-response
     */
    private JSONObject request(String path, Method method = Method.GET, JSONObject postParameter = null) {
        def apiResponse
        if (method.equals(Method.GET)) {
            apiResponse = ApiConsumer.getJson(configurationService.getAasUrl(), path, true)
        }
        else if (method.equals(Method.POST)) {
            apiResponse = ApiConsumer.postJson(configurationService.getAasUrl(), path, true, postParameter)
        }
        else if (method.equals(Method.PUT)) {
            apiResponse = ApiConsumer.putJson(configurationService.getAasUrl(), path, true, postParameter)
        }
        else if (method.equals(Method.DELETE)) {
            apiResponse = ApiConsumer.deleteJson(configurationService.getAasUrl(), path, true)
        }
        else {
            throw new BackendErrorException("No method for request defined")
        }
        if(!apiResponse.isOk()){
            log.error method + " request to " + path + " failed with " + apiResponse.status
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        return apiResponse.getResponse()
    }

    /**
     * Creates and returns a new API-Key which is not attached to any user
     *
     * @return The new API-Key as String
     */
    public String createApiKey() {
        JSONObject newApiKey = request(APIKEY_PATH + "generate", Method.GET)
        return newApiKey?.developerKey?.toString()
    }

    private def getAdminBasicAuth(){
        String auth = configurationService.getAasAdminUserId() + ":" + configurationService.getAasAdminPassword()

        def basicAuth = ['Authorization':'Basic ' + auth.bytes.encodeBase64().toString()]

        return basicAuth
    }

}