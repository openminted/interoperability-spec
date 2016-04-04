package eu.openminted.interop.controller
import static groovy.io.FileType.FILES
import org.apache.commons.io.FilenameUtils;
import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.Asciidoctor
import org.asciidoctor.AttributesBuilder
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.SafeMode
import org.yaml.snakeyaml.Yaml;
class GenerateWGSpec {

	static Map<String, String> wgSpecMapping = [:];
	static def stringPattern ="_Category:_" ;
	static def includeString = "include::{include-dir-spec}req";
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

	static def render(Boolean isDirectory, String model, String sourceAdoc, String targetDoc, String replaceStr,
			Asciidoctor asciidoctor){

		File adocSourceFolder = new File(sourceAdoc);

		println "Rendering... " + model
		Map<String, Object> attributes = AttributesBuilder.attributes()
				.linkCss(false)
				.dataUri(true)
				.asMap();

		attributes[replaceStr] = adocSourceFolder.absolutePath+'/';
		attributes['toc'] = 'left';
		attributes['docinfo1'] = true;
		attributes['toclevels'] = 8;
		attributes['sectanchors'] = true;

		OptionsBuilder options = OptionsBuilder.options()
				.backend('html5')
				.safe(SafeMode.UNSAFE)
				.mkDirs(true)
				.attributes(attributes)
				.docType("book")
				.toDir(new File(targetDoc));

		if(isDirectory)
			asciidoctor.renderDirectory(new AsciiDocDirectoryWalker(sourceAdoc), options);
		else {
			File adocSourceFile = new File(sourceAdoc + ".adoc")
			asciidoctor.renderFile(adocSourceFile, options);
		}
		println "Done!"
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
		Asciidoctor asciidoctor = Asciidoctor.Factory.create();

		render(true, "spec", "src/main/asciidoc/openminted-interoperability-spec",
				"target/generated-docs/spec", "include-dir-spec", asciidoctor);

		render(true, "scenario", "src/main/asciidoc/openminted-interoperability-scenarios",
				"target/generated-docs/scenarios", "include-dir-scenario", asciidoctor);

		render(false, "main", "src/main/asciidoc/main",
				"target/generated-docs/", "include-dir-scenario", asciidoctor);

		render(false, "spec-main", "src/main/asciidoc/openminted-interoperability-spec",
				"target/generated-docs/", "include-dir-spec", asciidoctor);

		render(false, "scenario-main", "src/main/asciidoc/openminted-interoperability-scenarios",
				"target/generated-docs/", "include-dir-scenario", asciidoctor);		

	}

}
