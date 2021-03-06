/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.sensor.http;

public enum Headers {

   /**
    * the session and request context as URL-encoded querystring
    */
   CIBET_CONTEXT,

   CIBET_EVENTRESULT,

   CIBET_PLAYINGMODE,

   CIBET_CONTROLEVENT,

   CIBET_REMARK,

   CIBET_CASEID,

   CIBET_SCHEDULEDDATE,

   CIBET_SCHEDULEDFIELD,

   CIBET_USER,

   CIBET_USERADDRESS,

   CIBET_TENANT,

   /**
    * the security context to transmit to the HTTP proxy sensor. It depends on the AuthenticationProvider implementation
    * what the security context actually is. For Shiro for example it is the session id.
    */
   CIBET_SECURITYCONTEXT,

   ;

}
