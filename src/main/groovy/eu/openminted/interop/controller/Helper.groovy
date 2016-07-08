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
		
		def sortedWG = wgSpecMapping.grep{it.value.contains(spec.name.replace(".adoc",""))}				
		if(sortedWG.size()>0)
		{
			ret = ret + '|*ID*|*Requirement*|*WG\'s*'
			sortedWG.each{it ->
			//def str = reqSpecMapping.get(it.key).replaceAll(' ','_').replaceAll('[+]','-').replaceAll("/","_").replaceAll("\\(","").replaceAll("\\)","").replaceAll(",","").replaceAll("&","");
			def linkId = Helper.createLinkIdFromDescription(reqSpecMapping.get(it.key));		
			ret = ret + "\n" + "|${it.key}|<<${linkId},${reqSpecMapping.get(it.key)}>>|${it.value}"
			
			}
		}
		
		return ret;
    }
}
