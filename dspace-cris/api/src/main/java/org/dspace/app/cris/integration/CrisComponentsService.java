/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.Map;

import it.cilea.osd.jdyna.components.IComponent;

public class CrisComponentsService
{
    private Map<String, IComponent> components;

    public void setComponents(Map<String, IComponent> components)
    {
        this.components = components;
    }

    public Map<String, IComponent> getComponents()
    {
        return components;
    }
    
    
}
