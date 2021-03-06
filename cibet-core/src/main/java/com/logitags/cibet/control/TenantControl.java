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
/**
 * 
 */
package com.logitags.cibet.control;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates the tenant from the context against configured setpoints. Considers tenant hierarchies. In tenant
 * hierarchies the tenant ids are separated by a minus. All setpoints of the tenant are returned. If no setpoints exist,
 * it is checked if setpoints for the parents up to the default tenant exist.
 */
public class TenantControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = -7285664451095511751L;

   private static Log log = LogFactory.getLog(TenantControl.class);

   public static final String NAME = "tenant";

   private static final String TENANT_SEPARATOR = "|";

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute tenant evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (values == null || values.isEmpty()) return null;

      String tenant = Context.internalSessionScope().getTenant();
      do {
         for (String spTenant : values) {
            if (spTenant.equals(tenant)) {
               return true;
            }
         }

         int start = tenant.lastIndexOf(TENANT_SEPARATOR);
         if (start <= 0) {
            return false;
         } else {
            // ( "1000|27|215" -> "1000|27" )
            tenant = tenant.substring(0, start);
         }
      } while (true);
   }

}
