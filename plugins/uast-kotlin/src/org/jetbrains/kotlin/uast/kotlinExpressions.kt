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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

interface KotlinUExpression : UExpression, PsiElementBacked {
    override fun evaluate(): Any? {
        val ktElement = psi as? KtExpression ?: return null
        return ktElement.analyze(BodyResolveMode.PARTIAL)[BindingContext.COMPILE_TIME_VALUE, ktElement]
    }
}



open class KotlinUSpecialExpressionList(
        override val parent: UElement,
        override val psi: PsiElement, // original element
        override val kind: UastSpecialExpressionKind
) : USpecialExpressionList, PsiElementBacked {
    class Empty(parent: UElement, psi: PsiElement, expressionType: UastSpecialExpressionKind) :
            KotlinUSpecialExpressionList(parent, psi, expressionType) {
        init { expressions = emptyList() }
    }

    override lateinit var expressions: List<UExpression>
}

class KotlinUQualifiedExpression(
        override val parent: UElement,
        override val psi: KtDotQualifiedExpression
) : UQualifiedExpression, PsiElementBacked, KotlinUExpression {
    override val receiver by lz { KotlinConverter.convertOrEmpty(psi.receiverExpression, this) }
    override val selector by lz { KotlinConverter.convertOrEmpty(psi.selectorExpression, this) }
}

class KotlinUSafeQualifiedExpression(
        override val parent: UElement,
        override val psi: KtSafeQualifiedExpression
) : UQualifiedExpression, PsiElementBacked, KotlinUExpression {
    override val receiver by lz { KotlinConverter.convertOrEmpty(psi.receiverExpression, this) }
    override val selector by lz { KotlinConverter.convertOrEmpty(psi.selectorExpression, this) }
}

class KotlinUSimpleReferenceExpression(
        override val parent: UElement,
        override val identifier: String,
        override val psi: PsiElement
) : USimpleReferenceExpression, PsiElementBacked, KotlinUExpression {
    override fun resolve(uastContext: UastContext) = uastContext.convert(
            psi.references.firstOrNull()?.resolve()) as? UDeclaration
}

class KotlinUFunctionCallExpression(
        override val parent: UElement,
        override val psi: KtCallExpression
) : UFunctionCallExpression, PsiElementBacked, KotlinUExpression {
    override val functionReference by lz { psi.calleeExpression?.let { KotlinConverter.convert(it, this) } }
    override val functionName = (psi.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()

    override val argumentCount = psi.valueArguments.size
    override val arguments by lz { psi.valueArguments.map { KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this) } }
    override val callType = UastCallType.FUNCTION_CALL
    override val anonymousDeclaration = null

    override fun resolve(context: UastContext): UFunction? {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val resultingDescriptor = psi.getResolvedCall(bindingContext)?.resultingDescriptor ?: return null
        val source = DescriptorToSourceUtilsIde.getAnyDeclaration(psi.project, resultingDescriptor) ?: return null
        return context.convert(source) as? UFunction
    }
}

class KotlinUBinaryExpression(
        override val parent: UElement,
        override val psi: KtBinaryExpression
) : UBinaryExpression, PsiElementBacked, KotlinUExpression {
    override val leftOperand by lz { KotlinConverter.convertOrEmpty(psi.left, this) }
    override val rightOperand by lz { KotlinConverter.convertOrEmpty(psi.right, this) }

    override val operatorType = when (psi.operationToken) {
        KtTokens.PLUS -> BinaryOperator.PLUS
        KtTokens.MINUS -> BinaryOperator.MINUS
        KtTokens.MUL -> BinaryOperator.MULT
        KtTokens.DIV -> BinaryOperator.DIV
        KtTokens.PERC -> BinaryOperator.MOD
        KtTokens.EQEQ -> BinaryOperator.EQUALS
        KtTokens.EXCLEQ -> BinaryOperator.NOT_EQUALS
        KtTokens.EQEQEQ -> BinaryOperator.IDENTITY_EQUALS
        KtTokens.EXCLEQEQEQ -> BinaryOperator.IDENTITY_NOT_EQUALS
        KtTokens.GT -> BinaryOperator.GREATER
        KtTokens.GTEQ -> BinaryOperator.GREATER_OR_EQUAL
        KtTokens.LT -> BinaryOperator.LESS
        KtTokens.LTEQ -> BinaryOperator.LESS_OR_EQUAL
        else -> BinaryOperator.UNKNOWN
    }

    override val operator = psi.operationReference.text
}

