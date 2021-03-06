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
package com.cibethelper.entities.owner;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.logitags.cibet.actuator.owner.Owner;

@Entity
@Table(name = "TEST_TRANSACTION")
public class Transaction implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private String id;

   @Owner
   private String tenant;

   @ManyToOne
   @Owner
   private Merchant3 merchant;

   public Transaction(String id, String tenant) {
      super();
      this.id = id;
      this.tenant = tenant;
   }

   public Transaction() {
   }

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id
    *           the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return the tenant
    */
   public String getTenant() {
      return tenant;
   }

   /**
    * @param tenant
    *           the tenant to set
    */
   public void setTenant(String tenant) {
      this.tenant = tenant;
   }

   /**
    * @return the merchant
    */
   public Merchant3 getMerchant() {
      return merchant;
   }

   /**
    * @param merchant
    *           the merchant to set
    */
   public void setMerchant(Merchant3 merchant) {
      this.merchant = merchant;
   }

}
