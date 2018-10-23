package org.ntlab.traceAnalysisPlatform;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class ExtensionLoader {
	private static final String EXTENSION_POINT_ID = Activator.PLUGIN_ID + ".additionalClasspaths";	//ägí£É|ÉCÉìÉgID
	private List<IAdditionalLaunchConfiguration> list;
	
	public List<IAdditionalLaunchConfiguration> getAdditionalLaunchConfigurations() {
		if (list != null) {
			return list;
		}

		IExtensionRegistry registory = Platform.getExtensionRegistry();
		IExtensionPoint point = registory.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null) {
			throw new IllegalStateException(EXTENSION_POINT_ID);
		}

		list = new ArrayList<IAdditionalLaunchConfiguration>();
		for (IExtension extension : point.getExtensions()) {
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				try {
					Object obj = element.createExecutableExtension("class");	//classëÆê´
					if (obj instanceof IAdditionalLaunchConfiguration) {
						list.add((IAdditionalLaunchConfiguration) obj);
					}
				} catch (CoreException e) {
					Activator.getDefault().getLog().log(e.getStatus());
				}
			}
		}
		return list;
	}
}
