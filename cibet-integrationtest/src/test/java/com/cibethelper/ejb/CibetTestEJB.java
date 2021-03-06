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
package com.cibethelper.ejb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.PostponedEjbException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.actuator.scheduler.SchedulerLoader;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.sensor.ejb.CibetInterceptor;

@Stateless(name = "com.cibethelper.ejb.CibetTestEJB")
@Interceptors(CibetInterceptor.class)
public class CibetTestEJB {

   private static Logger log = Logger.getLogger(CibetTestEJB.class);

   @Resource
   private SessionContext ctx;

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager applEman;

   private String TENANT = "testTenant";

   public CibetTestEJB() {
   }

   @PostConstruct
   public void init() {
      log.debug("init new EJB CibetTestEJBImpl " + this);
   }

   public <T> T persist(T entity) {
      log.debug("persist in CibetTestEJB: " + entity);
      log.debug("User=" + Context.sessionScope().getUser());
      applEman.persist(entity);
      return entity;
   }

   public <T> T merge(T entity) {
      log.debug("merge in CibetTestEJB: " + entity);
      log.debug("User=" + Context.sessionScope().getUser());
      return applEman.merge(entity);
   }

   public void throwException() {
      log.debug("start CibetTestEJBImpl.throwException");
      TEntity te = new TEntity("myname", 456, "oo");
      applEman.persist(te);
      throw new RuntimeException("explicit exception !!");
   }

   public void throwDirectException() throws PostponedEjbException {
      log.debug("start CibetTestEJBImpl.throwDirectException");
      throw new PostponedEjbException();
   }

   public <T> T persistNoCtx(T entity) {
      log.debug("persist in CibetTestEJBImpl: " + entity);
      log.debug("User=" + Context.sessionScope().getUser());
      applEman.persist(entity);
      return entity;
   }

   public void remove(TEntity entity) {
      log.debug("remove in CibetTestEJBImpl: " + entity);
      applEman.remove(entity);
   }

   public TEntity findTEntity(long id) {
      log.debug("find in CibetTestEJB");
      applEman.clear();
      return applEman.find(TEntity.class, id);
   }

   public TEntity findTEntity(long id, LockModeType lockType, Map<String, Object> props) {
      log.debug("find in CibetTestEJB");
      if (lockType == null) {
         if (props == null) {
            return applEman.find(TEntity.class, id);
         } else {
            return applEman.find(TEntity.class, id, props);
         }
      } else {
         if (props == null) {
            return applEman.find(TEntity.class, id, lockType);
         } else {
            return applEman.find(TEntity.class, id, lockType, props);
         }
      }
   }

   public TComplexEntity findTComplexEntity(long id) {
      log.debug("find in CibetTestEJB");
      TComplexEntity e = applEman.find(TComplexEntity.class, id);
      return e;
   }

