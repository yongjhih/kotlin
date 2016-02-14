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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.repl.ReplInterpreter
import java.io.File
import kotlin.system.exitProcess

object ExpressionRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val (code, classpath) = args
        val repl = ReplInterpreter(
                Disposer.newDisposable() /* TODO: dispose */, classpath.split(File.pathSeparator).map(::File),
                /* ideMode = */ false, null, /* noJdk = */ false
        )
        val result = repl.eval(code)
        if (result.type == ReplInterpreter.LineResultType.SUCCESS) {
            if (!result.isUnit) {
                println(result.value)
            }
        }
        else {
            System.err.print(result.errorText ?: "unknown error")
            // 32 is just a number different from 1, to distinguish compilation errors vs execution errors
            val exitCode = if (result.type == ReplInterpreter.LineResultType.RUNTIME_ERROR) 1 else 32
            exitProcess(exitCode)
        }
    }
}
