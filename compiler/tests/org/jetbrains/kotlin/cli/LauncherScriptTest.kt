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

package org.jetbrains.kotlin.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class LauncherScriptTest : TestCaseWithTmpdir() {
    private fun runProcess(
            executableName: String,
            vararg args: String,
            expectedStdout: String = "",
            expectedStderr: String = "",
            expectedExitCode: Int = ExitCode.OK.code
    ) {
        val executableFileName = if (SystemInfo.isWindows) "$executableName.bat" else executableName
        val launcherFile = File(PathUtil.getKotlinPathsForDistDirectory().homePath, "bin/$executableFileName")
        assertTrue("Launcher script not found, run 'ant dist': ${launcherFile.absolutePath}", launcherFile.exists())

        val processOutput = ExecUtil.execAndGetOutput(GeneralCommandLine(launcherFile.absolutePath, *args))
        val (stdout, stderr, exitCode) = Triple(
                StringUtil.convertLineSeparators(processOutput.stdout).trim(),
                StringUtil.convertLineSeparators(processOutput.stderr).trim(),
                processOutput.exitCode
        )

        try {
            assertEquals(expectedStdout, stdout)
            assertEquals(expectedStderr, stderr)
            assertEquals(expectedExitCode, exitCode)
        }
        catch (e: Throwable) {
            System.err.println("exit code $exitCode")
            System.err.println("<stdout>$stdout</stdout>")
            System.err.println("<stderr>$stderr</stderr>")
            throw e
        }
    }

    private val testDataDirectory: String
        get() = KotlinTestUtils.getTestDataPathBase() + "/launcher"

    fun testKotlincSimple() {
        runProcess(
                "kotlinc",
                "$testDataDirectory/helloWorld.kt",
                "-d", tmpdir.path
        )
    }

    fun testKotlincJvmSimple() {
        runProcess(
                "kotlinc-jvm",
                "$testDataDirectory/helloWorld.kt",
                "-d", tmpdir.path
        )
    }

    fun testKotlincJsSimple() {
        runProcess(
                "kotlinc-js",
                "$testDataDirectory/emptyMain.kt",
                "-no-stdlib",
                "-output", File(tmpdir, "out.js").path
        )
    }

    fun testKotlinSimple() {
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path)
        runProcess(
                "kotlin",
                "-cp", tmpdir.path,
                "test.HelloWorldKt",
                expectedStdout = "Hello!"
        )
    }

    fun testKotlinFromJar() {
        val jarFile = File(tmpdir, "out.jar").path
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", jarFile)
        runProcess(
                "kotlin",
                "-cp", jarFile,
                "test.HelloWorldKt",
                expectedStdout = "Hello!"
        )
    }

    fun testPassSystemProperties() {
        runProcess("kotlinc", "$testDataDirectory/systemProperties.kt", "-d", tmpdir.path)
        runProcess(
                "kotlin",
                "-cp", tmpdir.path,
                "-Dfoo.name=foo.value",
                "-J-Dbar.name=bar.value",
                "test.SystemPropertiesKt",
                expectedStdout = "foo.name=foo.value\nbar.name=bar.value"
        )
    }

    fun testSanitizedStackTrace() {
        runProcess("kotlinc", "$testDataDirectory/throwException.kt", "-d", tmpdir.path)
        runProcess(
                "kotlin",
                "-cp", tmpdir.path,
                "test.ThrowExceptionKt",
                expectedExitCode = 1,
                expectedStderr = """
Exception in thread "main" java.lang.RuntimeException: RE
	at test.ThrowExceptionKt.f7(throwException.kt:40)
	at test.ThrowExceptionKt.f8(throwException.kt:45)
	at test.ThrowExceptionKt.f9(throwException.kt:49)
	at test.ThrowExceptionKt.main(throwException.kt:53)
Caused by: java.lang.IllegalStateException: ISE
	at test.ThrowExceptionKt.f4(throwException.kt:23)
	at test.ThrowExceptionKt.f5(throwException.kt:28)
	at test.ThrowExceptionKt.f6(throwException.kt:32)
	at test.ThrowExceptionKt.f7(throwException.kt:37)
	... 3 more
Caused by: java.lang.AssertionError: assert
	at test.ThrowExceptionKt.f1(throwException.kt:7)
	at test.ThrowExceptionKt.f2(throwException.kt:11)
	at test.ThrowExceptionKt.f3(throwException.kt:15)
	at test.ThrowExceptionKt.f4(throwException.kt:20)
	... 6 more
""".trim()
        )
    }

    fun testScriptOK() {
        runProcess("kotlin", "$testDataDirectory/scriptOK.kts", "OK", expectedStdout = "OK")
    }

    fun testScriptCompilationError() {
        runProcess(
                "kotlin",
                "$testDataDirectory/scriptCompilationError.kts",
                expectedExitCode = 32,
                expectedStderr = """
compiler/testData/launcher/scriptCompilationError.kts:1:8: error: expecting an expression
val x =
       ^
""".trim()
        )
    }

    fun testScriptException() {
        runProcess(
                "kotlin",
                "$testDataDirectory/scriptException.kts",
                expectedExitCode = 1,
                expectedStderr = """
Exception in thread "main" java.lang.Error
	at ScriptException.<init>(Unknown Source)
""".trim()
        )
    }

    fun testKtXXXX() {
        runProcess("kotlin", "$testDataDirectory/scriptOK.kts", "-arg1", "value", "-arg2", expectedStdout = "-arg1, value, -arg2")
    }

    fun testExpressionOK() {
        runProcess("kotlin", "-expression", "2+2", expectedStdout = "4")
    }

    fun testExpressionCompilationError() {
        runProcess(
                "kotlin", "-e", "val x =",
                expectedExitCode = 32,
                expectedStderr = """
error: expecting an expression
val x =
       ^
""".trim())
    }

    fun testExpressionException() {
        runProcess(
                "kotlin", "-e", """throw IllegalStateException("error")""",
                expectedExitCode = 1,
                expectedStderr = """
java.lang.IllegalStateException: error
""".trim())
    }
}
