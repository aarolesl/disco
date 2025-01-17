/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.exceptions;

/**
 * Exception thrown when a failure occurred during the initialization of this caching strategy or while attempting to cache a processed source.
 */
public class PreprocessCacheException extends Exception {
    /**
     * Constructor
     *
     * @param message error message explaining the failure
     * @param cause root cause
     */
    public PreprocessCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message error message explaining the failure
     */
    public PreprocessCacheException(String message) {
        super(message);
    }
}
