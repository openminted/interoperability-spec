package eu.openminted.interop.controller

class Helper {

	/*
	* This method replaces various special characters in desc
	* and creates a valid cross-reference link id. 
	*/
    static String createLinkIdFromDescription(String desc){
    	String ret = desc.replaceAll(' ','_')
    		.replaceAll('[+]','-')
    		.replaceAll("/","_")
    		.replaceAll("\\(","")
    		.replaceAll("\\)","")
    		.replaceAll(",","")
    		.replaceAll("&","");
    		
    		return ret;    		
    }

	/*
	* Renders the summary table of a WG.  
	*/    
	static String renderWGTable(spec,  Map<Integer, String> wgSpecMapping,  Map<Integer, String> reqSpecMapping){		
		String ret = "";
		
		String wgName = spec.name.replace(".adoc","");		
		def sortedWG = wgSpecMapping.grep{it.value.contains(wgName)}
						
		if(sortedWG.size() > 0){
			ret = ret + '|*ID*|*Requirement*|*WG\'s*'
			sortedWG.each{it ->
				String linksStr = it.value;	
				linksStr = Helper.removeLink(wgName, linksStr);	
				ret = ret + "\n" + "|${it.key}|<<REQ-${it.key},${reqSpecMapping.get(it.key)}>>|${linksStr}" ;			
			}
		}
		
		return ret;
    }
    
	/*
	* This method removes the unneccessary link from linksStr based on the currentWG value.
	*/
    static String removeLink(String currentWG, String linksStr){    	
    	String src = "<<" + currentWG + "," + currentWG + ">>";
    	String trg = currentWG;
    	
    	String ret = linksStr.replaceAll(src, trg);
    	    		
    	return ret;    		
    }
}
