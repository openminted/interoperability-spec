package eu.openminted.interop.controller
import static groovy.io.FileType.FILES

import org.apache.commons.io.FileUtils;
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
	static def baseDirSpec = baseDirTarget + "openminted-interoperability-spec/";
	static def baseDirTarget = "target/generated-adocs/";
	static def baseDir = "src/main/asciidoc/";

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
		def specFile = new File(baseDirSpec +spec +".adoc");
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
	static def addLinkWithIdInSpecification(File baseDir, File targetDir, String linkScript, String linkCall){
		baseDir.createTempDir()
		baseDir.eachFile {
			String name = it.getName();
			if(!name.startsWith("TEMPLATE")){
				File temp = new File(targetDir.absolutePath+ "/"+ name);
				if(temp.exists())
					temp.delete();
				temp.createNewFile();
				it.readLines().eachWithIndex {line, idx ->
					if(idx==0){
						temp << linkScript+"\n";
						line = line.replace("===","=== " + name.replace("adoc",""));
						temp << line +"\n";
						temp << linkCall;
					}else{
						temp << line + "\n";
					}
				}
			}else{
				File temp = new File(targetDir.absolutePath + "/"+ name);
				temp.createNewFile();
				temp << it;
			}
		}
	}
	static main(args) {

		FileUtils.copyDirectory(new File(baseDir),new File(baseDirTarget));
		String linkScript = '''<%
		def links()
		{
			def html = '++++\\n<div style="float:right">\\n';
			html += "<a href=\\"${spec.source}\\" target=\\"_blank\\" >Edit ${spec.name}</a><br/>\\n";
			html += '</div>\\n++++\\n';
			return html;
		}\n%>''';
		String linkCall = '''${links()}''';

		addLinkWithIdInSpecification(new File(baseDir+"openminted-interoperability-spec/req/"),new File(baseDirTarget+"openminted-interoperability-spec/req/"), linkScript, linkCall);

		//read spec file and add them to map
		//remove all lines in WG's containing include string
		//add include string line in the WG files with spec mapping
		new File(baseDirSpec+"req").eachFileRecurse(FILES) {
			if (it.name.endsWith('.adoc') && !it.name.find("TEMPLATE")) {
				updateWGspecMapping(it);
			}
		}
		new File(baseDirSpec).eachFileRecurse(FILES) {
			if (it.name.endsWith('.adoc') && it.name.startsWith("WG")){
				def temp = new File(baseDirSpec+"/temp_"+it.name);
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
				temp.renameTo(baseDirSpec+name);

			}
		}
		for (var in wgSpecMapping) {
			addEntry(var.key,var.value);
		}
		// Adding edit options in the spec files
		println "Applying templates..."
		File adocTargetFolder = new File("target/generated-adocs/openminted-interoperability-spec/req");

		def te = new groovy.text.SimpleTemplateEngine(GenerateWGSpec.class.classLoader);
		new File("target/generated-adocs/openminted-interoperability-spec/req").eachFile(FILES) { tf ->
			if (!tf.name.endsWith(".adoc") || tf.name.equals("TEMPLATE.adoc")) {
				return;
			}
			println "Processing template ${tf.name}...";
			def spec =[:];
			spec["source"]="https://github.com/openminted/interoperability-spec/blob/master/src/main/asciidoc/openminted-interoperability-spec/req/" + tf.name;
			spec["name"] = tf.name;
			try {
				def template = te.createTemplate(tf.getText("UTF-8"));
				def result = template.make([
					spec: spec]);
				def output = new File(adocTargetFolder, "${tf.name}");
				output.parentFile.mkdirs();
				output.setText(result.toString(), 'UTF-8');
			}
			catch (Exception e) {
				te.setVerbose(true);
				te.createTemplate(tf.getText("UTF-8"));
				throw e;
			}
		}



		Asciidoctor asciidoctor = Asciidoctor.Factory.create();

		render(false, "main", "target/generated-adocs/main",
				"target/generated-docs/", "include-dir-scenario", asciidoctor);

		render(false, "spec-main", "target/generated-adocs/openminted-interoperability-spec",
				"target/generated-docs/", "include-dir-spec", asciidoctor);

		render(false, "scenario-main", "target/generated-adocs/openminted-interoperability-scenarios",
				"target/generated-docs/", "include-dir-scenario", asciidoctor);

	}

}