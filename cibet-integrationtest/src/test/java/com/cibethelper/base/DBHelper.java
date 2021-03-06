/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.cibethelper.base;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.history.History;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.resource.Resource;

public class DBHelper extends CoreTestBase {

   private static Logger log = Logger.getLogger(DBHelper.class);

   public static EntityManager applEman;
   public static EntityManagerFactory fac;

   @BeforeClass
   public static void beforeClass() throws Exception {
      try {
         log.debug("DBHelper beforeClass");
         fac = Persistence.createEntityManagerFactory("localTest");
         log.debug("now create EntityManager");
         applEman = fac.createEntityManager();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }
      log.debug("end beforeClass");
   }

   @Before
   public void doBefore() throws Exception {
      log.debug("DBHelper.doBefore");
      Context.start();
      applEman.getTransaction().begin();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);

      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      HTTPURL = properties.getProperty("http.url");
      HTTPSURL = properties.getProperty("https.url");
      if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("openejb")) {
         APPSERVER = TOMEE;
      } else if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("jboss")) {
         APPSERVER = JBOSS;
      } else if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("sun.enterprise")) {
         APPSERVER = GLASSFISH;
      }
   }

   @After
   public void doAfter() throws Exception {
      log.debug("DBHelper:doAfter()");
      if (fac == null) {
         beforeClass();
      }

      if (applEman.getTransaction().isActive()) {
         applEman.getTransaction().rollback();
         // applEman.getTransaction().commit();
      }

      boolean delete = true;
      if (delete) {

         Context.internalRequestScope().getOrCreateEntityManager(false).clear();
         applEman.clear();
         applEman.getTransaction().begin();

         Context.internalRequestScope().getOrCreateEntityManager(false).flush();
         Query q = applEman.createNamedQuery(TComplexEntity.SEL_ALL);
         List<TComplexEntity> l = q.getResultList();
         for (TComplexEntity tComplexEntity : l) {
            TComplexEntity t2 = applEman.merge(tComplexEntity);
            // applEman.remove(tComplexEntity);
            applEman.remove(t2);
         }

         Query q1 = applEman.createNamedQuery(TComplexEntity2.SEL_ALL);
         List<TComplexEntity2> l1 = q1.getResultList();
         for (TComplexEntity2 tComplexEntity : l1) {
            applEman.remove(tComplexEntity);
         }

         Query q2 = applEman.createNamedQuery(TEntity.DEL_ALL);
         q2.executeUpdate();

         Query q3 = Context.internalRequestScope().getOrCreateEntityManager(false).createNamedQuery(Archive.SEL_ALL);
         List<Archive> alist = q3.getResultList();
         for (Archive ar : alist) {
            Context.internalRequestScope().getOrCreateEntityManager(true).remove(ar);
         }

         Query q4 = Context.internalRequestScope().getOrCreateEntityManager(true)
               .createQuery("select d from Controllable d");
         List<Controllable> dclist = q4.getResultList();
         for (Controllable dc : dclist) {
            Context.internalRequestScope().getOrCreateEntityManager(true).remove(dc);
         }

         Query q5 = Context.internalRequestScope().getOrCreateEntityManager(true)
               .createQuery("select d from History d");
         List<History> hlist = q5.getResultList();
         for (History h : hlist) {
            Context.internalRequestScope().getOrCreateEntityManager(true).remove(h);
         }

         Query q6 = Context.internalRequestScope().getOrCreateEntityManager(true)
               .createQuery("SELECT a FROM EventResult a");
         Iterator<EventResult> itEV = q6.getResultList().iterator();
         while (itEV.hasNext()) {
            Context.internalRequestScope().getOrCreateEntityManager(true).remove(itEV.next());
         }

         Query q8 = Context.internalRequestScope().getOrCreateEntityManager(true)
               .createQuery("SELECT a FROM Resource a");
         List<Resource> ll = q8.getResultList();
         log.debug("size::" + ll.size());

         Iterator<Resource> itR2 = ll.iterator();
         while (itR2.hasNext()) {
            Context.internalRequestScope().getOrCreateEntityManager(true).remove(itR2.next());
         }

         applEman.getTransaction().commit();
         // applEman.getTransaction().rollback();
      }
      Context.end();
   }

   protected void resetContext() {
      applEman.getTransaction().commit();
      Context.end();
      Context.start();
      applEman.getTransaction().begin();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);

   }

   protected TEntity persistTEntity() {
      TEntity te = createTEntity(5, "valuexx");
      try {
         applEman.persist(te);
      } catch (PostponedException e) {
      }
      applEman.flush();
      applEman.clear();
      return te;
   }

   public <T> void persist(T entity) {
      applEman.getTransaction().begin();
      applEman.persist(entity);
      applEman.getTransaction().commit();
      applEman.clear();
   }

   protected TEntity persistTEntityWithoutClear() {
      TEntity te = createTEntity(5, "valuexx");
      try {
         applEman.persist(te);
      } catch (PostponedException e) {
      }
      applEman.flush();
      return te;
   }

   public TEntity persistTEntity(int counter, Date now, Calendar cal) {
      TEntity entity = new TEntity();
      entity.setCounter(counter);
      entity.setNameValue("valuexx");
      entity.setOwner(TENANT);
      entity.setXdate(now);
      entity.setXCaldate(cal);
      entity.setXCaltimestamp(cal);
      entity.setXtime(now);
      entity.setXtimestamp(now);
      try {
         applEman.persist(entity);
      } catch (PostponedException e) {
      }
      log.debug("after persist");
      applEman.flush();
      applEman.clear();
      log.debug("after persist");
      return entity;
   }

   public List<?> select(String query) {
      applEman.clear();
      Query q = applEman.createQuery(query);
      return q.getResultList();
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
         }
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   protected void authenticateSecond(String... roles) {
      log.debug("authenticate second");
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if ("NULL".equals(role)) {
            log.debug("set secondRole = null");
            Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
         } else {
            if (!role.startsWith("ROLE_")) {
               role = "ROLE_" + role;
            }
            authManager.addAuthority(role);
         }
      }

      try {
         Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
         Authentication result = authManager.authenticate(request);
         Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage());
      }
   }

}
