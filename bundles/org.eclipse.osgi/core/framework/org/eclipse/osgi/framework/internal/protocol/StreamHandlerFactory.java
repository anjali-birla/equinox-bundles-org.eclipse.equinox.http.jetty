/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol;

import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class contains the URL stream handler factory for the OSGi framework.
 */
public class StreamHandlerFactory extends MultiplexingFactory implements URLStreamHandlerFactory {
	static final SecureAction secureAction = new SecureAction();

	private ServiceTracker handlerTracker;

	protected static final String URLSTREAMHANDLERCLASS = "org.osgi.service.url.URLStreamHandlerService"; //$NON-NLS-1$
	protected static final String PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs"; //$NON-NLS-1$
	protected static final String INTERNAL_PROTOCOL_HANDLER_PKG = "org.eclipse.osgi.framework.internal.protocol"; //$NON-NLS-1$

	private static final List ignoredClasses = Arrays.asList(new Class[] {MultiplexingURLStreamHandler.class, StreamHandlerFactory.class, URL.class});
	private Hashtable proxies;
	private URLStreamHandlerFactory parentFactory;

	/**
	 * Create the factory.
	 *
	 * @param context BundleContext for the system bundle
	 */
	public StreamHandlerFactory(BundleContext context, FrameworkAdaptor adaptor) {
		super(context, adaptor);

		proxies = new Hashtable(15);
		handlerTracker = new ServiceTracker(context, URLSTREAMHANDLERCLASS, null);
		handlerTracker.open();
	}

	private Class getBuiltIn(String protocol, String builtInHandlers) {
		if (builtInHandlers == null)
			return null;
		Class clazz;
		StringTokenizer tok = new StringTokenizer(builtInHandlers, "|"); //$NON-NLS-1$
		while (tok.hasMoreElements()) {
			StringBuffer name = new StringBuffer();
			name.append(tok.nextToken());
			name.append("."); //$NON-NLS-1$
			name.append(protocol);
			name.append(".Handler"); //$NON-NLS-1$
			try {
				clazz = secureAction.forName(name.toString());
				if (clazz != null)
					return clazz; //this class exists, it is a built in handler	
			} catch (ClassNotFoundException ex) {
				// keep looking
			}
		}
		return null;
	}

	/**
	 * Creates a new URLStreamHandler instance for the specified
	 * protocol.
	 *
	 * @param protocol The desired protocol
	 * @return a URLStreamHandler for the specific protocol.
	 */
	public URLStreamHandler createURLStreamHandler(String protocol) {
		//first check for built in handlers
		String builtInHandlers = secureAction.getProperty(PROTOCOL_HANDLER_PKGS);
		Class clazz = getBuiltIn(protocol, builtInHandlers);
		if (clazz != null)
			return null; // let the VM handle it
		URLStreamHandler result = null;
		if (isMultiplexing()) {
			if (findAuthorizedURLStreamHandler(protocol) != null)
				result = new MultiplexingURLStreamHandler(protocol, this);
		} else {
			result = createInternalURLStreamHandler(protocol);
		}
		// if parent is present do parent lookup
		if (result == null && parentFactory != null)
			result = parentFactory.createURLStreamHandler(protocol);
		return result; //result may be null; let the VM handle it (consider sun.net.protocol.www.*)
	}

	public URLStreamHandler createInternalURLStreamHandler(String protocol) {

		//internal protocol handlers
		String internalHandlerPkgs = secureAction.getProperty(Constants.INTERNAL_HANDLER_PKGS);
		internalHandlerPkgs = internalHandlerPkgs == null ? INTERNAL_PROTOCOL_HANDLER_PKG : internalHandlerPkgs + '|' + INTERNAL_PROTOCOL_HANDLER_PKG;
		Class clazz = getBuiltIn(protocol, internalHandlerPkgs);

		if (clazz == null) {
			//Now we check the service registry
			//first check to see if the handler is in the cache
			URLStreamHandlerProxy handler = (URLStreamHandlerProxy) proxies.get(protocol);
			if (handler != null)
				return (handler);
			//look through the service registry for a URLStramHandler registered for this protocol
			org.osgi.framework.ServiceReference[] serviceReferences = handlerTracker.getServiceReferences();
			if (serviceReferences == null)
				return null;
			for (int i = 0; i < serviceReferences.length; i++) {
				Object prop = serviceReferences[i].getProperty(URLConstants.URL_HANDLER_PROTOCOL);
				if (!(prop instanceof String[]))
					continue;
				String[] protocols = (String[]) prop;
				for (int j = 0; j < protocols.length; j++)
					if (protocols[j].equals(protocol)) {
						handler = new URLStreamHandlerProxy(protocol, serviceReferences[i], context);
						proxies.put(protocol, handler);
						return (handler);
					}
			}
			return null;
		}

		// must be a built-in handler
		try {
			URLStreamHandler handler = (URLStreamHandler) clazz.newInstance();

			if (handler instanceof ProtocolActivator) {
				((ProtocolActivator) handler).start(context, adaptor);
			}

			return handler;
		} catch (Exception e) {
			return null;
		}
	}

	protected URLStreamHandler findAuthorizedURLStreamHandler(String protocol) {
		Object factory = findAuthorizedFactory(ignoredClasses);
		if (factory == null)
			return null;

		if (factory == this)
			return createInternalURLStreamHandler(protocol);

		try {
			Method createInternalURLStreamHandlerMethod = factory.getClass().getMethod("createInternalURLStreamHandler", new Class[] {String.class}); //$NON-NLS-1$
			return (URLStreamHandler) createInternalURLStreamHandlerMethod.invoke(factory, new Object[] {protocol});
		} catch (Exception e) {
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(StreamHandlerFactory.class.getName(), "findAuthorizedURLStreamHandler-loop", FrameworkLogEntry.ERROR, e, null)); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage());
		}
	}

	public Object getParentFactory() {
		return parentFactory;
	}

	public void setParentFactory(Object parentFactory) {
		if (this.parentFactory == null) // only allow it to be set once
			this.parentFactory = (URLStreamHandlerFactory) parentFactory;
	}
}
