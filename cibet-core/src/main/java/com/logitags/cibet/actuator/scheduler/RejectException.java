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
package com.logitags.cibet.actuator.scheduler;

/**
 * thrown when a scheduled task shall not be executed.
 * 
 * @author Wolfgang
 * 
 */
public class RejectException extends Exception {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public RejectException() {
      super();
   }

   public RejectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public RejectException(String message, Throwable cause) {
      super(message, cause);
   }

   public RejectException(String message) {
      super(message);
   }

   public RejectException(Throwable cause) {
      super(cause);
   }

}
