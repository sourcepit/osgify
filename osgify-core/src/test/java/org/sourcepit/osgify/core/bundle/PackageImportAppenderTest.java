/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.bundle;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.sourcepit.common.manifest.osgi.BundleHeaderName.IMPORT_PACKAGE;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.addPackageExport;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.appendPackageExport;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.appendTypeWithReferences;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.newBundleCandidate;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.newPackageExport;
import static org.sourcepit.osgify.core.bundle.TestContextHelper.setInternal;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.emf.common.util.EList;
import org.eclipse.sisu.launch.InjectedTest;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.sourcepit.common.manifest.osgi.PackageImport;
import org.sourcepit.common.manifest.osgi.VersionRange;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;
import org.sourcepit.osgify.core.model.context.BundleCandidate;
import org.sourcepit.osgify.core.model.context.BundleReference;
import org.sourcepit.osgify.core.model.context.ContextModelFactory;
import org.sourcepit.osgify.core.model.java.JavaArchive;
import org.sourcepit.osgify.core.model.java.JavaModelFactory;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public class PackageImportAppenderTest extends InjectedTest
{
   @Inject
   private PackageImportAppender importAppender;

   @Test
   public void testSortAlphabetically()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      appendPackageExport(ref, newPackageExport("a3", null));

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "z.Bar", "a3.Bar");
      appendTypeWithReferences(jArchive, "z.Bar", 47, "a2.Bar");
      appendTypeWithReferences(jArchive, "a2.Bar", 47, "a.Bar");

      BundleCandidate bundle = newBundleCandidate("1.0.1", "OSGi/Minimum-1.0", jArchive);
      bundle.getDependencies().add(ref);
      appendPackageExport(bundle, newPackageExport("z", null));
      appendPackageExport(bundle, newPackageExport("a2", null));
      appendPackageExport(bundle, newPackageExport("a", null));

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("a,a2,a3,z"));
   }

   @Test
   public void testImportReferencedPackages()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "org.sourcepit.Foo", 47, "foo.Bar");
      BundleCandidate bundle = newBundleCandidate("1.0.1", "OSGi/Minimum-1.0", jArchive);

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      appendPackageExport(ref, newPackageExport("foo", "1.2.3"));

      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[1.2,2)\""));

      options.put("osgifier.eraseMicro", "false");

      bundle.getManifest().setHeader(IMPORT_PACKAGE, null);

      importAppender.append(bundle, options);

      packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[1.2.3,2)\""));
   }

   @Test
   public void testFilterPlatformPackages()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      appendPackageExport(ref, newPackageExport("b", null));

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "javax.microedition.io.Foo", "b.Foo");

      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("b"));
   }

   @Test
   public void testIgnoreUnresolveablePackages()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "b.Foo");

      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, nullValue());
   }

   @Test
   public void testEquivalentVersionsForPublicSelfImports()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "b.Foo");

      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);
      appendPackageExport(bundle, newPackageExport("b", "2"));

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("b;version=\"[2,2.1)\""));
   }

   @Test
   public void testDefaultVersionIfPackageIsExportedWithDefaultVersion()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "b.Foo", "foo.Foo");

      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);
      appendPackageExport(bundle, newPackageExport("b", null));

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      appendPackageExport(ref, newPackageExport("foo", null));

      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);

      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("b,foo"));
   }

   @Test
   public void testIgnorePlatformFamilyPackages()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "javax.xml.stream.Foo", "sun.misc.Foo");
      BundleCandidate bundle = newBundleCandidate("1.0.1", "JavaSE-1.6", jArchive);
      importAppender.append(bundle, options);
      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertNull(packageImports);
   }

   @Test
   public void testUseVersionRangeOfReference()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "foo.Foo");

      // test package version fits into version range of reference
      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("[1,2)"));
      appendPackageExport(ref, newPackageExport("foo", "1.1"));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[1,2)\""));

      // test package version doesn't fit into version range of reference
      bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("[1,2)"));
      appendPackageExport(ref, newPackageExport("foo", "3"));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[3,4)\""));

      // test package version is default version
      bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("[1,2)"));
      appendPackageExport(ref, newPackageExport("foo", "0.0.0"));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo"));

      // test package version is default version (null)
      bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("[1,2)"));
      appendPackageExport(ref, newPackageExport("foo", null));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo"));

      // test ref version isn't a "real" range
      bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("1"));
      appendPackageExport(ref, newPackageExport("foo", "2"));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[2,3)\"")); // unbounded version ranges of package refs
                                                                            // are ignored
   }

   @Test
   public void testOptionalReference()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "foo.Foo");

      // test package version fits into version range of reference
      BundleCandidate bundle = newBundleCandidate("1.0.1", "CDC-1.0/Foundation-1.0", jArchive);

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setOptional(true);
      appendPackageExport(ref, newPackageExport("foo", null));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;resolution:=optional"));
   }

   @Test
   public void testIgnoreUnboundedVersionRange()
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "a.Bar", 47, "foo.Foo");

      BundleCandidate bundle = newBundleCandidate("999", "CDC-1.0/Foundation-1.0", jArchive);

      BundleReference ref = ContextModelFactory.eINSTANCE.createBundleReference();
      ref.setVersionRange(VersionRange.parse("1"));
      appendPackageExport(ref, newPackageExport("foo", "2"));
      bundle.getDependencies().add(ref);

      importAppender.append(bundle, options);
      String packageImports = bundle.getManifest().getHeaderValue(IMPORT_PACKAGE);
      assertThat(packageImports, IsEqual.equalTo("foo;version=\"[2,3)\""));
   }

   @Test
   public void testImportOwnExports() throws Exception
   {
      PropertiesMap options = new LinkedPropertiesMap();

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "javax.xml.stream.XMLStreamWriter", 47, "javax.xml.namespace.QName");

      BundleCandidate bundle = newBundleCandidate("1.0.1", null, jArchive);
      bundle.getManifest().setBundleSymbolicName("stax.api");
      bundle.setSymbolicName("stax.api");

      appendPackageExport(bundle, newPackageExport("javax.xml.namespace", null));
      appendPackageExport(bundle, newPackageExport("javax.xml.stream", null));

      importAppender.append(bundle, options);

      EList<PackageImport> importPackage = bundle.getManifest().getImportPackage();
      assertEquals(2, importPackage.size());

      PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals(packageImport.getPackageNames().get(0), "javax.xml.namespace");

      packageImport = importPackage.get(1);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals(packageImport.getPackageNames().get(0), "javax.xml.stream");
   }

   @Test
   public void testVersionErasureOfKnownEEPackages() throws Exception
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "javax.activation", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "javax.activation.MimeType");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setOptional(true);
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate("1", "J2SE-1.4", jArchive);
      bundleCandidate.getDependencies().add(reference);

      PropertiesMap options = new LinkedPropertiesMap();
      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      final PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("javax.activation", packageImport.getPackageNames().get(0));
      assertNull(packageImport.getVersion());
      assertEquals("optional", packageImport.getParameterValue("resolution"));
   }

   @Test
   public void testVersionErasureOfKnownVendorPackages() throws Exception
   {
      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "com.sun.javafx.event", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "com.sun.javafx.event.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setOptional(true);
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate("1", "J2SE-1.4", jArchive);
      bundleCandidate.getDependencies().add(reference);

      PropertiesMap options = new LinkedPropertiesMap();
      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      final PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("com.sun.javafx.event", packageImport.getPackageNames().get(0));
      assertNull(packageImport.getVersion());
      assertEquals("optional", packageImport.getParameterValue("resolution"));
   }

   @Test
   public void testCustomVersionRangePolicyForPublicImports() throws Exception
   {
      PropertiesMap options = new LinkedPropertiesMap();
      options.put("osgifier.publicImportPolicy", VersionRangePolicy.STRICT.literal());

      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "required.package", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      final PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("required.package", packageImport.getPackageNames().get(0));
      assertEquals("[1.2.3,1.2.4)", packageImport.getVersion().toString());
   }

   @Test
   public void testCustomVersionRangePolicyForInternalImports() throws Exception
   {
      PropertiesMap options = new LinkedPropertiesMap();
      options.put("osgifier.internalImportPolicy", VersionRangePolicy.GREATER_OR_EQUAL.literal());

      BundleCandidate requiredBundle = newBundleCandidate(null);
      setInternal(addPackageExport(requiredBundle, "required.package", "1.2.3"));

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      final PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("required.package", packageImport.getPackageNames().get(0));
      // 1.2 because of option osgifier.eraseMicro = true
      assertEquals("1.2", packageImport.getVersion().toString());
   }

   @Test
   public void testCustomVersionRangePolicyForSelfImports() throws Exception
   {
      PropertiesMap options = new LinkedPropertiesMap();
      options.put("osgifier.selfImportPolicy", VersionRangePolicy.ANY.literal());

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      setInternal(addPackageExport(bundleCandidate, "required.package", "1.2.3"));

      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      final PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("required.package", packageImport.getPackageNames().get(0));
      // 1.2 because of option osgifier.eraseMicro = true
      assertNull(packageImport.getVersion());
   }

   @Test
   public void testEraseMicro() throws Exception
   {
      PropertiesMap options = new LinkedPropertiesMap();

      BundleCandidate requiredBundle = newBundleCandidate(null);
      addPackageExport(requiredBundle, "required.package", "1.2.3");

      JavaArchive jArchive = JavaModelFactory.eINSTANCE.createJavaArchive();
      appendTypeWithReferences(jArchive, "bundle.package", 47, "required.package.Foo");

      BundleReference reference = ContextModelFactory.eINSTANCE.createBundleReference();
      reference.setTarget(requiredBundle);

      BundleCandidate bundleCandidate = newBundleCandidate(jArchive);
      bundleCandidate.getDependencies().add(reference);

      importAppender.append(bundleCandidate, options);

      List<PackageImport> importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      PackageImport packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("required.package", packageImport.getPackageNames().get(0));
      assertEquals("[1.2,2)", packageImport.getVersion().toString());

      options.setBoolean("osgifier.eraseMicro", false);
      bundleCandidate.getManifest().setImportPackage(null);
      importAppender.append(bundleCandidate, options);

      importPackage = bundleCandidate.getManifest().getImportPackage();
      assertEquals(1, importPackage.size());

      packageImport = importPackage.get(0);
      assertEquals(1, packageImport.getPackageNames().size());
      assertEquals("required.package", packageImport.getPackageNames().get(0));
      assertEquals("[1.2.3,2)", packageImport.getVersion().toString());
   }

}
