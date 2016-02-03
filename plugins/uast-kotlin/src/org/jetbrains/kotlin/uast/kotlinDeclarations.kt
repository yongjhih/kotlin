/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import com.intellij.psi.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.uast.*
import org.jetbrains.uast.java.lz
import org.jetbrains.uast.psi.PsiElementBacked

private val MODIFIER_MAP = mapOf(
        UastModifier.ABSTRACT to KtTokens.ABSTRACT_KEYWORD,
        UastModifier.ABSTRACT to KtTokens.INNER_KEYWORD
)

object KotlinInternalUastVisibility : UastVisibility("internal")

private fun KtDeclaration.getVisibility() = when (visibilityModifierType()) {
    KtTokens.PRIVATE_KEYWORD -> UastVisibility.PRIVATE
    KtTokens.PROTECTED_KEYWORD -> UastVisibility.PROTECTED
    KtTokens.INTERNAL_KEYWORD -> KotlinInternalUastVisibility
    else -> UastVisibility.PUBLIC
}

private fun KtModifierListOwner.hasModifier(modifier: UastModifier): Boolean {
    val javaModifier = MODIFIER_MAP[modifier] ?: return false
    return hasModifier(javaModifier)
}

class KotlinUFile(override val psi: KtFile): UFile, PsiElementBacked {
    override val packageFqName by lz { psi.packageFqName.asString() }
    override val declarations by lz { psi.declarations.map { KotlinConverter.convert(it, this) }.filterNotNull() }
    override val importStatements by lz { psi.importDirectives.map { KotlinUImportStatement(this, it) } }
}

class KotlinUImportStatement(
        override val parent: UElement,
        override val psi: KtImportDirective
) : UImportStatement, PsiElementBacked {
    override val nameToImport = psi.importedFqName?.asString()
}

class KotlinUFakeIdentifier(
        override val parent: UElement,
        override val psi: PsiElement
) : UIdentifier, PsiElementBacked {
    override val name = psi.text
    override fun resolve(context: UastContext) = null
}

class KotlinUClass(
        override val parent: UElement,
        override val psi: KtClassOrObject
) : UClass, PsiElementBacked {
    override val name = psi.name + " (${psi.getSuperTypeListEntries().map { it.typeAsUserType?.referencedName }})"
    override val nameElement by lz { KotlinConverter.convert(psi.nameIdentifier, this) }

    override val fqName = psi.fqName?.asString()
    override val isEnum = (psi as? KtClass)?.isEnum() ?: false
    override val isInterface = (psi as? KtClass)?.isInterface() ?: false
    override val isObject = psi is KtObjectDeclaration

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)

    override val declarations by lz { psi.declarations.map { KotlinConverter.convert(it, this) }.filterNotNull() }

    override val superTypes by lz { psi.getSuperTypeListEntries().map { KotlinUType(it.typeReference?.typeElement) } }

    override val visibility = psi.getVisibility()

    override fun isSubclassOf(name: String): Boolean {
        return psi.getSuperTypeListEntries().any {
            val userType = it.typeAsUserType
            userType != null && userType.referencedName == name
        }
    }
}

class KotlinConstructorUFunction(
        override val parent: UElement,
        override val psi: KtConstructor<*>
) : UFunction, PsiElementBacked {
    override val valueParameterCount = psi.getValueParameters().size
    override val valueParameters by lz { psi.getValueParameters().map { KotlinUValueParameter(this, it) } }
    override val isConstructor = true
    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.getBodyExpression()) }
    override val nameElement by lz { psi.getConstructorKeyword()?.let { KotlinUFakeIdentifier(this, it) } }
    override val name = psi.getName()
    override val visibility = psi.getVisibility()
}

class KotlinUFunction(
        override val parent: UElement,
        override val psi: KtFunction
) : UFunction, PsiElementBacked {
    override val isConstructor = false
    override val valueParameters by lz { psi.valueParameters.map { KotlinUValueParameter(this, it) } }
    override val valueParameterCount = psi.valueParameters.size
    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.bodyExpression) }
    override val nameElement by lz { psi.nameIdentifier?.let { KotlinUFakeIdentifier(this, it) } }
    override val name = psi.name
    override val visibility = psi.getVisibility()
}

class KotlinUVariable(
        override val parent: UElement,
        override val psi: KtProperty
) : UVariable, PsiElementBacked {
    override val name = psi.name
    override val nameElement by lz { psi.nameIdentifier?.let { KotlinUFakeIdentifier(this, it) } }

    override val isProperty = psi is PsiField
    override val initializer by lz { KotlinConverter.convertOrEmpty(this, psi.initializer) }
    override val modifiers = emptyList<UastModifier>()
}

class KotlinUType(val psi: KtTypeElement?) : UClassType {
    constructor(ref: KtTypeReference?) : this(ref?.typeElement)

    override val name = psi?.name ?: "<no type>"
    override val isInt = name == "int"

    override fun resolve(context: UastContext): UClass? {
        return null
    }
}

class KotlinUValueParameter(
        override val parent: UElement,
        override val psi: KtParameter
) : UValueParameter, PsiElementBacked {
    override val name = psi.name
    override val nameElement by lz { psi.nameIdentifier?.let { KotlinUFakeIdentifier(this, it) } }
    override val type = KotlinUType(psi.typeReference?.typeElement)
}