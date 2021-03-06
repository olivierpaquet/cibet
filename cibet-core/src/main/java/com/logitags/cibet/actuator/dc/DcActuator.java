/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.actuator.dc;

import com.logitags.cibet.actuator.common.Controllable;

public interface DcActuator {

   /**
    * releases a controlled object. If it is the second release of a 6-eyes process the controlled object entry is
    * updated. If it is a 4-eyes process the real object is updates, the controlled object is removed.
    * 
    * @param co
    *           Controllable object
    * @param remark
    *           comment
    * @return return value of method invocation or null for persistence controlled objects
    * @throws ResourceApplyException
    *            if the release action fails.
    */
   Object release(Controllable co, String remark) throws ResourceApplyException;

   /**
    * rejects a controlled object. The controlled object is removed.
    * 
    * @param co
    *           Controllable object
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            exception in case of error
    */
   void reject(Controllable co, String remark) throws ResourceApplyException;

   /**
    * returns the given dual control event back to the event producer. The event producer must correct the Controllable
    * or resource data before it can be released.
    * 
    * @param co
    *           Controllable to pass back
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            exception in case of error
    */
   void passBack(Controllable co, String remark) throws ResourceApplyException;

   /**
    * submits a Controllable for release. Used by the user who created the Controllable and to whom it is passed back.
    * The ExecutionStatus will be set to POSTPONED or FIRST_POSTPONED. Notifications will be sent if configured.
    * 
    * @param co
    *           Controllable to submit
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            exception in case of error
    */
   void submit(Controllable co, String remark) throws ResourceApplyException;

}
