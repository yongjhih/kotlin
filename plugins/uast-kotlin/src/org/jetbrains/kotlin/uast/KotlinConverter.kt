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

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.java.singletonListOrEmpty

object KotlinConverter : UastConverter {
    override fun isFileSupported(path: String) = path.endsWith(".kt", false) || path.endsWith(".kts", false)

    override fun convert(element: Any?, parent: UElement): UElement? {
        if (element !is KtElement) return null
        return convertKtElement(element, parent)
    }

    override fun convertWithParent(element: Any?): UElement? {
        if (element !is KtElement) return null
        if (element is KtFile) return KotlinUFile(element)

        val parent = element.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null
        return convertKtElement(element, parentUElement)
    }

    private fun convertKtElement(element: KtElement?, parent: UElement): UElement? = when (element) {
        is KtFile -> KotlinUFile(element)
        is KtDeclaration -> convert(element, parent)
        is KtImportDirective -> KotlinUImportStatement(parent, element)
        is KtCatchClause -> KotlinUCatchClause(parent, element)
        is KtExpression -> KotlinConverter.convert(element, parent)
        else -> {
            if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
                asSimpleReference(element, parent)
            } else {
                null
            }
        }
    }

    internal fun convert(element: KtDeclaration, parent: UElement): UDeclaration? = when (element) {
        is KtClassOrObject -> KotlinUClass(parent, element)
        is KtConstructor<*> -> KotlinConstructorUFunction(parent, element)
        is KtFunction -> KotlinUFunction(parent, element)
        is KtProperty -> KotlinUVariable(parent, element)
        is KtParameter -> KotlinUValueParameter(parent, element)
        else -> null
    }

    internal fun convert(expression: KtExpression, parent: UElement): UExpression = when (expression) {
        is KtFunction -> convertDeclaration(expression, parent)
        is KtProperty -> convertDeclaration(expression, parent)
        is KtClass -> convertDeclaration(expression, parent)

        is KtDotQualifiedExpression -> KotlinUQualifiedExpression(parent, expression)
        is KtSafeQualifiedExpression -> KotlinUSafeQualifiedExpression(parent, expression)
        is KtSimpleNameExpression -> KotlinUSimpleReferenceExpression(parent, expression.getReferencedName(), expression)
        is KtCallExpression -> KotlinUFunctionCallExpression(parent, expression)
        is KtBinaryExpression -> KotlinUBinaryExpression(parent, expression)
        is KtParenthesizedExpression -> KotlinUParenthesizedExpression(parent, expression)
        is KtPrefixExpression -> KotlinUPrefixExpression(parent, expression)
        is KtPostfixExpression -> KotlinUPostfixExpression(parent, expression)
        is KtThisExpression -> KotlinUThisExpression(parent, expression)
        is KtSuperExpression -> KotlinUSuperExpression(parent, expression)
        is KtIsExpression -> KotlinUTypeCheckExpression(parent, expression)
        is KtIfExpression -> KotlinUIfExpression(parent, expression)
        is KtWhileExpression -> KotlinUWhileExpression(parent, expression)
        is KtDoWhileExpression -> KotlinUDoWhileExpression(parent, expression)
        is KtForExpression -> KotlinUForEachExpression(parent, expression)
        is KtBreakExpression -> KotlinUSpecialExpressionList.Empty(parent, expression, UastSpecialExpressionKind.BREAK)
        is KtContinueExpression -> KotlinUSpecialExpressionList.Empty(parent, expression, UastSpecialExpressionKind.CONTINUE)
        is KtReturnExpression -> KotlinUSpecialExpressionList.Empty(parent, expression, UastSpecialExpressionKind.RETURN)
        is KtThrowExpression -> KotlinUSpecialExpressionList(parent, expression, UastSpecialExpressionKind.THROW).apply {
            expressions = singletonListOrEmpty(convertOrNull(expression.thrownExpression, this))
        }
        is KtBlockExpression -> KotlinUBlockExpression(parent, expression)
        is KtConstantExpression -> KotlinULiteralExpression(parent, expression)
        is KtTryExpression -> KotlinUTryExpression(parent, expression)
        is KtArrayAccessExpression -> KotlinUArrayAccess(parent, expression)
        is KtLambdaExpression -> KotlinULambdaExpression(parent, expression)
        is KtBinaryExpressionWithTypeRHS -> KotlinUBinaryExpressionWithType(parent, expression)

        else -> UnknownKotlinExpression(parent, expression)
    }

    internal fun asSimpleReference(element: PsiElement?, parent: UElement): USimpleReferenceExpression? {
        if (element == null) return null
        return KotlinUSimpleReferenceExpression(parent, KtPsiUtil.unquoteIdentifier(element.text), element)
    }

    internal fun convertOrEmpty(expression: KtExpression?, parent: UElement): UExpression {
        return if (expression != null) convert(expression, parent) else EmptyExpression(parent)
    }

    internal fun convertOrNull(expression: KtExpression?, parent: UElement): UExpression? {
        return if (expression != null) convert(expression, parent) else null
    }

    private fun convertDeclaration(declaration: KtDeclaration, parent: UElement): UExpression {
        val udeclarations = mutableListOf<UElement>()
        return SimpleUDeclarationsExpression(parent, udeclarations).apply {
            convert(declaration, this)?.let { udeclarations += it }
        }
    }
}