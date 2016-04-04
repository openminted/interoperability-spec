package eu.openminted.interop.controller
import static groovy.io.FileType.FILES

class GenerateWGSpec {

	static Map<String, String> wgSpecMapping = [:];
	static def stringPattern ="_Category:_" ;
	static def includeString = "include::{include-dir}req";
	static def baseDir = "src/main/asciidoc/openminted-interoperability-spec/";

	static def updateWGspecMapping(File aDescriptor){
		def text = aDescriptor.getText();
		text.eachLine {
			if(it =~ stringPattern) {
				def p =it.split("__");
				wgSpecMapping.put(aDescriptor.name, p[1]);
			}
		}
	}
	static def addEntry(String req , String spec ) {
		def specFile = new File(baseDir +spec +".adoc");
		println specFile
		specFile << includeString + "/" + req  +"[]" +"\n"
	}

	static main(args) {

		//read spec file and add them to map
		//remove all lines in WG's containing include string
		//add include string line in the WG files with spec mapping
		new File(baseDir+"req").eachFileRecurse(FILES) {
			if (it.name.endsWith('.adoc') && !it.name.find("TEMPLATE")) {
				updateWGspecMapping(it);
			}
		}
		new File(baseDir).eachFileRecurse(FILES) {
			if (it.name.endsWith('.adoc') && it.name.startsWith("WG")){
				def temp = new File(baseDir+"/temp_"+it.name);
				temp.createNewFile();
				it.getText().eachLine {tf ->

					if(!tf.startsWith(includeString))
					{
						temp << tf +"\n";
					}
				}
				def name = it.getName();
				println name;
				it.delete();
				temp.renameTo(baseDir+name);

			}
		}
		for (var in wgSpecMapping) {
			addEntry(var.key,var.value);
		}

	}

}
