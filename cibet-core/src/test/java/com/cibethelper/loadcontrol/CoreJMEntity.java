/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */

package com.cibethelper.loadcontrol;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 */
@Entity
@Table(name = "CIB_JMENTITY")
@NamedQueries({ @NamedQuery(name = CoreJMEntity.DEL_ALL, query = "DELETE FROM CoreJMEntity"),
      @NamedQuery(name = CoreJMEntity.SEL, query = "SELECT a FROM CoreJMEntity a WHERE a.nameValue LIKE :nameValue AND a.owner LIKE :owner") })
public class CoreJMEntity implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final static String DEL_ALL = "com.cibethelper.loadcontrol.CoreJMEntity.DEL_ALL";
   public final static String SEL = "com.cibethelper.loadcontrol.CoreJMEntity.SEL";

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   private String nameValue;

   private int counter;

   private String owner;

   public CoreJMEntity() {
   }

   public CoreJMEntity(String nameValue, int counter, String owner) {
      this.nameValue = nameValue;
      this.counter = counter;
      this.owner = owner;
   }

   /**
    * 
    * @return the id
    */
   public long getId() {
      return id;
   }

   /**
    * 
    * @param id
    *           the id to set
    */
   public void setId(long id) {
      this.id = id;
   }

   /**
    * 
    * @return the nameValue
    */
   public String getNameValue() {
      return nameValue;
   }

   /**
    * 
    * @param nameValue
    *           the nameValue to set
    */
   public void setNameValue(String nameValue) {
      this.nameValue = nameValue;
   }

   /**
    * 
    * @return the counter
    */
   public int getCounter() {
      return counter;
   }

   /**
    * 
    * @param counter
    *           the counter to set
    */
   public void setCounter(int counter) {
      this.counter = counter;
   }

   public String getOwner() {
      return owner;
   }

   public void setOwner(String owner) {
      this.owner = owner;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("TEntity id: ");
      b.append(id);
      b.append(", counter: ");
      b.append(counter);
      b.append(", owner: ");
      b.append(owner);
      return b.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof CoreJMEntity))
         return false;
      CoreJMEntity t = (CoreJMEntity) obj;

      return (id == t.getId() && counter == t.getCounter() && nameValue.equals(t.getNameValue())
            && owner.equals(t.getOwner()));
   }

}
