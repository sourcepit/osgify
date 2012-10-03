/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgify.core.model.java;

import org.sourcepit.osgify.core.java.internal.impl.JavaElementOperations;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public aspect JavaElementAspects
{
   pointcut getResourceBundle(JavaElement jElement) : target(jElement) && execution(JavaResourceBundle JavaElement.getResourceBundle());

   JavaResourceBundle around(JavaElement jElement) : getResourceBundle(jElement){

      return JavaElementOperations.getResourceBundle(jElement);
   }
}
