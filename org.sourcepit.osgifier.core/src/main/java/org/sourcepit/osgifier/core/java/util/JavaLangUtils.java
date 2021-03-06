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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.sourcepit.common.constraints.NotNull;
import org.sourcepit.common.utils.path.Path;

public final class JavaLangUtils {
   private JavaLangUtils() {
      super();
   }

   public static boolean isFullyQuallifiedPackageName(@NotNull Path path) {
      return isFullyQuallifiedPackageName(path.toString(), Path.SEPARATOR);
   }

   public static boolean isFullyQuallifiedPackageName(@NotNull String fullyQuallifiedName) {
      return isFullyQuallifiedPackageName(fullyQuallifiedName, ".");
   }

   public static boolean isFullyQuallifiedPackageName(@NotNull String fullyQuallifiedName, @NotNull String separator) {
      final StringTokenizer segments = new StringTokenizer(fullyQuallifiedName, separator);
      while (segments.hasMoreTokens()) {
         final String segment = segments.nextToken();
         if (!isJavaPackageSegmentName(segment)) {
            return false;
         }
      }
      return true;
   }

   public static boolean isJavaPackageSegmentName(@NotNull String segmentName) {
      final char[] chars = segmentName.toCharArray();
      if (chars.length > 0) {
         if (!Character.isJavaIdentifierStart(chars[0])) {
            return false;
         }
         for (int i = 1; i < chars.length; i++) {
            if (!Character.isJavaIdentifierPart(chars[i])) {
               return false;
            }
         }
      }
      return true;
   }

   @NotNull
   public static String toPackageName(@NotNull Path path) {
      return toPackageName(path.toString());
   }

   @NotNull
   public static String toPackageName(@NotNull String resourcePath) {
      String pkgName = resourcePath.replace('/', '.');
      if (pkgName.endsWith(".")) {
         pkgName = pkgName.substring(0, pkgName.length() - 1);
      }
      return pkgName;
   }

   @NotNull
   public static String[] getPackageAndFileName(@NotNull String resourcePath) {
      String[] pathAndFileName = trimFileName(resourcePath);
      String packageName = toPackageName(pathAndFileName[0]);
      String typeName = pathAndFileName[1];
      if (typeName.endsWith(".class")) {
         typeName = typeName.substring(0, typeName.length() - 6);
      }
      return new String[] { packageName, typeName };
   }

   @NotNull
   public static String[] trimFileName(@NotNull String resourcePath) {
      int idx = resourcePath.lastIndexOf("/");
      final String parentPath;
      final String fileName;
      if (idx == -1) {
         parentPath = "";
         fileName = resourcePath;
      }
      else {
         parentPath = resourcePath.substring(0, idx);
         fileName = resourcePath.substring(idx + 1);
      }
      return new String[] { parentPath, fileName };
   }

   @NotNull
   public static List<String> extractTypeNamesFromSignature(@NotNull String signature) {
      final List<String> typesNames = new ArrayList<String>();
      extractTypeNamesFromSignature(typesNames, signature);
      return typesNames;
   }

   // CSOFF
   private static void extractTypeNamesFromSignature(Collection<String> typesNames, String signature)
   // CSON
   {
      final char[] chars = signature.toCharArray();
      StringBuilder sb = null;
      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         switch (c) {
            case ':' :
               sb = null;
               break;
            case ';' :
            case '<' :
               if (sb != null) {
                  typesNames.add(normalizedTypeName(sb.toString()));
                  sb = null;
               }
               break;
            case 'L' :
               if (sb == null) {
                  sb = new StringBuilder();
                  break;
               }
            case 'T' :
               if (sb == null) {
                  i++;
                  break;
               }
            default :
               if (sb != null) {
                  sb.append(c);
               }
               break;
         }
      }
   }

   private static String normalizedTypeName(String typeName) {
      return typeName.replace('/', '.');
   }
}
