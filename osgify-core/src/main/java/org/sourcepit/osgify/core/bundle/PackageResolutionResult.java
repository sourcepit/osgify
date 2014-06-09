/**
 * Copyright (c) 2014 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.bundle;

import java.util.Collection;

public class PackageResolutionResult
{
   private String requiredPackage;

   private PackageOffer selectedOffer;

   private AccessModifier accessRule;

   private Collection<PackageOffer> offers;

   public PackageResolutionResult(String requiredPackage, PackageOffer selectedOffer, AccessModifier accessRule,
      Collection<PackageOffer> offers)
   {
      this.requiredPackage = requiredPackage;
      this.selectedOffer = selectedOffer;
      this.accessRule = accessRule;
      this.offers = offers;
   }

   public String getRequiredPackage()
   {
      return requiredPackage;
   }

   public PackageOffer getSelectedOffer()
   {
      return selectedOffer;
   }

   public AccessModifier getAccessRule()
   {
      return accessRule;
   }

   public Collection<PackageOffer> getOffers()
   {
      return offers;
   }
}