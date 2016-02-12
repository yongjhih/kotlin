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

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType

fun renderTypes(types: List<KotlinType>): Array<String> {
    val result = hashSetOf<ClassifierDescriptor>()
    val collectingRenderer = DescriptorRenderer.withOptions {
        textFormat = RenderingFormat.HTML
        modifiers = DescriptorRendererModifier.ALL
        customTypeConstructorRenderer = {
            descriptor ->
            result.add(descriptor)
            null
        }
    }

    types.forEach { collectingRenderer.renderType(it) }

    val classifiersByName = result.groupBy { it.name }

    val smartRenderer = DescriptorRenderer.withOptions {
        textFormat = RenderingFormat.HTML
        modifiers = DescriptorRendererModifier.ALL
        customTypeConstructorRenderer = {
            descriptor ->
            when {
                classifiersByName[descriptor.name]!!.size == 1 -> {
                    DescriptorRenderer.HTML.renderName(descriptor.name)
                }
                descriptor is ClassDescriptor -> {
                    DescriptorRenderer.HTML.renderFqName(descriptor.fqNameUnsafe)
                }
                descriptor is TypeParameterDescriptor -> {
                    DescriptorRenderer.HTML.renderName(descriptor.name) + " declared in ${DescriptorRenderer.HTML.renderFqName(descriptor.containingDeclaration.fqNameUnsafe)}"
                }
                else -> error("Unexpected classifier: ${descriptor.javaClass}")
            }
        }
    }

    return types.map { smartRenderer.renderType(it) }.toTypedArray<String>()
}