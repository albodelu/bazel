// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.java.proto;

import static com.google.common.collect.Iterables.transform;
import static com.google.devtools.build.lib.rules.java.JavaCompilationArgs.ClasspathType.BOTH;

import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.WrappingProvider;
import com.google.devtools.build.lib.rules.java.JavaCompilationArgs;
import com.google.devtools.build.lib.rules.java.JavaCompilationArgsProvider;
import com.google.devtools.build.lib.rules.java.JavaConfiguration;
import com.google.devtools.build.lib.rules.proto.ProtoConfiguration;

public class StrictDepsUtils {

  /**
   * Used in JavaXXXProtoLibrary.java files to construct a JCAP from 'deps', where those were
   * populated by the Aspect it injected.
   *
   * <p>Takes care of strict deps.
   */
  public static JavaCompilationArgsProvider constructJcapFromAspectDeps(
      RuleContext ruleContext,
      Iterable<JavaProtoLibraryAspectProvider> javaProtoLibraryAspectProviders) {
    if (StrictDepsUtils.isStrictDepsJavaProtoLibrary(ruleContext)) {
      return JavaCompilationArgsProvider.merge(
          WrappingProvider.Helper.unwrapProviders(
              javaProtoLibraryAspectProviders, JavaCompilationArgsProvider.class));
    } else if (ruleContext
        .getConfiguration()
        .getFragment(ProtoConfiguration.class)
        .jplNonStrictDepsLikePl()) {
      return JavaCompilationArgsProvider.merge(
          transform(javaProtoLibraryAspectProviders, p -> p.getNonStrictCompArgsProvider()));
    } else {
      return StrictDepsUtils.makeNonStrict(
          JavaCompilationArgsProvider.merge(
              WrappingProvider.Helper.unwrapProviders(
                  javaProtoLibraryAspectProviders, JavaCompilationArgsProvider.class)));
    }
  }

  /**
   * Returns true iff 'ruleContext' should enforce strict-deps.
   *
   * <p>Using this method requires requesting the JavaConfiguration fragment.
   */
  public static boolean isStrictDepsJavaProtoLibrary(RuleContext ruleContext) {
    if (ruleContext.getFragment(JavaConfiguration.class).strictDepsJavaProtos()) {
      return true;
    }
    return (boolean) ruleContext.getRule().getAttributeContainer().getAttr("strict_deps");
  }

  /**
   * Returns a new JavaCompilationArgsProvider whose direct-jars part is the union of both the
   * direct and indirect jars of 'provider'.
   */
  public static JavaCompilationArgsProvider makeNonStrict(JavaCompilationArgsProvider provider) {
    JavaCompilationArgs.Builder directCompilationArgs = JavaCompilationArgs.builder();
    directCompilationArgs
        .addTransitiveArgs(provider.getJavaCompilationArgs(), BOTH)
        .addTransitiveArgs(provider.getRecursiveJavaCompilationArgs(), BOTH);
    return JavaCompilationArgsProvider.create(
        directCompilationArgs.build(),
        provider.getRecursiveJavaCompilationArgs(),
        provider.getCompileTimeJavaDependencyArtifacts(),
        provider.getRunTimeJavaDependencyArtifacts());
  }
}
