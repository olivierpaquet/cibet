/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.sensor.jpa;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

public class CibetTestPersistenceProvider implements PersistenceProvider {

   @Override
   public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo arg0, Map arg1) {
      return (EntityManagerFactory) arg1.get("EMF");
   }

   @Override
   public EntityManagerFactory createEntityManagerFactory(String arg0, Map arg1) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ProviderUtil getProviderUtil() {
      return null;
   }

   @Override
   public void generateSchema(PersistenceUnitInfo info, Map map) {
   }

   @Override
   public boolean generateSchema(String persistenceUnitName, Map map) {
      return false;
   }

}
