/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.concurrent.preprocess.DiscoRunnableDecorator;
import software.amazon.disco.agent.interception.OneShotInstallable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.noneOf;

/**
 * A Java concurrency primitive is a Thread - instantiable one of two ways: 1) by subclassing and overriding the run() method
 * or 2) by construction with a Runnable parameter. This treatment is for a Thread itself, constructed with a Runnable target.
 * The treatment for subclasses of Thread is elsewhere.
 *
 * Threads are dispatched to a separate execution context by calling the start() method.
 *
 * See https://github.com/raphw/byte-buddy/issues/228 for a discussion of intercepting java.lang.Thread
 */
class ThreadInterceptor implements OneShotInstallable {
    public static Logger log = LogManager.getLogger(ThreadInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        //redefinition required, because Thread is already loaded at this point (we are in the main thread!)
        return InterceptorUtils.configureRedefinition(agentBuilder)
                .ignore(noneOf(Thread.class))
                .type(createThreadTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                    //the familiar idiom of method().intercept() does not work for Thread
                    .visit(Advice.to(StartAdvice.class)
                        .on(createStartMethodMatcher()))
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeDisposal() {
        // The Thread class has already been loaded by the time the installer installs us, so nothing to do here.
    }

    /**
     * Creates a type matcher which matches exactly against java.lang.Thread
     * @return the type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createThreadTypeMatcher() {
        //Usually we would match by name, to avoid initializing the target class, but Thread has already been loaded
        //hence why the install() method is a little different to most others throughout DiSCo
        return is(Thread.class);
    }

    /**
     * Creates a method matcher to match the start() method of a Thread
     * @return method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createStartMethodMatcher() {
        return named("start");
    }

    /**
     * A ByteBuddy Advice class to hook the start() method of java.lang.Thread. When the thread is the flavour
     * of Thread which was constructed from a Runnable - identifiable by its 'target' field being non-null,
     * we decorate the Runnable target.
     */
    public static class StartAdvice {
        /**
         * Advice OnMethodEnter to inspect the Thread and decorate its target Runnable if it has one.
         * @param target the 'target' field of the Thread, containing a Runnable
         */
        @Advice.OnMethodEnter
        public static void onStartEnter(@Advice.FieldValue(value="target", readOnly=false) Runnable target) {
             target = DiscoRunnableDecorator.maybeDecorate(target);
        }
    }
}
