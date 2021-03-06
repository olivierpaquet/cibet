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
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTest2EJB;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.CibetTestStatefulEJB;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.PostponedEjbException;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.UnapprovedResourceException;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.security.DefaultSecurityProvider;
import com.logitags.cibet.sensor.ejb.EjbResource;

@RunWith(Arquillian.class)
public class CibetInterceptorIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(CibetInterceptorIT.class);

   protected static final String SEL_CONTROLLABLE = "SELECT c FROM Controllable c WHERE c.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED";

   private static final String JNDINAME = "java:module/" + CibetTestEJB.class.getSimpleName();

   @EJB
   private CibetTestEJB ejb;

   @EJB
   private CibetTest2EJB ejbS;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = CibetInterceptorIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class, CibetTestStatefulEJB.class, CibetTest2EJB.class, ArquillianTestServlet1.class,
            RemoteEJB.class, RemoteEJBImpl.class, SecuredRemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeCibetInterceptorIT() {
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      cman = Configuration.instance();

      DefaultSecurityProvider sprov = new DefaultSecurityProvider();
      sprov.getSecrets().put("checkIntegrityMethodArchive", "2366Au37nBB.0ya?");
      sprov.setCurrentSecretKey("checkIntegrityMethodArchive");
      cman.registerSecurityProvider(sprov);
   }

   @After
   public void afterCibetInterceptorIT() {
      Context.end();
      new ConfigurationService().initialise();
   }

   @Test
   public void interceptReleaseWithInfolog() throws Exception {
      log.info("start interceptReleaseWithInfolog()");

      ArchiveActuator archa = (ArchiveActuator) cman.getActuator(ArchiveActuator.DEFAULTNAME);
      archa.setIntegrityCheck(false);

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      // invoke 4-eyes
      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      Assert.assertEquals(CibetTestEJB.class.getName(), ar.getResource().getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals("testInvoke", ((EjbResource) ar.getResource()).getMethod());
      Assert.assertEquals(6, ar.getResource().getParameters().size());

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
      Context.sessionScope().setUser("test2");
      List<?> res = (List<?>) ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);
      Assert.assertNotNull(res);
      Assert.assertEquals(6, res.size());

      // check
      List<Controllable> list2 = DcLoader.findAllUnreleased();
      Assert.assertEquals(0, list2.size());

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list1.size());
      Archive arch = list1.get(1);
      log.debug("releaseresult: " + ar.getResource().getResult());
      Assert.assertTrue("Happy New Year".equals(arch.getRemark()));
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, arch.getControlEvent());
   }

   @Test
   public void interceptReleaseWithJNDINameWithInfolog() throws Exception {
      log.info("start interceptReleaseWithJNDINameWithInfolog()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(InfoLogActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", JNDINAME, ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      EjbResource res = (EjbResource) list1.get(0).getResource();
      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertTrue("testInvoke".equals(res.getMethod()));
      Assert.assertTrue(res.getParameters().size() == 6);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
      Context.sessionScope().setUser("test2");
      Object result = ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);
      Assert.assertTrue(result instanceof List);
      Assert.assertEquals(6, ((List<?>) result).size());

      // check
      List<Controllable> list2 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(0, list2.size());

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list1.size());
      Archive arch = list1.get(1);
      log.debug("releaseresult: " + arch.getResource().getResult());
      Assert.assertTrue("Happy New Year".equals(arch.getRemark()));
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, arch.getControlEvent());
   }

   @Test
   public void interceptReleaseWithJNDINameError() throws Exception {
      log.info("start interceptReleaseWithJNDINameError()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(InfoLogActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", "ERROR-" + JNDINAME, ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // release
      EjbLookup.clearCache();

      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
      Context.sessionScope().setUser("test2");
      ejb.release(l.get(0), "Happy New Year");
   }

   @Test
   public void interceptPersistArchiveWithException() throws Exception {
      log.info("start interceptPersistArchiveWithException()");

      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "throwException", ControlEvent.INVOKE);

      try {
         ejb.throwException();
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage(), e);
      }

      Query q = applEman.createNativeQuery("SELECT * FROM CIB_TESTENTITY");
      List<TEntity> l = q.getResultList();
      Assert.assertEquals(0, l.size());
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void interceptPersistArchive() throws Exception {
      log.info("start interceptPersistArchive()");

      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "persist", ControlEvent.INVOKE);

      TEntity entity = createTEntity(5, "dassel");
      entity = ejb.persist(entity);
      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertTrue(list.get(0).toString().length() > 0);

      EjbResource res = (EjbResource) list.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list.get(0).getControlEvent());
      Assert.assertTrue("persist".equals(res.getMethod()));
      Assert.assertTrue(res.getParameters().size() == 1);
      ResourceParameter param = res.getParameters().iterator().next();
      Assert.assertTrue("java.lang.Object".equals(param.getClassname()));
      Assert.assertNotNull(res.getResult());
      TEntity ent = (TEntity) CibetUtil.decode(res.getResult());
      Assert.assertEquals(entity.getCounter(), ent.getCounter());
      Assert.assertEquals(entity.getId(), ent.getId());
   }

   @Test
   public void interceptPersist4Eyes() throws Exception {
      log.info("start interceptPersist4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "persist", ControlEvent.INVOKE);

      TEntity entity = createTEntity(6, "Kiste");
      entity = ejb.persist(entity);

      List<?> list3 = ejb.queryResultSet("Select a from TEntity a");
      Assert.assertEquals(0, list3.size());

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      EjbResource res = (EjbResource) list.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list.get(0).getControlEvent());
      Assert.assertTrue("persist".equals(res.getMethod()));
      Assert.assertEquals(1, res.getParameters().size());
      ResourceParameter param = res.getParameters().iterator().next();
      Assert.assertEquals("java.lang.Object", param.getClassname());

      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(1, list2.size());
      res = (EjbResource) list2.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list2.get(0).getControlEvent());
      Assert.assertEquals("persist", res.getMethod());
      Assert.assertEquals(1, res.getParameters().size());
      ResourceParameter mexParam = res.getParameters().iterator().next();
      Assert.assertEquals("java.lang.Object", mexParam.getClassname());
   }

   @Test
   public void interceptComplexSignatureArchive() throws Exception {
      log.info("start interceptComplexSignatureArchive()");

      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "testInvoke", ControlEvent.INVOKE);

      TEntity entity = createTEntity(88, "kkkas");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertEquals(6, list.size());
      Assert.assertEquals("Hals", list.get(0));
      Assert.assertEquals(-34, list.get(1));
      Assert.assertEquals(456, list.get(2));
      byte[] bytesList = (byte[]) list.get(3);
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], bytesList[i]);
      }
      TEntity entList = (TEntity) list.get(4);
      Assert.assertEquals("kkkas", entList.getNameValue());
      Assert.assertEquals(TENANT, entList.getOwner());
      Assert.assertEquals(new Long(43), ((Long) list.get(5)));

      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list1.get(0).getControlEvent());
      Assert.assertEquals("testInvoke", res.getMethod());
      Assert.assertEquals(6, res.getParameters().size());

      List<ResourceParameter> l2 = new ArrayList<ResourceParameter>(res.getParameters());
      Collections.sort(l2, new ParameterSequenceComparator());

      Iterator<ResourceParameter> iter = res.getParameters().iterator();
      ResourceParameter param = iter.next();
      Assert.assertEquals("java.lang.String", param.getClassname());
      Assert.assertEquals("Hals", param.getUnencodedValue());

      param = iter.next();
      Assert.assertEquals("int", param.getClassname());
      int v2 = (Integer) param.getUnencodedValue();
      Assert.assertEquals(-34, v2);

      param = iter.next();
      Assert.assertEquals("int", param.getClassname());
      Assert.assertEquals(456, param.getUnencodedValue());

      param = iter.next();
      Assert.assertEquals("[B", param.getClassname());
      byte[] v4 = (byte[]) param.getUnencodedValue();
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], v4[i]);
      }

      param = iter.next();
      Assert.assertEquals(TEntity.class.getName(), param.getClassname());
      TEntity v5 = (TEntity) param.getUnencodedValue();
      Assert.assertEquals("kkkas", v5.getNameValue());
      Assert.assertEquals(TENANT, v5.getOwner());

      param = iter.next();
      Assert.assertEquals("java.lang.Long", param.getClassname());
      Long v6 = (Long) param.getUnencodedValue();
      Assert.assertEquals(43, v6.longValue());

      List<?> li = (List<?>) CibetUtil.decode(res.getResult());

      Assert.assertEquals(6, li.size());
      Assert.assertEquals("Hals", li.get(0));
      Assert.assertTrue((Integer) li.get(1) == -34);
      Assert.assertTrue((Integer) li.get(2) == 456);
      v4 = (byte[]) li.get(3);
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], v4[i]);
      }
      v5 = (TEntity) li.get(4);
      Assert.assertEquals("kkkas", v5.getNameValue());
      Assert.assertEquals(TENANT, v5.getOwner());
      v6 = (Long) li.get(5);
      Assert.assertEquals(43, v6.longValue());
   }

   @Test
   public void interceptComplexSignatureArchiveWNull() throws Exception {
      log.info("start interceptComplexSignatureArchiveWNull()");

      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "testInvoke", ControlEvent.INVOKE);

      List<Object> list = ejb.testInvoke(null, -34, 456, null, null, null);
      Assert.assertEquals(6, list.size());
      Assert.assertNull(list.get(0));
      Assert.assertTrue((Integer) list.get(1) == -34);
      Assert.assertTrue((Integer) list.get(2) == 456);
      Assert.assertNull(list.get(3));
      Assert.assertNull(list.get(4));
      Assert.assertNull(list.get(5));

      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list1.get(0).getControlEvent());
      Assert.assertEquals("testInvoke", res.getMethod());
      Assert.assertEquals(6, res.getParameters().size());

      Iterator<ResourceParameter> iter = res.getParameters().iterator();
      ResourceParameter param = iter.next();
      Assert.assertEquals("java.lang.String", param.getClassname());
      Assert.assertNull(param.getUnencodedValue());

      param = iter.next();
      Assert.assertEquals("int", param.getClassname());
      int v2 = (Integer) param.getUnencodedValue();
      Assert.assertEquals(-34, v2);

      param = iter.next();
      Assert.assertEquals("int", param.getClassname());
      int v3 = (Integer) param.getUnencodedValue();
      Assert.assertEquals(456, v3);

      param = iter.next();
      Assert.assertEquals("[B", param.getClassname());
      Assert.assertNull(param.getUnencodedValue());

      param = iter.next();
      Assert.assertEquals(TEntity.class.getName(), param.getClassname());
      Assert.assertNull(param.getUnencodedValue());

      param = iter.next();
      Assert.assertEquals("java.lang.Long", param.getClassname());
      Assert.assertNull(param.getUnencodedValue());
   }

   @Test
   public void interceptRelease() throws Exception {
      log.info("start interceptRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE);

      TEntity entity = createTEntity(76, "Mausi");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list1.get(0).getControlEvent());
      Assert.assertEquals("testInvoke", res.getMethod());
      Assert.assertEquals(6, res.getParameters().size());

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);

      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(0, list2.size());

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list1.size());
      Assert.assertEquals("Happy New Year", list1.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, list1.get(1).getControlEvent());

      log.debug("++*****");
      List<Archive> alist = ArchiveLoader.loadArchivesByMethodName(CibetTestEJB.class, "testInvoke");
      Assert.assertEquals(2, alist.size());
      log.debug(alist.get(0));
   }

   @Test
   public void interceptReleaseWithJNDIName() throws Exception {
      log.info("start test interceptReleaseWithJNDIName()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", JNDINAME, ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      TEntity entity = createTEntity(56, "Kassenmann");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertTrue("testInvoke".equals(res.getMethod()));
      Assert.assertTrue(res.getParameters().size() == 6);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
      Context.sessionScope().setUser("test2");
      Object result = ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);
      Assert.assertTrue(result instanceof List);
      Assert.assertEquals(6, ((List<?>) result).size());

      // check
      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(0, list2.size());

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list1.size());
      Archive arch = list1.get(1);
      res = (EjbResource) arch.getResource();

      log.debug("releaseresult: " + res.getResult());
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, arch.getControlEvent());
      Assert.assertTrue("Happy New Year".equals(arch.getRemark()));
   }

   @Test
   public void statefulEJB() throws Exception {
      log.info("start test statefulEJB()");

      registerSetpoint(CibetTestStatefulEJB.class, FourEyesActuator.DEFAULTNAME, "testInvoke2", ControlEvent.INVOKE);

      List<Object> list = ejbS.testInvoke2("Hals");
      Assert.assertNull(list);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      try {
         ejb.release(l.get(0), "Happy New Year");
         Assert.fail();
      } catch (ResourceApplyException e) {
      }
   }

   @Test
   public void interceptReleaseWithException() throws Exception {
      log.info("start test interceptReleaseWithException()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke2", ControlEvent.INVOKE);

      List<Object> list = ejb.testInvoke2("Häls");
      Assert.assertNull(list);

      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list1.get(0).getControlEvent());
      Assert.assertEquals("testInvoke2", res.getMethod());
      Assert.assertEquals(1, res.getParameters().size());

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      try {
         ejb.release(l.get(0), "Happy New Year");
         Assert.fail();
      } catch (ResourceApplyException e) {
      }
   }

   @Test
   public void redoArchive() throws Exception {
      log.info("start redoArchive()");
      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "testInvoke", ControlEvent.INVOKE,
            ControlEvent.REDO);

      TEntity entity = createTEntity(56, "gg");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Häls", -34, 456, bytes, entity, new Long(43));
      Assert.assertEquals(6, list.size());
      Assert.assertEquals("Häls", list.get(0));
      Assert.assertEquals(-34, list.get(1));
      Assert.assertEquals(456, list.get(2));
      byte[] bytesList = (byte[]) list.get(3);
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], bytesList[i]);
      }
      TEntity entList = (TEntity) list.get(4);
      Assert.assertEquals("gg", entList.getNameValue());
      Assert.assertTrue(((Long) list.get(5)).longValue() == new Long(43));

      List<Archive> arlist = ejb.loadArchives(CibetTestEJB.class.getName());
      Assert.assertEquals(1, arlist.size());
      log.debug("parameter size: " + arlist.get(0).getResource().getParameters().size());

      List<Object> list2 = ejb.redo(arlist.get(0));
      Assert.assertEquals(6, list2.size());
      Assert.assertEquals("Häls", list2.get(0));
      Assert.assertEquals(-34, list2.get(1));
      Assert.assertEquals(456, list2.get(2));
      bytesList = (byte[]) list2.get(3);
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], bytesList[i]);
      }
      entList = (TEntity) list2.get(4);
      Assert.assertEquals("gg", entList.getNameValue());
      Assert.assertTrue(((Long) list2.get(5)).longValue() == new Long(43));

      arlist = ArchiveLoader.loadArchives(CibetTestEJB.class.getName());
      Assert.assertEquals(2, arlist.size());
      Archive mar = arlist.get(0);
      Archive mar1 = arlist.get(1);

      Assert.assertEquals(mar.getCaseId(), mar1.getCaseId());
      Assert.assertEquals(ControlEvent.INVOKE, mar.getControlEvent());
      Assert.assertEquals(ControlEvent.REDO, mar1.getControlEvent());
      Assert.assertEquals("gutes Schweinchen", mar1.getRemark());
   }

   @Test
   public void redo4Eyes() throws Exception {
      log.info("start redo4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.REDO,
            ControlEvent.RELEASE);

      TEntity entity = createTEntity(23, "Hase");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Löse", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // check archive
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      EjbResource res = (EjbResource) list1.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals("testInvoke", res.getMethod());
      Assert.assertEquals(6, res.getParameters().size());

      // redo before release
      List<Archive> arlist = ejb.loadArchives(CibetTestEJB.class.getName());
      Assert.assertEquals(1, arlist.size());

      try {
         ejb.redo(arlist.get(0));
         Assert.fail();
      } catch (EJBException e) {
         Assert.assertEquals(UnapprovedResourceException.class, e.getCause().getClass());
      }

      arlist = ArchiveLoader.loadArchives(CibetTestEJB.class.getName());
      Assert.assertEquals(1, arlist.size());

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      List rlist = (List) ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);

      // check
      Assert.assertEquals(6, rlist.size());
      Assert.assertEquals("Löse", rlist.get(0));
      Assert.assertEquals(-34, rlist.get(1));
      Assert.assertEquals(456, rlist.get(2));
      byte[] bytesList = (byte[]) rlist.get(3);
      for (int i = 0; i < bytes.length; i++) {
         Assert.assertEquals(bytes[i], bytesList[i]);
      }
      TEntity entList = (TEntity) rlist.get(4);
      Assert.assertEquals("Hase", entList.getNameValue());
      Assert.assertTrue(((Long) rlist.get(5)).longValue() == new Long(43));

      arlist = ArchiveLoader.loadArchives(CibetTestEJB.class.getName());
      Assert.assertEquals(2, arlist.size());
      Archive mar1 = arlist.get(0);
      Archive mar2 = arlist.get(1);

      Assert.assertEquals(mar1.getCaseId(), mar2.getCaseId());
      Assert.assertEquals(ControlEvent.INVOKE, mar1.getControlEvent());
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, mar2.getControlEvent());
      Assert.assertEquals("Happy New Year", mar2.getRemark());
   }

   @Test
   public void intercept4EyesWithException() throws Exception {
      log.info("start intercept4EyesWithException()");
      registerSetpoint(CibetTestEJB.class, FourEyesActuator.DEFAULTNAME, "testInvoke", ControlEvent.INVOKE);
      ((FourEyesActuator) cman.getActuator(FourEyesActuator.DEFAULTNAME)).setThrowPostponedException(true);

      TEntity entity = createTEntity(78, "Katrin");
      byte[] bytes = "Pausenclown".getBytes();
      try {
         ejb.testInvoke("Höls", -34, 456, bytes, entity, new Long(43));
         Assert.fail();
      } catch (PostponedException e) {
         log.warn(e.getMessage());
      }

      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void playInterceptReleaseWithInfolog() throws Exception {
      log.info("start playInterceptReleaseWithInfolog()");
      log.debug("Managed: " + Context.internalRequestScope().isManaged());

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      Context.requestScope().startPlay();

      // play: invoke 4-eyes
      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Häls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);
      EventResult er = Context.requestScope().stopPlay();
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(0, list1.size());

      // invoke 4-eyes
      list = ejb.testInvoke("Häls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      Assert.assertEquals(CibetTestEJB.class.getName(), ar.getResource().getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals("testInvoke", ((EjbResource) ar.getResource()).getMethod());
      Assert.assertEquals(6, ar.getResource().getParameters().size());

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());
      Context.sessionScope().setUser("test2");
      Context.requestScope().startPlay();
      List<?> res = (List<?>) ejb.release(l.get(0), "Happy New Year");
      Context.sessionScope().setUser(USER);
      er = Context.requestScope().stopPlay();
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      Assert.assertNull(res);

      // check
      List<Controllable> list2 = DcLoader.findAllUnreleased();
      Assert.assertEquals(1, list2.size());

      list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list1.size());
   }

   @Test
   public void interceptPersistWithException() throws Exception {
      log.info("start interceptPersistWithException()");

      registerSetpoint(CibetTestEJB.class, ArchiveActuator.DEFAULTNAME, "throwDirectException", ControlEvent.INVOKE);

      try {
         ejb.throwDirectException();
         Assert.fail();
      } catch (PostponedEjbException e) {
         log.debug(e.getMessage());
      }

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertTrue(list.get(0).toString().length() > 0);

      EjbResource res = (EjbResource) list.get(0).getResource();

      Assert.assertEquals(CibetTestEJB.class.getName(), res.getTarget());
      Assert.assertEquals(ControlEvent.INVOKE, list.get(0).getControlEvent());
      Assert.assertTrue("throwDirectException".equals(res.getMethod()));
      Assert.assertTrue(res.getParameters().size() == 0);
      Assert.assertNull(res.getResult());
      Assert.assertEquals(ExecutionStatus.ERROR, list.get(0).getExecutionStatus());
   }

}
