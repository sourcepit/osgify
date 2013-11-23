/**
 * Copyright (c) 2013 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.modularizor.maven;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.manifest.osgi.BundleManifestFactory;
import org.sourcepit.common.maven.model.MavenArtifact;
import org.sourcepit.common.maven.model.MavenModelFactory;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;
import org.sourcepit.modularizor.core.model.context.BundleCandidate;
import org.sourcepit.modularizor.core.model.context.ContextModelFactory;

public class ManifestGeneratorFilterTest
{

   @Test
   public void testIsOverrideNativeBundle()
   {
      BundleCandidate bundle = ContextModelFactory.eINSTANCE.createBundleCandidate();

      BundleManifest manifest = BundleManifestFactory.eINSTANCE.createBundleManifest();
      manifest.setBundleSymbolicName("foo");

      PropertiesMap options = new LinkedPropertiesMap();

      ManifestGeneratorFilter filter = new ManifestGeneratorFilter();

      boolean override = filter.isOverrideNativeBundle(bundle, manifest, options);
      assertFalse(override);

      options.put("osgifier.overrideNativeBundles", "foo");
      override = filter.isOverrideNativeBundle(bundle, manifest, options);
      assertTrue(override);

      options.put("osgifier.overrideNativeBundles", "false");
      override = filter.isOverrideNativeBundle(bundle, manifest, options);
      assertFalse(override);

      options.put("osgifier.overrideNativeBundles", "true");
      override = filter.isOverrideNativeBundle(bundle, manifest, options);
      assertTrue(override);

      MavenArtifact artifact = MavenModelFactory.eINSTANCE.createMavenArtifact();
      artifact.setGroupId("foo");
      artifact.setArtifactId("foo");
      artifact.setVersion("1");

      bundle.addExtension(artifact);

      options.put("osgifier.overrideNativeBundles", "foo:foo:jar");
      override = filter.isOverrideNativeBundle(bundle, manifest, options);
      assertTrue(override);
   }

}
