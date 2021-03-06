/*
Copyright 2014 Bernd Vogt and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.sourcepit.osgifier.core.tools;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.sourcepit.osgifier.core.tools.ExecutionEnvironmentAnalyzer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public class ExecutionEnvironmentAnalyzerTest
{
   private Properties readProxyProperties() throws IOException
   {
      InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/proxy.properties");
      try
      {
         final Properties properties = new Properties();
         assertThat(stream, IsNull.notNullValue());
         properties.load(stream);
         String property = properties.getProperty("http.proxyHost", "").trim();
         if (property.length() == 0 || property.startsWith("$"))
         {
            return null;
         }
         return properties;
      }
      finally
      {
         IOUtils.closeQuietly(stream);
      }
   }

   @Test
   public void test() throws Exception
   {
      Properties proxyProps = readProxyProperties();
      if (proxyProps != null)
      {
         Set<Entry<Object, Object>> entrySet = proxyProps.entrySet();
         for (Entry<Object, Object> entry : entrySet)
         {
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
         }
      }

      final File javaHome = new File(System.getProperty("java.home"));
      assertThat(javaHome.exists(), Is.is(true));

      final List<File> javaHomes = new ArrayList<File>();
      javaHomes.add(javaHome);

      StringWriter writer = new StringWriter();

      ExecutionEnvironmentAnalyzer jvmWriter = new ExecutionEnvironmentAnalyzer();
      jvmWriter.analyze(javaHomes, writer);

      JsonArray jvms = parse(writer.toString());
      assertThat(jvms, IsNull.notNullValue());
      assertThat(jvms.size(), Is.is(1));
      for (JsonElement jvm : jvms)
      {
         assertJvmInfo((JsonObject) jvm);
      }

      javaHomes.add(javaHome);
      writer = new StringWriter();
      jvmWriter.analyze(javaHomes, writer);

      jvms = parse(writer.toString());
      assertThat(jvms, IsNull.notNullValue());
      assertThat(jvms.size(), Is.is(2));
      for (JsonElement jvm : jvms)
      {
         assertJvmInfo((JsonObject) jvm);
      }
   }

   private static void assertJvmInfo(JsonObject jvm)
   {
      assertThat(jvm, IsNull.notNullValue());
      assertThat(jvm.get("vendor"), IsNull.notNullValue());
      assertThat(jvm.get("version"), IsNull.notNullValue());
      JsonArray jPackages = (JsonArray) jvm.get("platformPackages");
      for (JsonElement jPackage : jPackages)
      {
         String name = jPackage.getAsString();
         if ("java.lang".equals(name))
         {
            return;
         }
      }
      jPackages = (JsonArray) jvm.get("vendorPackages");
      for (JsonElement jPackage : jPackages)
      {
         String name = jPackage.getAsString();
         System.out.println(name);
         if ("sun.misc".equals(name))
         {
            return;
         }
      }
      fail("Unable to find package java.lang");
   }

   private JsonArray parse(String json)
   {
      return (JsonArray) new JsonStreamParser(new StringReader(json)).next();
   }

}
