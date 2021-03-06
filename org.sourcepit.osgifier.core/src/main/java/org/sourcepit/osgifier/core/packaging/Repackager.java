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

package org.sourcepit.osgifier.core.packaging;

import static org.sourcepit.common.utils.file.FileUtils.deleteFileOrDirectory;
import static org.sourcepit.common.utils.io.IO.buffIn;
import static org.sourcepit.common.utils.io.IO.buffOut;
import static org.sourcepit.common.utils.io.IO.fileIn;
import static org.sourcepit.common.utils.io.IO.fileOut;
import static org.sourcepit.common.utils.io.IO.jarIn;
import static org.sourcepit.common.utils.io.IO.jarOut;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.common.manifest.Manifest;
import org.sourcepit.common.manifest.osgi.resource.GenericManifestResourceImpl;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.lang.PipedIOException;
import org.sourcepit.common.utils.path.PathMatcher;
import org.sourcepit.osgifier.core.model.context.BundleLocalization;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
@Named
public class Repackager {
   private static final PathMatcher DEFAULT_CONTENT_MATCHER = createJarContentMatcher(null);

   private final Logger logger = LoggerFactory.getLogger(Repackager.class);

   public void injectManifest(final File jarFile, final Manifest manifest, final BundleLocalization localization)
      throws PipedIOException {
      try {
         final File tmpFile = move(jarFile);
         copyJarAndInjectManifest(tmpFile, jarFile, manifest, localization);
         deleteFileOrDirectory(tmpFile);
      }
      catch (IOException e) {
         throw Exceptions.pipe(e);
      }
   }

   private File move(final File srcFile) throws IOException {
      String prefix = ".rejar";
      String dirName = srcFile.getParentFile().getAbsolutePath();
      String fileName = srcFile.getName();

      File destFile = createTmpFile(dirName, fileName, prefix);
      final boolean rename = srcFile.renameTo(destFile);
      if (!rename) {
         org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
         try {
            deleteFileOrDirectory(srcFile);
         }
         catch (IOException e) {
            FileUtils.deleteQuietly(destFile);
            throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
         }
      }
      return destFile;
   }

   private static File createTmpFile(String dirName, String fileName, String prefix) throws IOException {
      int unique = 0;
      File file;
      do {
         file = genFile(dirName, fileName, prefix, unique++);
      }
      while (!file.createNewFile());

      return file;
   }

   private static File genFile(String dirName, String fileName, String prefix, int counter) {
      StringBuilder path = new StringBuilder();
      path.append(dirName);
      path.append(File.separatorChar);
      path.append(fileName);
      if (counter > 0) {
         final String n = String.valueOf(counter);
         int lead = 5 - n.length();
         for (int i = 0; i < lead; i++) {
            path.append('0');
         }
         path.append(n);
      }
      path.append(prefix);
      return new File(path.toString());
   }

   public void copyJarAndInjectManifest(final File srcJarFile, final File destJarFile, final Manifest manifest,
      final BundleLocalization localization) throws PipedIOException {
      copyJarAndInjectManifest(srcJarFile, destJarFile, manifest, localization, null);
   }

   public <T> void copyJarAndInjectManifest(final File srcJarFile, final File destJarFile, final Manifest manifest,
      final BundleLocalization localization, final Collection<String> pathFilters) throws PipedIOException {
      new IOOperation<JarOutputStream>(jarOut(buffOut(fileOut(destJarFile)))) {
         @Override
         protected void run(JarOutputStream destJarOut) throws IOException {
            rePackageJarFile(srcJarFile, manifest, localization, destJarOut, pathFilters);
         }
      }.run();
   }

   private void rePackageJarFile(File srcJarFile, final Manifest manifest, BundleLocalization localization,
      final JarOutputStream destJarOut, Collection<String> pathFilters) throws IOException {
      destJarOut.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
      writeManifest(manifest, destJarOut);
      destJarOut.closeEntry();

      if (localization != null) {
         final Set<String> paths = BundleLocalizationWriter.write(destJarOut, manifest, localization).keySet();
         pathFilters = pathFilters == null ? new HashSet<String>() : new HashSet<String>(pathFilters);
         for (String path : paths) {
            pathFilters.add("!" + path);
         }
      }

      final PathMatcher pathMatcher = pathFilters == null
         ? DEFAULT_CONTENT_MATCHER
         : createJarContentMatcher(pathFilters);

      new IOOperation<JarInputStream>(jarIn(buffIn(fileIn(srcJarFile)))) {
         @Override
         protected void run(JarInputStream srcJarIn) throws IOException {
            copyJarContents(srcJarIn, destJarOut, pathMatcher);
         }
      }.run();
   }

   private void writeManifest(Manifest manifest, OutputStream out) throws IOException {
      Resource manifestResource = new GenericManifestResourceImpl();
      manifestResource.getContents().add(EcoreUtil.copy(manifest));
      manifestResource.save(out, null);
   }

   private void copyJarContents(JarInputStream srcJarIn, final JarOutputStream destJarOut, PathMatcher contentMatcher)
      throws IOException {
      final Set<String> processedEntires = new HashSet<String>();

      JarEntry srcEntry = srcJarIn.getNextJarEntry();
      while (srcEntry != null) {
         final String entryName = srcEntry.getName();
         if (contentMatcher.isMatch(entryName)) {
            if (processedEntires.add(entryName)) {
               destJarOut.putNextEntry(new JarEntry(srcEntry.getName()));
               IOUtils.copy(srcJarIn, destJarOut);
               destJarOut.closeEntry();
            }
            else {
               logger.warn("Ignored duplicate jar entry: " + entryName);
            }
         }
         srcJarIn.closeEntry();
         srcEntry = srcJarIn.getNextJarEntry();
      }
   }

   private static PathMatcher createJarContentMatcher(Collection<String> customPathFilters) {
      final Set<String> pathFilters = new HashSet<String>();
      pathFilters.add("!" + JarFile.MANIFEST_NAME); // will be set manually
      pathFilters.add("!META-INF/*.SF");
      pathFilters.add("!META-INF/*.DSA");
      pathFilters.add("!META-INF/*.RSA");

      if (customPathFilters != null) {
         pathFilters.addAll(customPathFilters);
      }

      final String matcherPattern = toPathMatcherPattern(pathFilters);

      return PathMatcher.parse(matcherPattern, "/", ",");
   }

   private static String toPathMatcherPattern(Set<String> pathFilters) {
      final StringBuilder sb = new StringBuilder();
      for (String exclude : pathFilters) {
         sb.append(exclude);
         sb.append(',');
      }
      if (sb.length() > 0) {
         sb.deleteCharAt(sb.length() - 1);
      }
      return sb.toString();
   }
}
