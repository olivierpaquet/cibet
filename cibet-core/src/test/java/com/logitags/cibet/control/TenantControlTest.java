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
package com.logitags.cibet.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;

public class TenantControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(TenantControlTest.class);

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new TenantControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("tenant");
         if (value == null) {
            list.add(spi);
         } else {
            boolean okay = eval.evaluate(value, md);
            if (okay) {
               list.add(spi);
            }
         }
      }
      return list;
   }

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");
      initConfiguration("config_tenant_event_target.xml");

      List<Setpoint> spB = Configuration.instance().getSetpoints();
      log.debug("setpoints size: " + spB.size());

      EventMetadata md = new EventMetadata(ControlEvent.ALL, null);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());

      Context.sessionScope().setTenant("not|present");
      list = evaluate(md, spB);
      Assert.assertTrue(list.size() == 1);
      Assert.assertEquals("C", list.get(0).getId());

      Context.sessionScope().setTenant("ten1");
      list = evaluate(md, spB);
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());

      Context.sessionScope().setTenant("ten1|x");
      list = evaluate(md, spB);
      Assert.assertEquals(5, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("B1", list.get(1).getId());
      Assert.assertEquals("B2", list.get(2).getId());
      Assert.assertEquals("B3", list.get(3).getId());
      Assert.assertEquals("C", list.get(4).getId());

      // for (Setpoint sB : list) {
      // Assert.assertTrue(sB.getId().startsWith("B"));
      // }

      Context.sessionScope().setTenant("ten1|y");
      list = evaluate(md, spB);
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());

      Context.sessionScope().setTenant("ten1|y|z");
      list = evaluate(md, spB);
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

}