class KotlinUParenthesizedExpression(
        override val parent: UElement,
        override val psi: KtParenthesizedExpression
) : UParenthesizedExpression, PsiElementBacked, KotlinUExpression {
    override val expression by lz { KotlinConverter.convertOrEmpty(psi.expression, this) }
}

class KotlinUPrefixExpression(
        override val parent: UElement,
        override val psi: KtPrefixExpression
) : UPrefixExpression, PsiElementBacked {
    override val operatorType = when (psi.operationToken) {
        KtTokens.PLUS -> PrefixOperator.UNARY_PLUS
        KtTokens.MINUS -> PrefixOperator.UNARY_MINUS
        KtTokens.PLUSPLUS -> PrefixOperator.INC
        KtTokens.MINUSMINUS -> PrefixOperator.DEC
        else -> PrefixOperator.UNKNOWN
    }
    override val operator = operatorType.text
    override val operand by lz { KotlinConverter.convertOrEmpty(psi.baseExpression, this) }
    override fun evaluate() = null
}

class KotlinUPostfixExpression(
        override val parent: UElement,
        override val psi: KtPostfixExpression
) : UPostfixExpression, PsiElementBacked, KotlinUExpression {
    override val operatorType = when (psi.operationToken) {
        KtTokens.PLUSPLUS -> PostfixOperator.INC
        KtTokens.MINUSMINUS -> PostfixOperator.DEC
        KtTokens.EXCLEXCL -> PostfixOperator.UNKNOWN //TODO
        else -> PostfixOperator.UNKNOWN
    }
    override val operator = operatorType.text
    override val operand by lz { KotlinConverter.convertOrEmpty(psi.baseExpression, this) }
}

class KotlinUThisExpression(
        override val parent: UElement,
        override val psi: KtThisExpression
) : UThisExpression, PsiElementBacked

class KotlinUSuperExpression(
        override val parent: UElement,
        override val psi: KtSuperExpression
) : USuperExpression, PsiElementBacked

class KotlinUTypeCheckExpression(
        override val parent: UElement,
        override val psi: KtIsExpression
) : UBinaryExpressionWithType, PsiElementBacked, KotlinUExpression {
    override val operand by lz { KotlinConverter.convert(psi.leftHandSide, this) }
    override val type by lz { KotlinUType(psi.typeReference) }
    override val operationKind = BinaryExpressionWithTypeKind.INSTANCE_CHECK //TODO fix !is negated
}

class KotlinUBinaryExpressionWithType(
        override val parent: UElement,
        override val psi: KtBinaryExpressionWithTypeRHS
) : UBinaryExpressionWithType, PsiElementBacked, KotlinUExpression {
    override val operand by lz { KotlinConverter.convert(psi.left, this) }
    override val type by lz { KotlinUType(psi.right) }
    override val operationKind = when (psi.operationReference.getReferencedNameElementType()) {
        KtTokens.AS_KEYWORD -> BinaryExpressionWithTypeKind.TYPE_CAST
        KtTokens.AS_SAFE -> BinaryExpressionWithTypeKind.TYPE_CAST //TODO fix safe
        else -> BinaryExpressionWithTypeKind.TYPE_CAST //TODO fix unknown
    }
}

