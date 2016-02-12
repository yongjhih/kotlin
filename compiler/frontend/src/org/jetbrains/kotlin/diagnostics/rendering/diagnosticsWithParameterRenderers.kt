/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters3
import org.jetbrains.kotlin.renderer.Renderer


class DiagnosticWithParameters1Renderer<A : Any>(
        message: String,
        private val rendererForA: Renderer<A>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        return arrayOf(renderParameter<A>(diagnostic.a, rendererForA))
    }
}

class DiagnosticWithParameters2Renderer<A : Any, B : Any>(
        message: String,
        private val rendererForA: Renderer<A>?,
        private val rendererForB: Renderer<B>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters2<*, A, B>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters2<*, A, B>): Array<out Any> {
        return arrayOf(renderParameter<A>(diagnostic.a, rendererForA), renderParameter<B>(diagnostic.b, rendererForB))
    }
}

class DiagnosticWithParameters3Renderer<A : Any, B : Any, C : Any>(
        message: String,
        private val rendererForA: Renderer<A>?,
        private val rendererForB: Renderer<B>?,
        private val rendererForC: Renderer<C>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<*, A, B, C>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters3<*, A, B, C>): Array<out Any> {
        return arrayOf(
                renderParameter(diagnostic.a, rendererForA),
                renderParameter(diagnostic.b, rendererForB),
                renderParameter(diagnostic.c, rendererForC)
        )
    }
}

class DiagnosticWithParameters1MultiRenderer<A>(
        message: String,
        private val renderer: MultiRenderer1<A>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        return renderer.render(diagnostic.a)
    }
}

class DiagnosticWithParameters2MultiRenderer<A, B>(
        message: String,
        private val renderer: MultiRenderer2<A, B>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters2<*, A, B>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters2<*, A, B>): Array<out Any> {
        return renderer.render(diagnostic.a, diagnostic.b)
    }
}

class DiagnosticWithParameters3MultiRenderer<A, B, C>(
        message: String,
        private val renderer: MultiRenderer3<A, B, C>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<*, A, B, C>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters3<*, A, B, C>): Array<out Any> {
        return renderer.render(diagnostic.a, diagnostic.b, diagnostic.c)
    }
}

interface MultiRenderer1<in A> {
    fun render(a: A): Array<String>
}

interface MultiRenderer2<in A, in B> {
    fun render(a: A, b: B): Array<String>
}

interface MultiRenderer3<in A, in B, in C> {
    fun render(a: A, b: B, c: C): Array<String>
}
