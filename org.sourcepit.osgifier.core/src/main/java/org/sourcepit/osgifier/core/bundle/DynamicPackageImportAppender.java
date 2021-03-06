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

package org.sourcepit.osgifier.core.bundle;

import static org.sourcepit.common.manifest.osgi.BundleHeaderName.DYNAMICIMPORT_PACKAGE;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.manifest.osgi.DynamicPackageImport;
import org.sourcepit.common.modeling.Annotation;
import org.sourcepit.osgifier.core.java.inspect.ClassForNameDetector;
import org.sourcepit.osgifier.core.model.context.BundleCandidate;
import org.sourcepit.osgifier.core.model.java.JavaResourceBundle;
import org.sourcepit.osgifier.core.model.java.JavaType;
import org.sourcepit.osgifier.core.model.java.JavaTypeVisitor;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
@Named
public class DynamicPackageImportAppender {
   private static final Logger LOGGER = LoggerFactory.getLogger(DynamicPackageImportAppender.class);

   public void append(BundleCandidate bundle) {
      final BundleManifest manifest = bundle.getManifest();
      if (!hasDynamicImportPackage(manifest, "*") && usesClassForName(bundle.getContent())) {
         LOGGER.warn("Detected usage of Class.forName(String). The behaviour of this method differs between OSGi and pure Java. Setting the 'DynamicImport-Package: *' header to workaround this problem. ");
         manifest.setHeader(DYNAMICIMPORT_PACKAGE, "*");
      }
      mergeDynamicPackageImportFromEmbeddedBundles(bundle);
   }

   private void mergeDynamicPackageImportFromEmbeddedBundles(BundleCandidate bundle) {
      final List<BundleCandidate> embeddedBundles = BundleUtils.getEmbeddedBundles(bundle);
      if (!embeddedBundles.isEmpty()) {
         final BundleManifest manifest = bundle.getManifest();

         final List<DynamicPackageImport> dynamicPackageImports = new ArrayList<DynamicPackageImport>();
         List<DynamicPackageImport> tmp = manifest.getDynamicImportPackage();
         if (tmp != null) {
            dynamicPackageImports.addAll(tmp);
         }

         boolean combine = false;
         for (BundleCandidate embeddedBundle : embeddedBundles) {
            tmp = embeddedBundle.getManifest().getDynamicImportPackage();
            if (tmp != null) {
               dynamicPackageImports.addAll(tmp);
               combine = true;
            }
         }

         if (combine) {
            final List<DynamicPackageImport> result = PackageDeclarationCombiner.combineDynamicPackageImports(dynamicPackageImports);
            if (!result.isEmpty()) {
               final List<DynamicPackageImport> target = manifest.getDynamicImportPackage(true);
               target.clear();
               target.addAll(result);
            }
         }
      }
   }

   public boolean usesClassForName(JavaResourceBundle jBundle) {
      try {
         jBundle.accept(new JavaTypeVisitor() {
            @Override
            protected void visit(JavaType jType) {
               final Annotation annotation = jType.getAnnotation(ClassForNameDetector.SOURCE);
               if (annotation != null) {
                  if (annotation.getData(ClassForNameDetector.CLASS_FOR_NAME, false)) {
                     throw new IllegalStateException("break");
                  }
               }
            }
         });
      }
      catch (IllegalStateException e) {
         if ("break".equals(e.getMessage())) {
            return true;
         }
         throw e;
      }
      return false;
   }

   private boolean hasDynamicImportPackage(BundleManifest manifest, String pattern) {
      EList<DynamicPackageImport> dynamicImportPackage = manifest.getDynamicImportPackage();
      if (dynamicImportPackage != null) {
         for (DynamicPackageImport dynamicPackageImport : dynamicImportPackage) {
            if (dynamicPackageImport.getPackageNames().contains(pattern)) {
               return true;
            }
         }
      }
      return false;
   }
}
