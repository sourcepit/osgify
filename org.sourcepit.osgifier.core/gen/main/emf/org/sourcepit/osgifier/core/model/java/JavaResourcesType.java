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

package org.sourcepit.osgifier.core.model.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Java Resources Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * 
 * @see org.sourcepit.osgifier.core.model.java.JavaModelPackage#getJavaResourcesType()
 * @model
 * @generated
 */
public enum JavaResourcesType implements Enumerator {
   /**
    * The '<em><b>BIN</b></em>' literal object.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @see #BIN_VALUE
    * @generated
    * @ordered
    */
   BIN(0, "BIN", "BIN"),

   /**
    * The '<em><b>SRC</b></em>' literal object.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @see #SRC_VALUE
    * @generated
    * @ordered
    */
   SRC(1, "SRC", "SRC");

   /**
    * The '<em><b>BIN</b></em>' literal value.
    * <!-- begin-user-doc -->
    * <p>
    * If the meaning of '<em><b>BIN</b></em>' literal object isn't clear, there really should be more of a description
    * here...
    * </p>
    * <!-- end-user-doc -->
    * 
    * @see #BIN
    * @model
    * @generated
    * @ordered
    */
   public static final int BIN_VALUE = 0;

   /**
    * The '<em><b>SRC</b></em>' literal value.
    * <!-- begin-user-doc -->
    * <p>
    * If the meaning of '<em><b>SRC</b></em>' literal object isn't clear, there really should be more of a description
    * here...
    * </p>
    * <!-- end-user-doc -->
    * 
    * @see #SRC
    * @model
    * @generated
    * @ordered
    */
   public static final int SRC_VALUE = 1;

   /**
    * An array of all the '<em><b>Java Resources Type</b></em>' enumerators.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   private static final JavaResourcesType[] VALUES_ARRAY = new JavaResourcesType[] { BIN, SRC, };

   /**
    * A public read-only list of all the '<em><b>Java Resources Type</b></em>' enumerators.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public static final List<JavaResourcesType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

   /**
    * Returns the '<em><b>Java Resources Type</b></em>' literal with the specified literal value.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public static JavaResourcesType get(String literal) {
      for (int i = 0; i < VALUES_ARRAY.length; ++i) {
         JavaResourcesType result = VALUES_ARRAY[i];
         if (result.toString().equals(literal)) {
            return result;
         }
      }
      return null;
   }

   /**
    * Returns the '<em><b>Java Resources Type</b></em>' literal with the specified name.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public static JavaResourcesType getByName(String name) {
      for (int i = 0; i < VALUES_ARRAY.length; ++i) {
         JavaResourcesType result = VALUES_ARRAY[i];
         if (result.getName().equals(name)) {
            return result;
         }
      }
      return null;
   }

   /**
    * Returns the '<em><b>Java Resources Type</b></em>' literal with the specified integer value.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public static JavaResourcesType get(int value) {
      switch (value) {
         case BIN_VALUE :
            return BIN;
         case SRC_VALUE :
            return SRC;
      }
      return null;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   private final int value;

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   private final String name;

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   private final String literal;

   /**
    * Only this class can construct instances.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   private JavaResourcesType(int value, String name, String literal) {
      this.value = value;
      this.name = name;
      this.literal = literal;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public int getValue() {
      return value;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public String getName() {
      return name;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   public String getLiteral() {
      return literal;
   }

   /**
    * Returns the literal value of the enumerator, which is its string representation.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * 
    * @generated
    */
   @Override
   public String toString() {
      return literal;
   }

} // JavaResourcesType