   public List<Archive> findArchives() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();
      Query qa = em.createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      qa.setParameter("tenant", Context.sessionScope().getTenant());
      List<Archive> list = qa.getResultList();
      return list;
   }

   public Long[] persistWithArchiveRollback(String owner) {
      Long[] res = new Long[2];
      TEntity entity1 = new TEntity();
      entity1.setCounter(25);
      entity1.setNameValue("valuexxcc");
      entity1.setOwner(owner);
      entity1 = (TEntity) persist(entity1);
      res[0] = entity1.getId();

      Setpoint sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT);

      TEntity entity = new TEntity();
      entity.setCounter(5);
      entity.setNameValue("valuexx");
      entity.setOwner(owner);
      entity = (TEntity) persist(entity);
      res[1] = entity.getId();

      Configuration.instance().unregisterControl(sp.getId());

      return res;
   }

   public long persistWithQueryInBetween(String owner, long id1) {
      TEntity entity1 = new TEntity();
      entity1.setCounter(25);
      entity1.setNameValue("valuexxcc");
      entity1.setOwner(owner);
      persist(entity1);
      applEman.flush();

      log.debug("find TEntity with id " + id1);
      TEntity selEnt1 = applEman.find(TEntity.class, id1);
      if (selEnt1 == null || selEnt1.getCounter() != 5) {
         throw new RuntimeException("TEntity == null or counter != 5");
      }

      TEntity selEnt2 = applEman.find(TEntity.class, entity1.getId());
      if (selEnt2 == null || selEnt2.getCounter() != 25) {
         throw new RuntimeException("TEntity2 == null or counter != 25");
      }
      return entity1.getId();
   }

   public long persistWithQueryInBetweenRollback(String owner, long id1) {
      TEntity entity1 = new TEntity();
      entity1.setCounter(25);
      entity1.setNameValue("valuexxcc");
      entity1.setOwner(owner);
      persist(entity1);

      TEntity selEnt1 = applEman.find(TEntity.class, id1);
      if (selEnt1 == null || selEnt1.getCounter() != 5) {
         throw new RuntimeException("TEntity == null or counter != 5");
      }

      ctx.setRollbackOnly();

      return entity1.getId();
   }

   public List<Controllable> findUnreleased() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();

      List<Controllable> l = DcLoader.findUnreleased();
      for (Controllable co : l) {
         co.getResource().getParameters().size();
         co.getResource().getUnencodedTargetObject();
      }

      return l;
   }

   public List<Controllable> findScheduled() {
      List<Controllable> l = SchedulerLoader.findScheduled();
      return l;
   }

   public List<Controllable> findUnreleased(Class<?> cl) {
      List<Controllable> l = DcLoader.findUnreleased(cl.getName());
      return l;
   }

   public Object release() throws ResourceApplyException {
      log.debug(
            "applEman= " + applEman + ", delegate= " + Context.internalRequestScope().getOrCreateEntityManager(true));
      List<Controllable> l = DcLoader.findUnreleased();
      if (l.size() != 1) {
         throw new RuntimeException("list size is not 1: " + l.size());
      }
      Controllable co = l.get(0);
      return co.release(applEman, null);
   }

   public void release(Controllable co) throws ResourceApplyException {
      co.release(applEman, null);
   }

   public Object release(Controllable co, String remark) throws ResourceApplyException {
      Object obj = co.release(remark);
      return obj;
   }

   public void reject(Controllable co, String remark) throws ResourceApplyException {
      co.reject(remark);
   }

   public Object playRelease(Controllable co, String remark) throws ResourceApplyException {
      Context.requestScope().startPlay();
      Object res = co.release(remark);
      Context.requestScope().stopPlay();
      return res;
   }

   public List<Object> testInvoke(String str1, int int1, int int2, byte[] bytes1, TEntity entity, Long long1) {
      log.info("start CibetTestEJBImpl.testInvoke");
      List<Object> list = new ArrayList<Object>();
      list.add(str1);
      list.add(int1);
      list.add(int2);
      list.add(bytes1);
      list.add(entity);
      list.add(long1);
      return list;
   }

   public List<Object> testInvoke2(String str1) {
      throw new RuntimeException("This is a artificial Exception invoked");
   }

   public String longCalculation(int loops) {
      log.info("start longCalculation");
      long start = System.currentTimeMillis();
      Random rnd = new Random(start);
      for (int i = 1; i < loops; i++) {
         double d = rnd.nextDouble();
         Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(d)))))))));
      }

      long end = System.currentTimeMillis();
      long duration = end - start;
      log.info("duration: " + duration);
      return "DURATIONRESULT=" + String.valueOf(duration);
   }

   public int executeNativeQuery(String qn, Object... objects) {
      Query q = applEman.createNativeQuery(qn);
      int i = 1;
      for (Object ob : objects) {
         q.setParameter(i, ob);
         i++;
      }

      int result = q.executeUpdate();
      return result;
   }

   public void patchEjbInvokerJndiPropertiesFilename(String name) throws Exception {
      Field f = EjbLookup.class.getDeclaredField("JNDI_PROPERTIES_FILENAME");
      f.setAccessible(true);
      f.set(null, name);
   }

   public List<Object> redo(Archive mar) throws Exception {
      return (List) mar.redo("gutes Schweinchen");
   }

   public Object querySingleResult(String query) {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();

      if ("SELECT a FROM Controllable a".equals(query)) {
         Query q = em.createNamedQuery(Controllable.SEL_BY_TENANT);
         q.setParameter("tenant", Context.sessionScope().getTenant());
         return q.getSingleResult();
      } else {
         Query q = em.createQuery(query);
         return q.getSingleResult();
      }
   }

   public List<?> queryResultSet(String query) {
      applEman.clear();
      Query q = applEman.createQuery(query);
      return q.getResultList();
   }

   public List<Archive> queryArchive(String tenant, String targetType, String objectId) {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();
      // TypedQuery<Archive> q = em.createNamedQuery(Archive.SEL_BY_PRIMARYKEYID, Archive.class);
      // q.setParameter(1, targetType);
      // q.setParameter(2, objectId);
      // return q.getResultList();
      return ArchiveLoader.loadArchivesByPrimaryKeyId(targetType, objectId);
   }

   public List<Archive> loadArchives(String targetType) {
      List<Archive> l = ArchiveLoader.loadArchives(targetType);
      for (Archive ar : l) {
         ar.getResource().getParameters().size();
      }

      return l;
   }

   public List<Archive> queryArchiveByTenant() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();
      Query q = em.createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      List<Archive> list = q.getResultList();

      // make distinct
      Set<Archive> s = new LinkedHashSet<>(list);
      List<Archive> list2 = new ArrayList<>(s);
      return list2;
   }

   public List<Archive> queryArchives() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();
      Query q = em.createNamedQuery(Archive.SEL_ALL);
      return q.getResultList();
   }

   public List<Controllable> queryControllable() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      em.clear();
      Query q = em.createNamedQuery(Controllable.SEL_BY_TENANT);
      q.setParameter("tenant", Context.sessionScope().getTenant());
      return q.getResultList();
   }

   public Controllable lockMethodFromClass() throws AlreadyLockedException, SecurityException, NoSuchMethodException {
      Method m = CibetTestEJB.class.getMethod("testInvoke", String.class, int.class, int.class, byte[].class,
            TEntity.class, Long.class);
      log.debug("method='" + m.toString() + "'");
      Controllable lo = Locker.lock(CibetTestEJB.class, "testInvoke", ControlEvent.INVOKE, "testremark");
      return lo;
   }

   public List<EventResult> insertTComplexEntity() {
      log.debug("EVRESZLT: " + Context.requestScope().getExecutedEventResult());

      List<EventResult> list = new ArrayList<EventResult>();
      list.add(Context.requestScope().getExecutedEventResult());
      TEntity t1 = new TEntity("nam1", 45, "own1");
      TEntity t2 = new TEntity("nam2", 45, "own2");
      TComplexEntity c = new TComplexEntity();
      c.setOwner("myOwner");
      EventResult e1 = Context.requestScope().getExecutedEventResult();
      list.add(e1);
      log.debug("added ER: " + e1);

      c.addLazyList(t1);
      EventResult e2 = Context.requestScope().getExecutedEventResult();
      list.add(e2);
      log.debug("added ER: " + e2);

      c.setTen(t2);
      EventResult e3 = Context.requestScope().getExecutedEventResult();
      list.add(e3);
      log.debug("added ER: " + e3);

      c.setStatValue(88);
      EventResult e4 = Context.requestScope().getExecutedEventResult();
      list.add(e4);
      log.debug("added ER: " + e4);

      // log.debug("persist in CibetTestEJB: " + c);
      applEman.persist(c);
      EventResult e5 = Context.requestScope().getExecutedEventResult();
      list.add(e5);
      log.debug("added ER: " + e5);

      long id = c.getId();
      EventResult e6 = Context.requestScope().getExecutedEventResult();
      list.add(e6);
      log.debug("added ER: " + e6);
      return list;
   }

   public List<EventResult> selectEventResults() {
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
      Query q = em.createQuery("SELECT a FROM EventResult a WHERE a.parentResult IS NULL");
      return q.getResultList();
   }

   public List<?> executeQuery(String query) {
      applEman.clear();
      Query qa = applEman.createQuery(query);
      List<?> list = qa.getResultList();
      return list;
   }

   protected Setpoint registerSetpoint(String clazz, String act, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()));
      sp.addTargetIncludes(clazz);
      sp.addEventIncludes(events);
      Configuration cman = Configuration.instance();
      sp.addActuator(cman.getActuator(act));
      cman.registerSetpoint(sp);
      return sp;
   }

}
