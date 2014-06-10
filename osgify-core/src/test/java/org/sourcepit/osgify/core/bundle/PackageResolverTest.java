/**
 * Copyright (c) 2014 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.addPackageExport;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.appendTypeWithReferences;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.newBundleCandidate;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.setInternal;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;
import org.sourcepit.osgify.core.model.context.BundleCandidate;
import org.sourcepit.osgify.core.model.context.BundleReference;
import org.sourcepit.osgify.core.model.context.ContextModelFactory;
import org.sourcepit.osgify.core.model.java.JavaArchive;
import org.sourcepit.osgify.core.model.java.JavaModelFactory;

public class PackageResolverTest extends InjectedTest
{
   @Inject
   private PackageResolver packageResolver;

   @Test
   public void testImportPublicPackage()
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "required.package", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(1, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("required.package", result.getRequiredPackage());
      assertEquals(AccessRestriction.NONE, result.getAccessRestriction());
      assertEquals(1, result.getExporters().size());
      assertEquals(requiredBundle, result.getSelectedExporter().getBundle());
      assertEquals(result.getSelectedExporter(), result.getExporters().get(0));
      assertEquals(reference, result.getSelectedExporter().getBundleReference());
   }

   @Test
   public void testImportInternalPackage()
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      setInternal(addPackageExport(requiredBundle, "required.package", "1.2.3"));

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(1, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("required.package", result.getRequiredPackage());
      assertEquals(AccessRestriction.DISCOURAGED, result.getAccessRestriction());
      assertEquals(1, result.getExporters().size());
      assertEquals(requiredBundle, result.getSelectedExporter().getBundle());
      assertEquals(result.getSelectedExporter(), result.getExporters().get(0));
      assertEquals(reference, result.getSelectedExporter().getBundleReference());
   }

   @Test
   public void testImportInternalAndPublicPackageFromSameBundle()
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "required.package", "1.2.3");
      setInternal(addPackageExport(requiredBundle, "required.internal.package", "1.2.3"));

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo",
         "required.internal.package.FooImpl");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(2, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("required.internal.package", result.getRequiredPackage());
      assertEquals(AccessRestriction.DISCOURAGED, result.getAccessRestriction());
      assertEquals(1, result.getExporters().size());
      assertEquals(requiredBundle, result.getSelectedExporter().getBundle());
      assertEquals(result.getSelectedExporter(), result.getExporters().get(0));
      assertEquals(reference, result.getSelectedExporter().getBundleReference());

      result = results.get(1);
      assertEquals("required.package", result.getRequiredPackage());
      assertEquals(AccessRestriction.DISCOURAGED, result.getAccessRestriction());
      assertEquals(1, result.getExporters().size());
      assertEquals(requiredBundle, result.getSelectedExporter().getBundle());
      assertEquals(result.getSelectedExporter(), result.getExporters().get(0));
      assertEquals(reference, result.getSelectedExporter().getBundleReference());
   }

   @Test
   public void testPlatformPackage() throws Exception
   {
      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "java.lang.Object");

      BundleCandidate bundleCandidate = newBundleCandidate("1", "JavaSE-1.6", jArchive);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(1, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("java.lang", result.getRequiredPackage());
      assertEquals(AccessRestriction.NONE, result.getAccessRestriction());
      assertEquals(1, result.getExporters().size());

      final PackageExportDescription selectedExporter = result.getSelectedExporter();
      assertEquals(selectedExporter, result.getExporters().get(0));

      assertEquals(PackageExporterType.EXECUTION_ENVIRONMENT, selectedExporter.getExporterType());
      assertNull(selectedExporter.getBundle());
      assertNull(selectedExporter.getBundleReference());
   }

   @Test
   public void testResolutionOrderSelfPlatformDependencies() throws Exception
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "javax.activation", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "javax.activation.MimeType", 47, "javax.activation.MimeTypeParameterList");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate("1", "JavaSE-1.6", jArchive);
      addPackageExport(bundleCandidate, "javax.activation", "1");
      bundleCandidate.getDependencies().add(reference);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(1, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("javax.activation", result.getRequiredPackage());
      assertEquals(AccessRestriction.NONE, result.getAccessRestriction());
      assertEquals(3, result.getExporters().size());

      final PackageExportDescription selectedExporter = result.getSelectedExporter();
      assertEquals(selectedExporter, result.getExporters().get(0));

      assertEquals(PackageExporterType.OWN_BUNDLE, selectedExporter.getExporterType());
      assertEquals(bundleCandidate, selectedExporter.getBundle());
      assertNull(selectedExporter.getBundleReference());

      PackageExportDescription exporter = result.getExporters().get(1);
      assertEquals(PackageExporterType.EXECUTION_ENVIRONMENT, exporter.getExporterType());
      assertNull(exporter.getBundle());
      assertNull(exporter.getBundleReference());

      exporter = result.getExporters().get(2);
      assertEquals(PackageExporterType.REQUIRED_BUNDLE, exporter.getExporterType());
      assertEquals(requiredBundle, exporter.getBundle());
      assertEquals(reference, exporter.getBundleReference());
   }

   @Test
   public void testReferenceToDefaultPackage() throws Exception
   {
      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.Foo", 47, "Bar");

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(0, results.size());
   }

   @Test
   public void testUnresolvableImport() throws Exception
   {
      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "foo.Bar", 47, "bar.Foo");

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);

      List<PackageResolutionResult> results = packageResolver.resolveRequiredPackages(bundleCandidate, false);
      assertEquals(1, results.size());

      PackageResolutionResult result = results.get(0);
      assertEquals("bar", result.getRequiredPackage());
      assertNull(result.getAccessRestriction());
      assertNull(result.getSelectedExporter());
      assertEquals(0, result.getExporters().size());
   }

}
