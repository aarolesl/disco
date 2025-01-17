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
package software.amazon.disco.agent.event;

/**
 * @deprecated replaced by {@link ActivityRequestHeaderRetrievable}.
 * In the past HeaderRetrievable have only existed for 'incoming requests' and the plugins have a reliance on this implicit semantics.
 * So using this interface to retrieve 'outgoing' request headers would break the plugins that rely on this semantics.
 * The Activity/DownstreamHeaderRetrievable makes it explicit the direction of the headers and is a more concrete public contract.
 *
 * A composable interface that disco events can implement if consumers of those events should be able to
 * retrieve the headers of the request associated with the event.
 */
@Deprecated
public interface HeaderRetrievable extends ActivityRequestHeaderRetrievable {
}
