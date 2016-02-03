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

import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.*

object KotlinConverter : UastConverter {
    override fun isFileSupported(path: String) = path.endsWith(".kt", false) || path.endsWith(".kts", false)

    override fun convert(element: Any?, parent: UElement?): UElement? {
        if (element !is KtElement) return null
        return convertKtElement(element, parent)
    }

    override fun convertWithParent(element: Any?): UElement? {
        if (element !is KtElement) return null
        val parent = element.parent
        return convertKtElement(element, convertWithParent(parent))
    }

    private fun convertKtElement(element: KtElement?, parent: UElement?): UElement? = when (element) {
        is KtFile -> KotlinUFile(element)
        is KtDeclaration -> convert(element, parent)
        is KtImportDirective -> KotlinUImportStatement(parent!!, element)
        is KtExpression -> KotlinConverter.convert(parent!!, element)
        else -> null
    }

    internal fun convert(element: KtDeclaration, parent: UElement?): UDeclaration? = when (element) {
        is KtClassOrObject -> KotlinUClass(parent!!, element)
        is KtConstructor<*> -> KotlinConstructorUFunction(parent!!, element)
        is KtFunction -> KotlinUFunction(parent!!, element)
        is KtProperty -> KotlinUVariable(parent!!, element)
        is KtParameter -> KotlinUValueParameter(parent!!, element)
        else -> null
    }

    internal fun convert(parent: UElement, expression: KtExpression): UExpression = when (expression) {
        is KtDotQualifiedExpression -> KotlinUDotExpression(parent, expression)
        is KtSimpleNameExpression -> KotlinUReferenceExpression(parent, expression)
        is KtCallExpression -> KotlinUFunctionCallExpression(parent, expression)
        is KtBinaryExpression -> KotlinUBinaryExpression(parent, expression)
        is KtParenthesizedExpression -> KotlinUParenthesizedExpression(parent, expression)
        is KtPrefixExpression -> KotlinUPrefixExpression(parent, expression)
        is KtPostfixExpression -> KotlinUPostfixExpression(parent, expression)
        is KtThisExpression -> KotlinUThisExpression(parent, expression)
        is KtSuperExpression -> KotlinUSuperExpression(parent, expression)
        is KtIsExpression -> KotlinUTypeCheckExpression(parent, expression)
        is KtWhileExpression -> KotlinUWhileExpression(parent, expression)
        is KtDoWhileExpression -> KotlinUDoWhileExpression(parent, expression)
        is KtForExpression -> KotlinUForEachExpression(parent, expression)
        is KtBreakExpression -> KotlinUBreakExpression(parent, expression)
        is KtContinueExpression -> KotlinUContinueExpression(parent, expression)

        else -> UnknownKotlinExpression(parent, expression)
    }

    internal fun convertOrEmpty(parent: UElement, expression: KtExpression?): UExpression {
        return if (expression != null) convert(parent, expression) else EmptyExpression(parent)
    }
}