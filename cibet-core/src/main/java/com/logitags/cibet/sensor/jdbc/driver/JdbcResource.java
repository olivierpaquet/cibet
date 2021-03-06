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
package com.logitags.cibet.sensor.jdbc.driver;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.script.ScriptEngine;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResource;

@Entity
@DiscriminatorValue(value = "JdbcResource")
public class JdbcResource extends JpaResource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(JdbcResource.class);

   public JdbcResource() {
   }

   /**
    * constructor used for JDBC resources
    * 
    * @param sql
    * @param target
    * @param pk
    * @param params
    */
   public JdbcResource(String sql, String target, SqlParameter pk, Set<ResourceParameter> params) {
      try {
         setTargetObject(CibetUtil.encode(sql));
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new IllegalArgumentException(e);
      }
      setTarget(target);
      setPrimaryKeyObject(pk);
      if (params != null) {
         setParameters(params);
      }
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public JdbcResource(JdbcResource copy) {
      super(copy);
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGET", getTarget());
      engine.put("$TARGETOBJECT", getUnencodedTargetObject());
      engine.put("$PRIMARYKEY", getPrimaryKeyId());

      SqlParser parser = new SqlParser(null, (String) getUnencodedTargetObject());
      List<SqlParameter> setColumns = parser.getInsertUpdateColumns();
      engine.put("$COLUMNS", setColumns);
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      Connection conn = null;
      boolean close = false;
      try {
         Object delegate = Context.internalRequestScope().getApplicationEntityManager().getDelegate();
         if (delegate instanceof Connection) {
            conn = (Connection) delegate;
         } else if (delegate instanceof DataSource) {
            conn = ((DataSource) delegate).getConnection();
            close = true;
         } else {
            throw new ResourceApplyException(
                  "Application EntityManager in CibetContext is not of type JdbcBridgeEntityManager");
         }
         log.debug("EM connection: " + conn);

         StatementType statementType = null;
         Object addValue = null;
         for (ResourceParameter par : getParameters()) {
            if (par.getParameterType() == ParameterType.JDBC_STATEMENT_TYPE) {
               statementType = (StatementType) par.getUnencodedValue();
            }
            if (par.getParameterType() == ParameterType.JDBC_STATEMENT_ADDITIONAL_VALUE) {
               addValue = par.getUnencodedValue();
            }
         }
         if (statementType == null) {
            String err = "Failed to retrieve Statement type from the ResourceParameters. No parameter of type "
                  + ParameterType.JDBC_STATEMENT_TYPE + " is present";
            throw new RuntimeException(err);
         }

         Object result = null;
         switch (statementType) {
         case STATEMENT_EXECUTE:
            result = executeStatement(conn, addValue);
            break;
         case STATEMENT_EXECUTEUPDATE:
            result = executeUpdateStatement(conn, addValue);
            break;
         case PREPAREDSTATEMENT_EXECUTE:
            result = executePreparedStatement(conn);
            break;
         case PREPAREDSTATEMENT_EXECUTEUPDATE:
            result = executeUpdatePreparedStatement(conn);
            break;
         }

         if (log.isDebugEnabled()) {
            log.debug(getUnencodedTargetObject() + ": result: " + result);
         }
         return result;

      } catch (SQLException e) {
         log.error(e.getMessage(), e);
         throw new ResourceApplyException(e.getMessage(), e);
      } finally {
         if (close) {
            if (conn != null)
               try {
                  conn.close();
               } catch (SQLException e) {
                  log.error(e.getMessage(), e);
               }
         }
      }
   }

   /**
    * the primary key object of a JPA or JDBC resource.
    * 
    * @param id
    *           the primaryKeyObject to set
    */
   public void setPrimaryKeyObject(Object id) {
      this.primaryKeyObject = id;
      if (id != null) {
         SqlParameter sqlId = (SqlParameter) id;
         setPrimaryKeyId(sqlId.getValue() == null ? null : sqlId.getValue().toString());
      }
   }

   private boolean executeStatement(Connection conn, Object addValue) throws SQLException {
      Statement stmt = null;
      try {
         stmt = conn.createStatement();

         if (addValue == null) {
            return stmt.execute((String) getUnencodedTargetObject());
         } else if (addValue instanceof Integer) {
            return stmt.execute((String) getUnencodedTargetObject(), (int) addValue);
         } else if (addValue instanceof int[]) {
            return stmt.execute((String) getUnencodedTargetObject(), (int[]) addValue);
         } else if (addValue instanceof String[]) {
            return stmt.execute((String) getUnencodedTargetObject(), (String[]) addValue);
         } else {
            throw new RuntimeException(
                  "Failed to execute Statement " + (String) getUnencodedTargetObject() + " with additional value "
                        + addValue + ": Type " + addValue.getClass().getName() + " is not supported");
         }
      } finally {
         if (stmt != null)
            stmt.close();
      }
   }

   private int executeUpdateStatement(Connection conn, Object addValue) throws SQLException {
      Statement stmt = null;
      try {
         stmt = conn.createStatement();

         if (addValue == null) {
            return stmt.executeUpdate((String) getUnencodedTargetObject());
         } else if (addValue instanceof Integer) {
            return stmt.executeUpdate((String) getUnencodedTargetObject(), (int) addValue);
         } else if (addValue instanceof int[]) {
            return stmt.executeUpdate((String) getUnencodedTargetObject(), (int[]) addValue);
         } else if (addValue instanceof String[]) {
            return stmt.executeUpdate((String) getUnencodedTargetObject(), (String[]) addValue);
         } else {
            throw new RuntimeException(
                  "Failed to executeUpdate Statement " + (String) getUnencodedTargetObject() + " with additional value "
                        + addValue + ": Type " + addValue.getClass().getName() + " is not supported");
         }
      } finally {
         if (stmt != null)
            stmt.close();
      }
   }

   private boolean executePreparedStatement(Connection conn) throws SQLException {
      PreparedStatement stmt = null;
      try {
         stmt = conn.prepareStatement((String) getUnencodedTargetObject());
         for (ResourceParameter par : getParameters()) {
            setParameter(stmt, par);
         }
         return stmt.execute();
      } finally {
         if (stmt != null)
            stmt.close();
      }
   }

   private int executeUpdatePreparedStatement(Connection conn) throws SQLException {
      PreparedStatement stmt = null;
      try {
         stmt = conn.prepareStatement((String) getUnencodedTargetObject());
         for (ResourceParameter par : getParameters()) {
            setParameter(stmt, par);
         }
         return stmt.executeUpdate();
      } finally {
         if (stmt != null)
            stmt.close();
      }
   }

   private void setParameter(PreparedStatement stmt, ResourceParameter par) throws SQLException {
      switch (par.getParameterType()) {
      case JDBC_PARAMETER_CHARACTERSTREAM:
         Reader rd = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setCharacterStream(par.getSequence(), rd);
         break;

      case JDBC_PARAMETER_CHARACTERSTREAM_INT:
         Reader rd1 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setCharacterStream(par.getSequence(), rd1, Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_CHARACTERSTREAM_LONG:
         Reader rd2 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setCharacterStream(par.getSequence(), rd2, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_CLOB_READER:
         Reader rd3 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setClob(par.getSequence(), rd3);
         break;

      case JDBC_PARAMETER_CLOB_READER_LONG:
         Reader rd4 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setClob(par.getSequence(), rd4, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_NCHARACTERSTREAM:
         Reader rd5 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setNCharacterStream(par.getSequence(), rd5);
         break;

      case JDBC_PARAMETER_NCHARACTERSTREAM_LONG:
         Reader rd6 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setNCharacterStream(par.getSequence(), rd6, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_NCLOB_READER:
         Reader rd7 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setNClob(par.getSequence(), rd7);
         break;

      case JDBC_PARAMETER_NCLOB_READER_LONG:
         Reader rd8 = par.getUnencodedValue() == null ? null : new CharArrayReader((char[]) par.getUnencodedValue());
         stmt.setNClob(par.getSequence(), rd8, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_ASCIISTREAM:
         InputStream in = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setAsciiStream(par.getSequence(), in);
         break;

      case JDBC_PARAMETER_ASCIISTREAM_INT:
         InputStream in1 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setAsciiStream(par.getSequence(), in1, Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_ASCIISTREAM_LONG:
         InputStream in2 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setAsciiStream(par.getSequence(), in2, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_UNICODESTREAM:
         InputStream in3 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setUnicodeStream(par.getSequence(), in3, Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_BINARYSTREAM:
         InputStream in4 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setBinaryStream(par.getSequence(), in4);
         break;

      case JDBC_PARAMETER_BINARYSTREAM_INT:
         InputStream in5 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setBinaryStream(par.getSequence(), in5, Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_BINARYSTREAM_LONG:
         InputStream in6 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setBinaryStream(par.getSequence(), in6, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_INT:
         stmt.setInt(par.getSequence(), (Integer) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_LONG:
         stmt.setLong(par.getSequence(), (Long) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_SHORT:
         stmt.setShort(par.getSequence(), (Short) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_FLOAT:
         stmt.setFloat(par.getSequence(), (Float) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_DOUBLE:
         stmt.setDouble(par.getSequence(), (Double) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_BIGDECIMAL:
         stmt.setBigDecimal(par.getSequence(), (BigDecimal) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_DATE:
         stmt.setDate(par.getSequence(), (Date) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_DATE_CAL:
         Date date = "-".equals(par.getClassname()) ? null : new Date(Long.parseLong(par.getClassname()));
         stmt.setDate(par.getSequence(), date, (Calendar) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_TIME:
         stmt.setTime(par.getSequence(), (Time) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_TIME_CAL:
         Time time = "-".equals(par.getClassname()) ? null : new Time(Long.parseLong(par.getClassname()));
         stmt.setTime(par.getSequence(), time, (Calendar) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_TIMESTAMP:
         stmt.setTimestamp(par.getSequence(), (Timestamp) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_TIMESTAMP_CAL:
         Timestamp timestamp = "-".equals(par.getClassname()) ? null
               : new Timestamp(Long.parseLong(par.getClassname()));
         stmt.setTimestamp(par.getSequence(), timestamp, (Calendar) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_BYTE:
         stmt.setByte(par.getSequence(), (Byte) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_BYTES:
         stmt.setBytes(par.getSequence(), (byte[]) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_BOOLEAN:
         stmt.setBoolean(par.getSequence(), (Boolean) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_STRING:
         stmt.setString(par.getSequence(), (String) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_NSTRING:
         stmt.setNString(par.getSequence(), (String) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_NULL:
         stmt.setNull(par.getSequence(), Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_NULL_TYPENAME:
         stmt.setNull(par.getSequence(), Integer.parseInt(par.getClassname()), par.getName());
         break;

      case JDBC_PARAMETER_BLOB_INPUTSTREAM:
         InputStream in7 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setBlob(par.getSequence(), in7);
         break;

      case JDBC_PARAMETER_BLOB_INPUTSTREAM_LONG:
         InputStream in8 = par.getUnencodedValue() == null ? null
               : new ByteArrayInputStream((byte[]) par.getUnencodedValue());
         stmt.setBlob(par.getSequence(), in8, Long.parseLong(par.getClassname()));
         break;

      case JDBC_PARAMETER_OBJECT:
         stmt.setObject(par.getSequence(), par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_OBJECT_TARGETSQLTYPE:
         stmt.setObject(par.getSequence(), par.getUnencodedValue(), Integer.parseInt(par.getClassname()));
         break;

      case JDBC_PARAMETER_OBJECT_TARGETSQLTYPE_SCALE:
         stmt.setObject(par.getSequence(), par.getUnencodedValue(), Integer.parseInt(par.getClassname()),
               Integer.parseInt(par.getName()));
         break;

      case JDBC_PARAMETER_URL:
         stmt.setURL(par.getSequence(), (URL) par.getUnencodedValue());
         break;

      case JDBC_PARAMETER_BLOB:
         try {
            Blob blob = null;
            if (par.getUnencodedValue() != null) {
               blob = stmt.getConnection().createBlob();
               OutputStream out = blob.setBinaryStream(1L);
               out.write((byte[]) par.getUnencodedValue());
               out.flush();
               out.close();
            }
            stmt.setBlob(par.getSequence(), blob);
            break;
         } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CibetJdbcException(e.getMessage(), e);
         }

      case JDBC_PARAMETER_CLOB:
         try {
            Clob clob = null;
            if (par.getUnencodedValue() != null) {
               clob = stmt.getConnection().createClob();
               Writer out = clob.setCharacterStream(1L);
               out.write((char[]) par.getUnencodedValue());
               out.flush();
               out.close();
            }

            stmt.setClob(par.getSequence(), clob);
            break;
         } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CibetJdbcException(e.getMessage(), e);
         }

      case JDBC_PARAMETER_NCLOB:
         try {
            NClob clob = null;
            if (par.getUnencodedValue() != null) {
               clob = stmt.getConnection().createNClob();
               Writer out = clob.setCharacterStream(1L);
               out.write((char[]) par.getUnencodedValue());
               out.flush();
               out.close();
            }

            stmt.setNClob(par.getSequence(), clob);
            break;
         } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CibetJdbcException(e.getMessage(), e);
         }

      default:
         log.debug(
               "Ignoring other parameter of type " + par.getParameterType() + " with value " + par.getUnencodedValue());
      }
   }

   public String toString() {
      StringBuffer b = new StringBuffer(super.toString());

      b.append(" ; SQL: ");
      b.append(getUnencodedTargetObject());

      return b.toString();
   }

}
