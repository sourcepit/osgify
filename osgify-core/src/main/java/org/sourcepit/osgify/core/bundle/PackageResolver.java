/**
 * Copyright (c) 2014 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.bundle;

import static org.sourcepit.osgify.core.bundle.AccessModifier.INTERNAL;
import static org.sourcepit.osgify.core.bundle.AccessModifier.PUBLIC;
import static org.sourcepit.osgify.core.bundle.PackageOrigin.OWN_BUNDLE;
import static org.sourcepit.osgify.core.bundle.PackageOrigin.REQUIRED_BUNDLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.sourcepit.common.manifest.osgi.PackageExport;
import org.sourcepit.osgify.core.ee.AccessRule;
import org.sourcepit.osgify.core.ee.ExecutionEnvironmentService;
import org.sourcepit.osgify.core.model.context.BundleCandidate;
import org.sourcepit.osgify.core.model.context.BundleReference;

@Named
public class PackageResolver
{
   private ExecutionEnvironmentService environmentService;

   @Inject
   public PackageResolver(ExecutionEnvironmentService environmentService)
   {
      this.environmentService = environmentService;
   }

   public final List<PackageResolutionResult> resolveRequiredPackages(BundleCandidate bundle,
      boolean treatInheritedPackagesAsInternal)
   {
      final Collection<String> referencedPackages = getReferencedPackages(bundle);

      Map<String, List<PackageOffer>> foo = new LinkedHashMap<String, List<PackageOffer>>(referencedPackages.size());
      for (String requiredPackage : referencedPackages)
      {
         final List<PackageOffer> exportes = resolve(bundle, requiredPackage);
         if (exportes.isEmpty())
         {
            final boolean hiddenBundlePackage = getBundlePackages(bundle).getPackages().contains(requiredPackage);
            if (!hiddenBundlePackage)
            {
               foo.put(requiredPackage, exportes);
            }
         }
         else
         {
            foo.put(requiredPackage, exportes);
         }
      }

      final Map<BundleCandidate, AccessModifier> bundleToAccessRuleMap = new HashMap<BundleCandidate, AccessModifier>();

      for (Entry<String, List<PackageOffer>> entry : foo.entrySet())
      {
         if (!entry.getValue().isEmpty())
         {
            final String requiredPackage = entry.getKey();
            final PackageOffer selectedOffer = entry.getValue().get(0);
            updateBundleAccessRule(bundleToAccessRuleMap, bundle, requiredPackage, selectedOffer,
               treatInheritedPackagesAsInternal);
         }
      }

      final List<PackageResolutionResult> result = new ArrayList<PackageResolutionResult>();

      for (Entry<String, List<PackageOffer>> entry : foo.entrySet())
      {
         final String requiredPackage = entry.getKey();
         final List<PackageOffer> offers = entry.getValue();
         final PackageOffer selectedOffer = offers.isEmpty() ? null : offers.get(0);

         final AccessModifier accessRule;
         if (selectedOffer == null)
         {
            accessRule = null;
         }
         else
         {
            switch (selectedOffer.getType())
            {
               case OWN_BUNDLE :
               case REQUIRED_BUNDLE :
                  accessRule = bundleToAccessRuleMap.get(selectedOffer.getBundle());
                  break;
               case EXECUTION_ENVIRONMENT :
                  accessRule = PUBLIC;
                  break;
               case VENDOR :
                  accessRule = INTERNAL;
                  break;
               default :
                  throw new IllegalStateException();
            }
         }

         result.add(new PackageResolutionResult(requiredPackage, selectedOffer, accessRule, offers));
      }

      return result;
   }

   private void updateBundleAccessRule(final Map<BundleCandidate, AccessModifier> bundleToAccessRuleMap,
      BundleCandidate bundle, final String requiredPackage, final PackageOffer selectedOffer,
      boolean treatInheritedPackagesAsInternal)
   {
      if (selectedOffer != null
         && (selectedOffer.getType() == REQUIRED_BUNDLE || selectedOffer.getType() == OWN_BUNDLE))
      {
         final BundleCandidate requiredBundle = selectedOffer.getBundle();

         final AccessModifier currentImportType = bundleToAccessRuleMap.get(requiredBundle);
         if (currentImportType == null || currentImportType == PUBLIC)
         {
            final PackageExport packageExport = selectedOffer.getPackageExport();

            AccessModifier access = BundleUtils.isInternalPackage(packageExport) ? INTERNAL : PUBLIC;

            if (access == PUBLIC && treatInheritedPackagesAsInternal)
            {
               // if our bundle implements classes from a public package we assume that we are providing an api
               // implementation
               final boolean roleProvider = getBundlePackages(bundle).getReferencedPackages().getInherited()
                  .contains(requiredPackage);
               if (roleProvider)
               {
                  access = INTERNAL;
               }
            }
            bundleToAccessRuleMap.put(requiredBundle, access);
         }
      }
   }


   private List<PackageOffer> resolve(BundleCandidate bundle, String requiredPackage)
   {
      final List<PackageOffer> exporters = new ArrayList<PackageOffer>();

      // self
      {
         final PackageExport packageExport = getPackageExport(bundle, requiredPackage);
         if (packageExport != null)
         {
            exporters.add(PackageOffer.exportedByOwnBundle(bundle, packageExport));
         }
      }

      // dependencies
      for (BundleReference bundleReference : bundle.getDependencies())
      {
         final BundleCandidate requiredBundle = bundleReference.getTarget();

         final PackageExport packageExport = getPackageExport(requiredBundle, requiredPackage);
         if (packageExport != null)
         {
            exporters.add(PackageOffer.exportedByRequiredBundle(bundleReference, packageExport));
         }
      }

      // ee or vendor
      switch (getAccessRule(bundle, requiredPackage))
      {
         case ACCESSIBLE : // ee
            exporters.add(PackageOffer.exportedByExecutionEnvironment());
            break;
         case DISCOURAGED : // vendor
            exporters.add(PackageOffer.exportedByVendor());
            break;
         case NON_ACCESSIBLE : // neither nor
            break;
         default :
            throw new IllegalStateException();
      }

      return exporters;
   }

   private Collection<String> getReferencedPackages(BundleCandidate bundle)
   {
      final BundlePackages bundlePackages = getBundlePackages(bundle);

      final Collection<String> requiredPackges = new ArrayList<String>(bundlePackages.getReferencedPackages().getAll());
      removeEEPackages(bundle, requiredPackges);

      // add self imports after removing ee packages to add ee packages contributed by our bundle also
      final List<PackageExport> exportPackage = bundle.getManifest().getExportPackage();
      if (exportPackage != null)
      {
         for (PackageExport packageExport : exportPackage)
         {
            requiredPackges.addAll(packageExport.getPackageNames());
         }
      }

      return requiredPackges;
   }

   private final WeakHashMap<BundleCandidate, BundlePackages> cache = new WeakHashMap<BundleCandidate, BundlePackages>();

   private BundlePackages getBundlePackages(BundleCandidate bundle)
   {
      BundlePackages result = cache.get(bundle);
      if (result == null)
      {
         result = new BundlePackagesCollector().collect(bundle);
         cache.put(bundle, result);
      }
      return result;
   }

   private void removeEEPackages(BundleCandidate bundle, final Collection<String> requiredPackges)
   {
      final List<String> requiredEEs = bundle.getManifest().getBundleRequiredExecutionEnvironment();
      if (requiredEEs != null)
      {
         for (String ee : requiredEEs)
         {
            requiredPackges.removeAll(environmentService.getExecutionEnvironment(ee).getPackages());
         }
      }
   }

   private static final PackageExport getPackageExport(BundleCandidate requiredBundle, String requiredPackage)
   {
      final List<PackageExport> exportPackage = requiredBundle.getManifest().getExportPackage();
      if (exportPackage != null)
      {
         for (PackageExport packageExport : exportPackage)
         {
            if (packageExport.getPackageNames().contains(requiredPackage))
            {
               return packageExport;
            }
         }
      }
      return null;
   }

   private AccessRule getAccessRule(BundleCandidate bundle, String requiredPackage)
   {
      final List<String> requiredEEs = bundle.getManifest().getBundleRequiredExecutionEnvironment();
      if (requiredEEs != null)
      {
         return environmentService.getAccessRuleById(requiredEEs, requiredPackage);
      }
      return AccessRule.NON_ACCESSIBLE;
   }

}
