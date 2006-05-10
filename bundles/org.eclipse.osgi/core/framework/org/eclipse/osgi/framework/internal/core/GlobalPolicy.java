/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Global policy is an implementation of a buddy policy. It is responsible
 * for looking up a class within the global set of exported classes. If multiple
 * version of the same package are exported in the system, the exported package
 * with the highest version will be returned.
 */
public class GlobalPolicy implements IBuddyPolicy {
	private PackageAdmin admin;

	public GlobalPolicy( PackageAdmin admin) {
		this.admin = admin;
	}

	public Class loadClass(String name) {
		ExportedPackage pkg = admin.getExportedPackage(BundleLoader.getPackageName(name));
		if (pkg == null)
			return null;
		try {
			return pkg.getExportingBundle().loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public URL loadResource(String name) {
        //get all exported packages that match the resource's package
		ExportedPackage pkg = admin.getExportedPackage(BundleLoader.getResourcePackageName(name));
		if (pkg == null)
			return null;
		return pkg.getExportingBundle().getResource(name);
	}

	public Enumeration loadResources(String name) {
        //get all exported packages that match the resource's package
		ExportedPackage[] pkgs = admin.getExportedPackages(BundleLoader.getResourcePackageName(name));
		if (pkgs == null || pkgs.length == 0)
			return null;
        
        //get all matching resources for each package
        Vector resources = null;
        for (int i=0; i<pkgs.length; i++) {
            try {
                Enumeration results = pkgs[i].getExportingBundle().getResources(name);
                if (results != null) {
                    if (resources == null)
                        resources = new Vector();
                    while (results.hasMoreElements()) {
                        Object url = results.nextElement();
                        if (!resources.contains(url)) //avoid exact duplicates
                            resources.add(url);
                    }
                }
            }
            catch (IOException e) {
                //ignore IO problems and try next package
            }
        }
        
        return resources == null ? null : resources.elements();
	}
}
