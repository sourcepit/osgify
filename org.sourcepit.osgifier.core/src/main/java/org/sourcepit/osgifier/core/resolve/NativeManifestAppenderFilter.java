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

package org.sourcepit.osgifier.core.resolve;

import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.utils.props.PropertiesSource;
import org.sourcepit.osgifier.core.model.context.BundleCandidate;

public interface NativeManifestAppenderFilter {
   static NativeManifestAppenderFilter APPEND_ALL = new NativeManifestAppenderFilter() {
      @Override
      public boolean isAppendNativeManifest(BundleCandidate bundle, BundleManifest manifest, PropertiesSource options) {
         return true;
      }
   };

   boolean isAppendNativeManifest(BundleCandidate bundle, BundleManifest manifest, PropertiesSource options);
}
