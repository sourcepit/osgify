/**
 * Copyright (c) 2013 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.maven;

import static com.google.common.base.Preconditions.checkState;
import static org.sourcepit.common.utils.io.IO.osgiIn;
import static org.sourcepit.common.utils.io.IO.read;
import static org.sourcepit.common.utils.lang.Exceptions.pipe;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.sourcepit.common.manifest.Manifest;
import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.manifest.osgi.BundleManifestFactory;
import org.sourcepit.common.manifest.osgi.Version;
import org.sourcepit.common.manifest.osgi.resource.GenericManifestResourceImpl;
import org.sourcepit.common.maven.model.MavenArtifact;
import org.sourcepit.common.maven.model.MavenDependency;
import org.sourcepit.common.maven.model.Scope;
import org.sourcepit.common.utils.io.Read.FromStream;
import org.sourcepit.common.utils.lang.PipedException;
import org.sourcepit.common.utils.props.PropertiesSource;
import org.sourcepit.maven.dependency.model.ArtifactAttachmentFactory;
import org.sourcepit.maven.dependency.model.DependencyModel;
import org.sourcepit.maven.dependency.model.DependencyModelResolver;
import org.sourcepit.maven.dependency.model.DependencyNode;
import org.sourcepit.maven.dependency.model.DependencyTree;
import org.sourcepit.maven.dependency.model.JavaSourceAttachmentFactory;
import org.sourcepit.osgify.core.bundle.DynamicPackageImportAppender;
import org.sourcepit.osgify.core.bundle.PackageExportAppender;
import org.sourcepit.osgify.core.bundle.PackageImportAppender;
import org.sourcepit.osgify.core.bundle.RequiredExecutionEnvironmentAppender;
import org.sourcepit.osgify.core.java.inspect.ClassForNameDetector;
import org.sourcepit.osgify.core.java.inspect.IJavaTypeAnalyzer;
import org.sourcepit.osgify.core.java.inspect.JavaResourcesBundleScanner;
import org.sourcepit.osgify.core.java.inspect.JavaTypeReferencesAnalyzer;
import org.sourcepit.osgify.core.model.context.BundleCandidate;
import org.sourcepit.osgify.core.model.context.BundleReference;
import org.sourcepit.osgify.core.model.context.OsgifyContext;
import org.sourcepit.osgify.core.resolve.SymbolicNameResolver;
import org.sourcepit.osgify.core.resolve.VersionRangeResolver;
import org.sourcepit.osgify.core.resolve.VersionResolver;
import org.sourcepit.osgify.core.util.OsgifyContextUtils;
import org.sourcepit.osgify.maven.impl.OsgifyStubModelCreator;

@Named
public class OsgifyModelBuilder
{
   @Inject
   private DependencyModelResolver dependencyModelResolver;

   @Inject
   private OsgifyStubModelCreator stubModelCreator;

   public static class Result
   {
      public DependencyModel dependencyModel;
      public OsgifyContext osgifyModel;
   }

   public Result build(PropertiesSource properties, Collection<Dependency> dependencies)
   {
      final DependencyModel dependencyModel = resolve(dependencies);

      final OsgifyContext osgifyModel = createStubModel(dependencyModel);
      applyNativeBundles(osgifyModel);
      applyBuildOrder(osgifyModel);
      applyJavaContent(osgifyModel);
      applyManifests(properties, osgifyModel);

      final Result result = new Result();
      result.dependencyModel = dependencyModel;
      result.osgifyModel = osgifyModel;
      return result;
   }

   private void applyBuildOrder(final OsgifyContext osgifyModel)
   {
      final EList<BundleCandidate> bundles = osgifyModel.getBundles();
      final List<BundleCandidate> orderedBundles = OsgifyContextUtils.computeBuildOrder(osgifyModel);
      for (int i = 0; i < orderedBundles.size(); i++)
      {
         bundles.move(i, orderedBundles.get(i));
      }
   }


   private DependencyModel resolve(Collection<Dependency> dependencies)
   {
      final DependencyModel dependencyModel;

      final boolean includeSource = true;

      final ArtifactAttachmentFactory attachmentFactory = includeSource ? new JavaSourceAttachmentFactory() : null;

      try
      {
         dependencyModel = dependencyModelResolver.resolve(dependencies, attachmentFactory);
      }
      catch (ProjectBuildingException e)
      {
         throw pipe(e);
      }
      catch (DependencyResolutionException e)
      {
         throw pipe(e);
      }

      final Iterator<MavenArtifact> it = dependencyModel.getArtifacts().iterator();
      while (it.hasNext())
      {
         MavenArtifact mavenArtifact = (MavenArtifact) it.next();
         if (mavenArtifact.getFile() == null)
         {
            DependencyTree dependencyTree = dependencyModel.getDependencyTree(mavenArtifact);
            if (dependencyTree != null)
            {
               dependencyTree.setArtifact(null);
            }
            // dependencyModel.getDependencyTrees().remove(dependencyTree);

            for (DependencyTree tree : dependencyModel.getDependencyTrees())
            {
               TreeIterator<EObject> eAllContents = tree.eAllContents();
               while (eAllContents.hasNext())
               {
                  EObject eObject = (EObject) eAllContents.next();
                  if (eObject instanceof DependencyNode)
                  {
                     DependencyNode node = (DependencyNode) eObject;
                     if (node.getArtifact() == mavenArtifact)
                     {
                        node.setArtifact(null);
                        node.setSelected(false);
                     }
                  }
               }
            }

            it.remove();
            System.out.println("Removed " + mavenArtifact.getArtifactKey());
         }
      }
      return dependencyModel;
   }

   @Inject
   private SymbolicNameResolver symbolicNameResolver;

   @Inject
   private VersionResolver versionResolver;

   @Inject
   private VersionRangeResolver versionRangeResolver;

   @Inject
   private RequiredExecutionEnvironmentAppender environmentAppender;

   @Inject
   private PackageExportAppender packageExports;

   @Inject
   private DynamicPackageImportAppender dynamicImports;

   @Inject
   private PackageImportAppender packageImports;

   private void applyManifests(PropertiesSource properties, OsgifyContext osgifyModel)
   {
      for (BundleCandidate bundle : osgifyModel.getBundles())
      {
         if (isGenerateManifest(bundle))
         {
            final BundleManifest manifest = BundleManifestFactory.eINSTANCE.createBundleManifest();
            bundle.setManifest(manifest);

            final String symbolicName = symbolicNameResolver.resolveSymbolicName(bundle);
            checkState(symbolicName != null, "Failed to determine bundle symbolic name for %s", bundle.getLocation());
            manifest.setBundleSymbolicName(symbolicName);
            bundle.setSymbolicName(symbolicName);

            final Version version = versionResolver.resolveVersion(bundle);
            checkState(version != null, "Failed to determine bundle version for %s", bundle.getLocation());
            manifest.setBundleVersion(version);
            bundle.setVersion(version);

            for (BundleReference reference : bundle.getDependencies())
            {
               // TODO move elsewhere
               reference.setVersionRange(versionRangeResolver.resolveVersionRange(reference));
               final MavenDependency mavenDependency = reference.getExtension(MavenDependency.class);
               if (mavenDependency != null)
               {
                  reference.setOptional(mavenDependency.isOptional());
                  reference.setProvided(mavenDependency.getScope() == Scope.PROVIDED);
               }
            }

            if (isSourceBundle(bundle))
            {
               applySourceBundles(bundle);
            }
            else
            {
               environmentAppender.append(bundle);
               packageExports.append(properties, bundle);
               packageImports.append(bundle);
               dynamicImports.append(bundle);
            }
         }
      }
   }

   private void applySourceBundles(BundleCandidate bundle)
   {
      final BundleCandidate targetBundle = bundle.getTargetBundle();
      final String symbolicName = targetBundle.getSymbolicName();

      BundleManifest manifest = bundle.getManifest();
      manifest.getBundleSymbolicName(true).setSymbolicName(symbolicName + ".source");
      bundle.setSymbolicName(symbolicName + ".source");

      // Eclipse-SourceBundle: com.ibm.icu;version="4.4.2.v20110823";roots:="."
      manifest.setHeader("Eclipse-SourceBundle",
         targetBundle.getSymbolicName() + ";version=\"" + targetBundle.getVersion() + "\";roots:=\".\"");
   }

   private void applyNativeBundles(final OsgifyContext osgifyModel)
   {
      for (BundleCandidate bundle : osgifyModel.getBundles())
      {
         if (bundle.getLocation() != null)
         {
            final FromStream<Manifest> fromStream = new FromStream<Manifest>()
            {
               @Override
               public Manifest read(InputStream inputStream) throws Exception
               {
                  final Resource resource = new GenericManifestResourceImpl();
                  resource.load(inputStream, null);
                  return (Manifest) resource.getContents().get(0);
               }
            };

            try
            {
               final Manifest m = read(fromStream, osgiIn(bundle.getLocation(), "META-INF/MANIFEST.MF"));
               if (m instanceof BundleManifest)
               {
                  BundleManifest manifest = (BundleManifest) m;
                  bundle.setNativeBundle(true);
                  bundle.setManifest(manifest);

                  // TODO deprecate! replace with operation
                  bundle.setSymbolicName(manifest.getBundleSymbolicName().getSymbolicName());
                  bundle.setVersion(manifest.getBundleVersion());
               }
            }
            catch (PipedException e)
            {
               // TODO log
               e.printStackTrace();
            }
         }
      }
   }

   private OsgifyContext createStubModel(final DependencyModel dependencyModel)
   {
      return stubModelCreator.create(dependencyModel);
   }

   private void applyJavaContent(final OsgifyContext osgifyModel)
   {
      // TODO projects?
      for (BundleCandidate bundle : osgifyModel.getBundles())
      {
         final File location = bundle.getLocation();
         if (location != null && location.isFile() && isScanBundle(bundle))
         {
            bundle.setContent(newScanner().scan(location));
         }
      }
   }

   private JavaResourcesBundleScanner newScanner()
   {
      final JavaResourcesBundleScanner scanner = new JavaResourcesBundleScanner();
      final List<IJavaTypeAnalyzer> typeAnalyzers = new ArrayList<IJavaTypeAnalyzer>();
      typeAnalyzers.add(new JavaTypeReferencesAnalyzer());
      typeAnalyzers.add(new ClassForNameDetector());
      scanner.setJavaTypeAnalyzer(typeAnalyzers);
      return scanner;
   }

   private boolean isGenerateManifest(BundleCandidate bundle)
   {
      return !bundle.isNativeBundle() || isOverrideNativeBundle(bundle);
   }

   private boolean isOverrideNativeBundle(BundleCandidate bundle)
   {
      return false;
   }

   private boolean isSourceBundle(BundleCandidate bundle)
   {
      return bundle.getTargetBundle() != null;
   }

   private boolean isScanBundle(BundleCandidate bundle)
   {
      return isGenerateManifest(bundle) && !isSourceBundle(bundle);
   }
}