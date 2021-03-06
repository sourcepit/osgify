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

package org.sourcepit.osgifier.core.java.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

public class JavaLangUtilsTest {

   @Test
   public void testIsFullyQiallifiedPackageName() {
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("", "."));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com.bosch", "."));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com.bosch", "."));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com.bo-sch", "."));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com.1bosch", "."));

      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("", "/"));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com/bosch/", "/"));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com/bosch", "/"));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com/bo-sch", "/"));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com/1bosch", "/"));

      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("", "\\"));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com\\bosch\\", "\\"));
      assertTrue(JavaLangUtils.isFullyQuallifiedPackageName("com\\bosch", "\\"));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com\\bo-sch", "\\"));
      assertFalse(JavaLangUtils.isFullyQuallifiedPackageName("com\\1bosch", "\\"));
   }

   @Test
   public void testExtractTypesFromSignature() {
      try {
         JavaLangUtils.extractTypeNamesFromSignature(null);
         fail();
      }
      catch (IllegalArgumentException e) {
      }

      List<String> types = JavaLangUtils.extractTypeNamesFromSignature("Z");
      assertThat(types.size(), Is.is(0));

      types = JavaLangUtils.extractTypeNamesFromSignature("Lorg/sourcepit/Foo;");
      assertThat(types.size(), Is.is(1));
      assertThat(types.get(0), IsEqual.equalTo("org.sourcepit.Foo"));

      types = JavaLangUtils.extractTypeNamesFromSignature("Lorg/sourcepit/Lulu;");
      assertThat(types.size(), Is.is(1));
      assertThat(types.get(0), IsEqual.equalTo("org.sourcepit.Lulu"));

      types = JavaLangUtils.extractTypeNamesFromSignature("Lorg/sourcepit/Foo;Lorg/sourcepit/Bar;");
      assertThat(types.size(), Is.is(2));
      assertThat(types.get(0), IsEqual.equalTo("org.sourcepit.Foo"));
      assertThat(types.get(1), IsEqual.equalTo("org.sourcepit.Bar"));

      types = JavaLangUtils.extractTypeNamesFromSignature("[Ljava/lang/Short;");
      assertThat(types.size(), Is.is(1));
      assertThat(types.get(0), IsEqual.equalTo("java.lang.Short"));

      types = JavaLangUtils.extractTypeNamesFromSignature("Ljava/util/Map<[Ljava/lang/Short;[[[Lorg/sourcepit/Lulu;>;");
      assertThat(types.size(), Is.is(3));
      assertThat(types.get(0), IsEqual.equalTo("java.util.Map"));
      assertThat(types.get(1), IsEqual.equalTo("java.lang.Short"));
      assertThat(types.get(2), IsEqual.equalTo("org.sourcepit.Lulu"));

      types = JavaLangUtils.extractTypeNamesFromSignature("(Lorg/hamcrest/Matcher<+TE;>;)Lorg/junit/internal/matchers/CombinableMatcher<TE;>;");
      assertThat(types.size(), Is.is(2));
      assertThat(types.get(0), IsEqual.equalTo("org.hamcrest.Matcher"));
      assertThat(types.get(1), IsEqual.equalTo("org.junit.internal.matchers.CombinableMatcher"));

      types = JavaLangUtils.extractTypeNamesFromSignature("<E:Ljava/lang/Object;>(Lorg/hamcrest/Matcher<+TE;>;)Lorg/junit/internal/matchers/CombinableMatcher<TE;>;");
      assertThat(types.size(), Is.is(3));
      assertThat(types.get(0), IsEqual.equalTo("java.lang.Object"));
      assertThat(types.get(1), IsEqual.equalTo("org.hamcrest.Matcher"));
      assertThat(types.get(2), IsEqual.equalTo("org.junit.internal.matchers.CombinableMatcher"));

      types = JavaLangUtils.extractTypeNamesFromSignature("<LHS:Ljava/lang/Object;>(Lorg/hamcrest/Matcher<-TLHS;>;)Lorg/hamcrest/core/CombinableMatcher<TLHS;>;");
      assertThat(types.size(), Is.is(3));
      assertThat(types.get(0), IsEqual.equalTo("java.lang.Object"));
      assertThat(types.get(1), IsEqual.equalTo("org.hamcrest.Matcher"));
      assertThat(types.get(2), IsEqual.equalTo("org.hamcrest.core.CombinableMatcher"));
   }
}
