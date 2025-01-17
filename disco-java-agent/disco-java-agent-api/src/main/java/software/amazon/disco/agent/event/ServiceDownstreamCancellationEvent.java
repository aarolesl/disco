/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * An event issued to the event bus when downstream service request is cancelled
 */
public class ServiceDownstreamCancellationEvent extends AbstractServiceCancellationEvent{
    /**
     * Constructor for a ServiceDownstreamCancellationEvent
     *
     * @param origin    the origin of this event e.g. 'Web' or 'gRPC'
     * @param service   the service name e.g. WeatherService
     * @param operation the operation name e.g getWeather
     * @param requestEvent  the associated cancelled request Event
     */
    public ServiceDownstreamCancellationEvent(String origin, String service, String operation, ServiceDownstreamRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceEvent.Type getType() {
        return ServiceEvent.Type.DOWNSTREAM;
    }
}
