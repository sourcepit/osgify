/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgifier.core.model.java;

import org.sourcepit.osgifier.core.java.internal.impl.JavaResourcesRootOperations;
import org.sourcepit.osgifier.core.model.java.JavaResourcesRoot;
import org.sourcepit.osgifier.core.model.java.JavaType;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public aspect JavaResourcesRootAspects
{
   pointcut getType(JavaResourcesRoot jRoot, String qualifiedPackageName, String typeName, boolean createOnDemand): target(jRoot) && args(qualifiedPackageName, typeName, createOnDemand) && execution(JavaType JavaResourcesRoot.getType(String, String, boolean));

   JavaType around(JavaResourcesRoot jRoot, String qualifiedPackageName, String typeName, boolean createOnDemand) : getType(jRoot, qualifiedPackageName, typeName, createOnDemand)
   {
      return JavaResourcesRootOperations.getType(jRoot, qualifiedPackageName, typeName, createOnDemand);
   }
}