class KotlinUIfExpression(
        override val parent: UElement,
        override val psi: KtIfExpression
) : UIfExpression, PsiElementBacked, KotlinUExpression {
    override val condition by lz { KotlinConverter.convertOrEmpty(psi.condition, this) }
    override val thenBranch by lz { KotlinConverter.convertOrNull(psi.then, this) }
    override val elseBranch by lz { KotlinConverter.convertOrNull(psi.`else`, this) }
}

class KotlinUWhileExpression(
        override val parent: UElement,
        override val psi: KtWhileExpression
) : UWhileExpression, PsiElementBacked, NoEvaluate {
    override val condition by lz { KotlinConverter.convertOrEmpty(psi.condition, this) }
    override val body by lz { KotlinConverter.convertOrEmpty(psi.body, this) }
}

class KotlinUDoWhileExpression(
        override val parent: UElement,
        override val psi: KtDoWhileExpression
) : UDoWhileExpression, PsiElementBacked, NoEvaluate {
    override val condition by lz { KotlinConverter.convertOrEmpty(psi.condition, this) }
    override val body by lz { KotlinConverter.convertOrEmpty(psi.body, this) }
}

class KotlinUForEachExpression(
        override val parent: UElement,
        override val psi: KtForExpression
) : UForEachExpression, PsiElementBacked, NoEvaluate {
    override val variableName by lz { psi.loopParameter?.name }
    override val iteratedValue by lz { KotlinConverter.convertOrEmpty(psi.loopRange, this) }
    override val body by lz { KotlinConverter.convertOrEmpty(psi.body, this) }
}

class UnknownKotlinExpression(
        override val parent: UElement,
        override val psi: KtExpression
) : UExpression, PsiElementBacked, KotlinUExpression {
    override fun traverse(handler: UastHandler) {}
    override fun logString() = "[!] UnknownKotlinExpression ($psi)"
}

class KotlinUBlockExpression(
        override val parent: UElement,
        override val psi: KtBlockExpression
) : UBlockExpression, PsiElementBacked, NoEvaluate {
    override val expressions by lz { psi.statements.map { KotlinConverter.convertOrEmpty(it, this) } }
}

class KotlinULiteralExpression(
        override val parent: UElement,
        override val psi: KtConstantExpression
) : ULiteralExpression, PsiElementBacked {
    override val isNull: Boolean
        get() = psi.isNullExpression()

    override val text: String
        get() = psi.text

    override val value by lazy {
        psi.analyze(BodyResolveMode.PARTIAL)[BindingContext.COMPILE_TIME_VALUE, psi]
    }

    override fun evaluate() = value
}

class KotlinUTryExpression(
        override val parent: UElement,
        override val psi: KtTryExpression
) : UTryExpression, PsiElementBacked, NoEvaluate {
    override val tryClause by lz { KotlinConverter.convert(psi.tryBlock, this) }
    override val catchClauses by lz { psi.catchClauses.map { KotlinUCatchClause(this, it) } }
    override val finallyClause by lz { psi.finallyBlock?.finalExpression?.let { KotlinConverter.convert(it, this) } }
}

class KotlinUCatchClause(
        override val parent: UElement,
        override val psi: KtCatchClause
) : UCatchClause, PsiElementBacked {
    override val body by lz { KotlinConverter.convertOrEmpty(psi.catchBody, this) }
}

class KotlinUArrayAccess(
        override val parent: UElement,
        override val psi: KtArrayAccessExpression
) : UArrayAccessExpression, PsiElementBacked {
    override val receiver by lz { KotlinConverter.convertOrEmpty(psi.arrayExpression, this) }
    override val indices by lz { psi.indexExpressions.map { KotlinConverter.convert(it, this) } }
}

class KotlinULambdaExpression(
        override val parent: UElement,
        override val psi: KtLambdaExpression
) : ULambdaExpression, PsiElementBacked {
    override val body by lz { KotlinConverter.convertOrEmpty(psi.bodyExpression, this) }
    override val valueParameters by lz { psi.valueParameters.map { KotlinUValueParameter(this, it) } }
}