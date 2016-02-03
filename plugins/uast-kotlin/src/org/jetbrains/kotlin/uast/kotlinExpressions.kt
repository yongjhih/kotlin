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

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.uast.KotlinConverter
import org.jetbrains.kotlin.uast.KotlinUType
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUType
import org.jetbrains.uast.java.lz
import org.jetbrains.uast.psi.PsiElementBacked

interface KotlinUExpression : UExpression, PsiElementBacked {
    override fun evaluate() = null
}

interface NoEvaluate : UExpression, PsiElementBacked {
    override fun evaluate() = null
}

class KotlinUIdentifier(
        override val parent: UElement,
        override val psi: KtSimpleNameExpression
) : UIdentifier, PsiElementBacked {
    override val name = psi.getReferencedName()
    override fun resolve(context: UastContext) = null
}

class KotlinUDotExpression(
        override val parent: UElement,
        override val psi: KtDotQualifiedExpression
) : UDotExpression, PsiElementBacked, KotlinUExpression {
    override val qualifier by lz { KotlinConverter.convertOrEmpty(this, psi.receiverExpression) }
    override val identifier by lz { psi.selectorExpression?.text ?: "" }
}

class KotlinUReferenceExpression(
        override val parent: UElement,
        override val psi: KtSimpleNameExpression
) : UReferenceExpression, PsiElementBacked, KotlinUExpression {
    override val identifier by lz { psi.getReferencedName() }
}

class KotlinUFakeReferenceExpression(
        override val parent: UElement,
        override val psi: PsiElement
) : UReferenceExpression, PsiElementBacked {
    override val identifier = psi.text
    override fun evaluate() = null
}

class KotlinUFunctionCallExpression(
        override val parent: UElement,
        override val psi: KtCallExpression
) : UFunctionCallExpression, PsiElementBacked, KotlinUExpression {
    override val functionReference by lz { KotlinUFakeReferenceExpression(this, psi.calleeExpression ?: psi) }
    override val argumentCount = psi.valueArguments.size
    override val arguments by lz { psi.valueArguments.map { KotlinConverter.convertOrEmpty(this, it.getArgumentExpression()) } }
    override val isConstructorCall = false
    override fun resolve(context: UastContext) = null
}

class KotlinUBinaryExpression(
        override val parent: UElement,
        override val psi: KtBinaryExpression
) : UBinaryExpression, PsiElementBacked, KotlinUExpression {
    override val leftOperand by lz { KotlinConverter.convertOrEmpty(this, psi.left) }
    override val rightOperand by lz { KotlinConverter.convertOrEmpty(this, psi.right) }

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
    override val expression by lz { KotlinConverter.convertOrEmpty(this, psi.expression) }
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
    override val operand by lz { KotlinConverter.convertOrEmpty(this, psi.baseExpression) }
    override fun evaluate() = null
}

class KotlinUPostfixExpression(
        override val parent: UElement,
        override val psi: KtPostfixExpression
) : UPostfixExpression, PsiElementBacked, KotlinUExpression {
    override val operatorType = when (psi.operationToken) {
        KtTokens.PLUSPLUS -> PostfixOperator.INC
        KtTokens.MINUSMINUS -> PostfixOperator.DEC
        else -> PostfixOperator.UNKNOWN
    }
    override val operator = operatorType.text
    override val operand by lz { KotlinConverter.convertOrEmpty(this, psi.baseExpression) }
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
    override val operand by lz { KotlinConverter.convert(this, psi.leftHandSide) }
    override val type by lz { KotlinUType(psi.typeReference) }
    override val operationKind = BinaryExpressionWithTypeKind.TYPE_CAST
}

//class KotlinUArrayAccessExpression(
//        override val parent: UElement,
//        override val psi: KtArrayAccessExpression
//) : UArrayAccessExpression, PsiElementBacked, NoEvaluate {
//    override val array by lz { KotlinConverter.convertOrEmpty(this, psi.arrayExpression) }
//    override val indexes by lz { psi.indexExpressions.map { KotlinConverter.convertOrEmpty(this, it) } }
//}

//class KotlinUClassLiteralExpression(
//        override val parent: UElement,
//        override val psi: KtClassLiteralExpression
//) : UClassLiteralExpression, PsiElementBacked, NoEvaluate {
//    override val type by lz { KotlinUType(psi.typeReference) }
//}
//
//class KotlinUFunctionLiteralExpression(
//        override val parent: UElement,
//        override val psi: KtLambdaExpression
//) : UFunctionLiteralExpression, PsiElementBacked, NoEvaluate {
//    override val parameters by lz { psi.valueParameters.map { KotlinConverter.convert(this, it) } }
//    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.bodyExpression) }
//}

class KotlinUWhileExpression(
        override val parent: UElement,
        override val psi: KtWhileExpression
) : UWhileExpression, PsiElementBacked, NoEvaluate {
    override val condition by lz { KotlinConverter.convertOrEmpty(this, psi.condition) }
    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.body) }
}

class KotlinUDoWhileExpression(
        override val parent: UElement,
        override val psi: KtDoWhileExpression
) : UDoWhileExpression, PsiElementBacked, NoEvaluate {
    override val condition by lz { KotlinConverter.convertOrEmpty(this, psi.condition) }
    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.body) }
}

class KotlinUForEachExpression(
        override val parent: UElement,
        override val psi: KtForExpression
) : UForEachExpression, PsiElementBacked, NoEvaluate {
    override val variableName by lz { psi.loopParameter?.name }
    override val iteratedValue by lz { KotlinConverter.convertOrEmpty(this, psi.loopRange) }
    override val body by lz { KotlinConverter.convertOrEmpty(this, psi.body) }
}

class KotlinUBreakExpression(
        override val parent: UElement,
        override val psi: KtBreakExpression
) : UBreakExpression, PsiElementBacked

class KotlinUContinueExpression(
        override val parent: UElement,
        override val psi: KtContinueExpression
) : UContinueExpression, PsiElementBacked

class UnknownKotlinExpression(override val parent: UElement, override val psi: KtExpression) : UExpression, PsiElementBacked, KotlinUExpression {
    override fun traverse(handler: (UElement) -> Unit) {}
    override fun logString() = "[!] UnknownKotlinExpression ($psi)"
}