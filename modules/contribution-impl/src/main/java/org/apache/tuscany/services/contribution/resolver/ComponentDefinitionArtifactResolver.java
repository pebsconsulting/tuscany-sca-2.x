/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.tuscany.services.contribution.resolver;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.tuscany.services.contribution.model.Contribution;
import org.apache.tuscany.services.contribution.model.DeployedArtifact;
import org.apache.tuscany.services.spi.contribution.ArtifactResolver;
import org.apache.tuscany.services.spi.contribution.ArtifactResolverRegistry;
import org.apache.tuscany.services.spi.contribution.extension.ArtifactResolverExtension;

public class ComponentDefinitionArtifactResolver extends ArtifactResolverExtension implements ArtifactResolver {

    public ComponentDefinitionArtifactResolver(ArtifactResolverRegistry registry) {
        super(registry);
    }

    @Override
    public Class<?> getType() {
        return ComponentDefinition.class;
    }

    public <ComponentDefinition> ComponentDefinition resolve(Contribution contribution,
                                                             Class<ComponentDefinition> modelClass,
                                                             String namespace,
                                                             String name,
                                                             Map attributes/*,
                                                             DeploymentContext context*/) {

        // generate artifact uri based on it's name
        URI artifactURI = contribution.getUri().resolve(name);
        DeployedArtifact artifact = contribution.getArtifact(artifactURI);

        ComponentDefinition componentDefinition =
            (ComponentDefinition)artifact.getModelObject(CompositeComponentType.class, null);
        return componentDefinition;
    }

    public URL resolve(Contribution contribution, String targetNamespace, String location, String baseURI) {
        // TODO Auto-generated method stub
        return null;
    }

}
