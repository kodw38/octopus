package com.octopus.utils.cls.jcl.resource;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface IGetURL {

	public URL[] getURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list);
	
	public URL[] removeURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list);
}
