/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.jca.adapters.jdbc.extensions.oracle;

import org.jboss.jca.adapters.jdbc.spi.reauth.ReauthPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Oracle plugin for reauthentication.
 *
 * @author <a href="mailto:ricardo.arguello@gmail.com">Ricardo Arguello</a>
 */
public class OracleReauthPlugin implements ReauthPlugin
{
   private Method openProxySessionMethod;
   private Field proxyTypeUserNameField;
   private Field proxyUserNameField;
   private Field proxyPasswordField;

   /**
    * Default constructor
    */
   public OracleReauthPlugin()
   {
   }

   /**
    * Initialize
    * @param cl The class loader which can be used for initialization
    * @exception SQLException Thrown in case of an error
    */
   public synchronized void initialize(ClassLoader cl) throws SQLException {
      try
      {
         Class<?> oracleConnection = cl.loadClass("oracle.jdbc.OracleConnection");
         openProxySessionMethod = oracleConnection.getMethod("openProxySession", new Class[] {Integer.class, Properties.class});
	 proxyTypeUserNameField = oracleConnection.getField("PROXYTYPE_USER_NAME");
	 proxyUserNameField = oracleConnection.getField("PROXY_USER_NAME");
	 proxyPasswordField = oracleConnection.getField("PROXY_PASSWORD");
      } 
      catch (Throwable t) 
      {
         throw new SQLException("Cannot resolve oracle.jdbc.OracleConnection openProxySession method", t);
      }
   }

   /**
    * Reauthenticate
    * @param c The connection
    * @param userName The user name
    * @param password The password
    * @exception SQLException Thrown in case of an error
    */
   public synchronized void reauthenticate(Connection c, String userName, String password) throws SQLException
   {
      int proxyTypeUserName = 0;
      int proxyUserName = 0;
      int proxyPassword = 0;

      try
      {
         proxyTypeUserName = proxyTypeUserNameField.getInt(c);
         proxyUserName = proxyUserNameField.getInt(c);
         proxyPassword = proxyPasswordField.getInt(c);
      }
      catch (IllegalAccessException e)
      {
         throw new SQLException("Unexpected error in openProxySession", e);
      }

      Properties props = new Properties();
      props.put(proxyUserName, userName);

      if (password != null)
      {
         props.put(proxyPassword, password);
      }

      Object[] params = new Object[] {proxyTypeUserName, props};

      try
      {
         openProxySessionMethod.invoke(c, params);
      }
      catch (Throwable t) 
      {
         Throwable cause = t.getCause();

         if (cause instanceof SQLException)
         {
            throw (SQLException) cause;
         }
         else
         {
            throw new SQLException("Unexpected error in openProxySession", t);
         }
      }
   }
}
