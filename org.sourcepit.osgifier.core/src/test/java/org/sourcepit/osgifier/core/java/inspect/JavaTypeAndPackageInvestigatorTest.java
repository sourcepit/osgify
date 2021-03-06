/*
 * Copyright 2014 Bernd Vogt and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sourcepit.osgifier.core.java.inspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.sourcepit.common.modeling.Annotation;
import org.sourcepit.osgifier.core.model.java.JavaArchive;
import org.sourcepit.osgifier.core.model.java.JavaClass;
import org.sourcepit.osgifier.core.model.java.JavaModelFactory;
import org.sourcepit.osgifier.core.model.java.JavaPackage;
import org.sourcepit.osgifier.core.model.java.JavaProject;
import org.sourcepit.osgifier.core.model.java.JavaType;
import org.sourcepit.osgifier.core.util.AbstractTraverserTest;
import org.sourcepit.osgifier.test.resources.TypeA;

/**
 * @author Bernd
 */
public class JavaTypeAndPackageInvestigatorTest {

   @Test
   public void testJar() throws Exception {
      try {
         new JavaResourcesBundleScanner().scan((JavaArchive) null, null, null);
         fail();
      }
      catch (IllegalArgumentException e) { // noop
      }

      JavaArchive javaArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      try {
         new JavaResourcesBundleScanner().scan(javaArchive, null, null);
         fail();
      }
      catch (IllegalArgumentException e) { // noop
      }

      File jarFile = new File("target/testResources/org.sourcepit.osgifier.core.jar");
      assertTrue(jarFile.exists());

      javaArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      new JavaResourcesBundleScanner().scan(javaArchive, jarFile, null);

      JavaPackage pgk = javaArchive.getPackage(AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH, false);
      assertThat(pgk, IsNull.notNullValue());

      org.sourcepit.osgifier.core.model.java.File pkgInfo = pgk.getFile("packageinfo");
      assertThat(pkgInfo, IsNull.notNullValue());

      String version = pkgInfo.getAnnotationData("content", "version");
      assertThat(version, IsEqual.equalTo("1.0"));

      JavaType type = javaArchive.getType(AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH,
         TypeA.class.getSimpleName(), false);
      assertThat(type, IsNull.notNullValue());

      JavaClass jClass = (JavaClass) type.getFile();
      assertThat(jClass, IsNull.notNullValue());
      assertThat(jClass.getMajor() >= 49, Is.is(true));
      assertThat(jClass.getMinor() > -1, Is.is(true));

      EList<JavaType> innerTypes = type.getInnerTypes();
      assertThat(innerTypes.size(), Is.is(1));

      JavaType innerType = innerTypes.get(0);
      assertThat(innerType.getName(), IsEqual.equalTo(TypeA.Hans.class.getSimpleName()));
      assertThat(innerType.getOuterType(), IsEqual.equalTo(type));

      JavaType innerType2 = javaArchive.getType(AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH,
         TypeA.class.getSimpleName() + "." + TypeA.Hans.class.getSimpleName(), false);
      assertThat(innerType, IsEqual.equalTo(innerType2));
   }

   @Test
   public void testProject() throws Exception {
      try {
         new JavaResourcesBundleScanner().scan((JavaProject) null, null, null);
         fail();
      }
      catch (IllegalArgumentException e) { // noop
      }

      JavaProject javaProject = JavaModelFactory.eINSTANCE.createJavaProject();
      try {
         new JavaResourcesBundleScanner().scan(javaProject, null, null);
         fail();
      }
      catch (IllegalArgumentException e) { // noop
      }

      File projectDir = new File("target/testResources");
      assertTrue(projectDir.exists());

      javaProject = JavaModelFactory.eINSTANCE.createJavaProject();

      new JavaResourcesBundleScanner().scan(javaProject, projectDir, null);

      JavaPackage pgk = javaProject.getPackage("", AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH, false);
      assertThat(pgk, IsNull.notNullValue());

      org.sourcepit.osgifier.core.model.java.File pkgInfo = pgk.getFile("packageinfo");
      assertThat(pkgInfo, IsNull.notNullValue());

      String version = pkgInfo.getAnnotationData("content", "version");
      assertThat(version, IsEqual.equalTo("1.0"));

      JavaType type = javaProject.getType("", AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH,
         TypeA.class.getSimpleName(), false);
      assertThat(type, IsNull.notNullValue());

      JavaClass jClass = (JavaClass) type.getFile();
      assertThat(jClass, IsNull.notNullValue());
      assertThat(jClass.getMajor() >= 49, Is.is(true));
      assertThat(jClass.getMinor() > -1, Is.is(true));

      EList<JavaType> innerTypes = type.getInnerTypes();
      assertThat(innerTypes.size(), Is.is(1));

      JavaType innerType = innerTypes.get(0);
      assertThat(innerType.getName(), IsEqual.equalTo(TypeA.Hans.class.getSimpleName()));
      assertThat(innerType.getOuterType(), IsEqual.equalTo(type));

      JavaType innerType2 = javaProject.getType("", AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH,
         TypeA.class.getSimpleName() + "." + TypeA.Hans.class.getSimpleName(), false);
      assertThat(innerType, IsEqual.equalTo(innerType2));
   }

   @Test
   public void testWithReferenceCollector() {
      File projectDir = new File("target/testResources");
      assertTrue(projectDir.exists());

      JavaProject javaProject = JavaModelFactory.eINSTANCE.createJavaProject();

      new JavaResourcesBundleScanner().scan(javaProject, projectDir,
         Collections.singleton(new JavaTypeReferencesAnalyzer()));

      JavaType jType = javaProject.getType("", AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH,
         TypeA.class.getSimpleName(), false);

      Annotation typeRefs = jType.getAnnotation("referencedTypes");

      Set<String> expectedRefs = new HashSet<String>();
      expectedRefs.add("java.lang.Object");
      expectedRefs.add("java.lang.String");
      expectedRefs.add("java.lang.Boolean");

      assertThat(typeRefs.getReferences().keySet(), IsEqual.equalTo(expectedRefs));

      jType = javaProject.getType("", AbstractTraverserTest.TEST_RESOURCES_PACKAGE_PATH, "TypeA.Hans", false);

      typeRefs = jType.getAnnotation("referencedTypes");

      expectedRefs = new HashSet<String>();

      expectedRefs.add("java.util.Map");
      expectedRefs.add("java.lang.Short");
      expectedRefs.add("java.lang.Boolean");
      expectedRefs.add("org.hamcrest.Matcher");
      expectedRefs.add("org.hamcrest.BaseMatcher");
      expectedRefs.add("java.lang.Object");
      expectedRefs.add("java.lang.String");
      expectedRefs.add("java.lang.Long");
      expectedRefs.add("java.util.HashMap");
      expectedRefs.add("java.lang.Runnable");
      expectedRefs.add("java.lang.Integer");

      final Set<String> actualRefs = typeRefs.getReferences().keySet();
      assertEquals(expectedRefs.size(), actualRefs.size());
      assertThat(actualRefs, IsEqual.equalTo(expectedRefs));
   }
}
