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
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.reflect.InvocationTargetException

object ScriptRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val (scriptPath, classpath) = args
        val scriptArgs = args.drop(2)

        val kotlinPaths = PathUtil.getKotlinPathsForCompiler()
        val messageCollector = GroupingMessageCollector(PrintingMessageCollector(
                System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, /* verbose = */ false
        ))
        val messageSeverityCollector = MessageSeverityCollector(messageCollector)

        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            addKotlinSourceRoot(scriptPath)

            addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
            addJvmClasspathRoot(kotlinPaths.runtimePath)
            addJvmClasspathRoot(kotlinPaths.reflectPath)
            addJvmClasspathRoots(classpath.split(File.pathSeparator).map(::File))

            put(JVMConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)
            add(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, StandardScriptDefinition)
        }

        val environment = KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable() /* TODO: dispose */, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        val scriptClass = try {
            if (messageSeverityCollector.anyReported(CompilerMessageSeverity.ERROR)) throw CompilationError()

            KotlinToJVMBytecodeCompiler.compileScript(configuration, kotlinPaths, environment) ?: throw CompilationError()
        }
        finally {
            messageCollector.flush()
        }

        try {
            scriptClass.getConstructor(Array<String>::class.java).newInstance(scriptArgs.toTypedArray())
        }
        catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private class CompilationError : Error()
}
