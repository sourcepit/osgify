/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.bundle;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.manifest.osgi.BundleManifestFactory;
import org.sourcepit.osgify.core.model.context.BundleCandidate;
import org.sourcepit.osgify.core.model.context.OsgifyContext;
import org.sourcepit.osgify.core.util.OsgifyContextUtils;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
@Named
public class BundleManifestAppender
{
   private static final Logger LOGGER = LoggerFactory.getLogger(BundleManifestAppender.class);

   @Inject
   private RequiredExecutionEnvironmentAppender environmentAppender;

   @Inject
   private PackageExportAppender packageExports;

   @Inject
   private DynamicPackageImportAppender dynamicImports;
   
   @Inject
   private PackageImportAppender packageImports;

   public void append(OsgifyContext context)
   {
      final List<BundleCandidate> bundleCandidates = OsgifyContextUtils.computeBuildOrder(context);
      for (BundleCandidate bundleCandidate : bundleCandidates)
      {
         if (bundleCandidate.isNativeBundle())
         {
            LOGGER.info("Skipping manifest creation for native bundle " + bundleCandidate.getSymbolicName() + "_"
               + bundleCandidate.getVersion());
         }
         else
         {
            LOGGER.info("Building manifest for bundle candidate " + bundleCandidate.getSymbolicName() + "_"
               + bundleCandidate.getVersion());
            append(bundleCandidate);
         }
      }
   }

   private void append(BundleCandidate bundle)
   {
      final BundleManifest manifest = BundleManifestFactory.eINSTANCE.createBundleManifest();
      manifest.setBundleSymbolicName(bundle.getSymbolicName());
      manifest.setBundleVersion(bundle.getVersion());
      bundle.setManifest(manifest);

      environmentAppender.append(bundle);
      dynamicImports.append(bundle);
      packageExports.append(bundle);
      packageImports.append(bundle);
   }
}