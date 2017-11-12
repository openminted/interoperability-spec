package eu.openminted.interop.controller
import static groovy.io.FileType.FILES

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.Asciidoctor
import org.asciidoctor.AttributesBuilder
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.SafeMode
import org.yaml.snakeyaml.Yaml

import eu.openminted.interop.model.ProductCategories
import eu.openminted.interop.model.Requirement
import eu.openminted.interop.utils.RequirementUtils

class GenerateWGSpec 
{
    public static def MAVEN_PROJECT;
    
	static Map<Integer, String> wgSpecMapping = [:];
	static Map<Integer,String> reqSpecMapping = [:];
	static def stringPatternCategory ="_Category:_" ;
	static def stringPatternHeading = "===";
	static def includeString = "include::{include-dir-spec}req";
	static def baseDirSpec = baseDirTarget + "openminted-interoperability-spec/";
	static def baseDirTarget = "target/generated-adocs/";
	static def baseDir = "src/main/asciidoc/";

	static def updateWGspecAndReqMapping(File aDescriptor)
    {
		def text = aDescriptor.getText();
		text.eachLine {
			if(it =~ stringPatternCategory) {
				def p =it.substring(it.indexOf("__"),it.length()-1);
				p=p.replaceAll("__","");
				wgSpecMapping.put(aDescriptor.name.replace(".adoc","").toInteger(), p);
			}
			if(it.startsWith(stringPatternHeading)){
				def reqNo = aDescriptor.name.replace(".adoc","");
				def p = it.substring(it.indexOf(reqNo) + reqNo.length()+1,it.length()).replaceFirst("^ *","");
				reqSpecMapping.put(aDescriptor.name.replace(".adoc","").toInteger(),p);
			}
		}
	}
    
	static def addEntry(String req , String spec ) 
    {
		def detailFile = new File(baseDirSpec + "Details.adoc");
		detailFile << "\n" + includeString + "/" + req+".adoc"  +"[]";
	}

	static def render(Boolean isDirectory, String model, String sourceAdoc, String targetDoc, 
        HashMap<String,String> replaceMap, Asciidoctor asciidoctor)
    {
		File adocSourceFolder = new File(sourceAdoc);

		println "Rendering... " + model
		Map<String, Object> attributes = AttributesBuilder.attributes()
				.linkCss(false)
				.dataUri(true)
				.asMap();
		
		replaceMap.keySet().each { key -> attributes[key] = replaceMap.get(key) };
		attributes['toc'] = 'left';
		attributes['docinfo1'] = true;
		attributes['toclevels'] = 8;
		attributes['experimental'] = true;
		attributes['sectanchors'] = true;
        attributes['pdf-style'] = 'src/main/asciidoc/theme/custom-theme.yml';
        if (MAVEN_PROJECT) {
            attributes['project-version'] = MAVEN_PROJECT.version as String;
            attributes['revnumber'] = MAVEN_PROJECT.version as String;
        }

		OptionsBuilder htmlOptions = OptionsBuilder.options()
				.backend('html5')
				.safe(SafeMode.UNSAFE)
				.mkDirs(true)
				.attributes(attributes)
				.docType("book")
				.toDir(new File(targetDoc));

        OptionsBuilder pdfOptions = OptionsBuilder.options()
                .backend('pdf')
                .safe(SafeMode.UNSAFE)
                .mkDirs(true)
                .attributes(attributes)
                .docType("book")
                .toDir(new File(targetDoc));

		if (isDirectory) {
            println "Rendering HTML..."
			asciidoctor.renderDirectory(new AsciiDocDirectoryWalker(sourceAdoc), htmlOptions);
            println "Rendering PDF..."
            asciidoctor.renderDirectory(new AsciiDocDirectoryWalker(sourceAdoc), pdfOptions);
        }
		else {
            println "Rendering HTML..."
			asciidoctor.renderFile(new File(sourceAdoc + ".adoc"), htmlOptions);
            println "Rendering PDF..."
            asciidoctor.renderFile(new File(sourceAdoc + ".adoc"), pdfOptions);
		}
		println "Done!"
	}
            
	static def addLinkWithIdInSpecification(File baseDir, File targetDir, String linkScript, 
        String linkCall)
    {
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
						temp << linkScript + "\n";
						line = line.replace("==","== " + name.replace("adoc",""));
						temp << line + "\n";
                        temp << 'ifeval::["{backend}" == "html5"]\n';
						temp << linkCall + '\n';
                        temp << 'endif::[]\n';
					}else{
						if(line.contains(stringPatternCategory)) {
							def wgFileStr = line.substring(line.indexOf("__"),line.length()-1);
							wgFileStr = wgFileStr.replaceAll("__","");
							def wgListArr = wgFileStr.split(",")
							wgListArr.eachWithIndex{wg,id->
								//if(id!=0){
								line = line.replaceAll(wg,"<<"+wg+","+wg+">>");
								//}
							}
							temp << line + "\n";
						}else{
							temp << line + "\n";
						}
					}
				}
			} else {
				File temp = new File(targetDir.absolutePath + "/"+ name);
				temp.createNewFile();
				temp << it;
			}
		}
	}
    
	static main(args) 
    {
		FileUtils.copyDirectory(new File(baseDir),new File(baseDirTarget));

        // Parse requirements
		new File("target/generated-adocs/openminted-interoperability-spec/req").eachFile(FILES) { tf ->
			if (!tf.name.endsWith(".adoc") || tf.name.equals("TEMPLATE.adoc")) {
				return;
			}
			Requirement req = new Requirement(tf)
			RequirementUtils.addRequirement(req);
		}

		def te = new groovy.text.SimpleTemplateEngine(GenerateWGSpec.class.classLoader);

		//make requirement view by product
		new File("target/generated-adocs/template").eachFile(FILES){tf->
			if(!tf.name.endsWith(".adoc")){
				return;
			}
			println "Processing template ${tf.name}...";
			try {
				def template = te.createTemplate(tf.getText("UTF-8"));
				def result = template.make([
                    requirementList: RequirementUtils.requirementList,
					requirementProductList: RequirementUtils.requirementProductList]);

				def output = new File("target/generated-adocs/template", "${tf.name}");
				output.parentFile.mkdirs();
				output.setText(result.toString(), 'UTF-8');
			}
			catch (Exception e) {
				te.setVerbose(true);
				te.createTemplate(tf.getText("UTF-8"));
				throw e;
			}
		}

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
				updateWGspecAndReqMapping(it);
			}
		}
		wgSpecMapping = wgSpecMapping.sort{ a,b -> a.key <=> b.key };
		for (var in wgSpecMapping) {
			addEntry(var.key.toString(),var.value);
		}
		reqSpecMapping = reqSpecMapping.sort{a,b -> a.key <=> b.key};
		// Adding edit options in the spec files
		println "Applying templates for WG..."

		File adocTargetFolderWG = new File("target/generated-adocs/openminted-interoperability-spec")
		//processing WG template
		new File("target/generated-adocs/openminted-interoperability-spec").eachFile(FILES){tf->
			if(!tf.name.endsWith(".adoc") || !tf.name.startsWith("WG")){
				return;
			}
			println "Processing template ${tf.name}...";
			def spec =[:];
			spec["source"]="https://github.com/openminted/interoperability-spec/blob/master/src/main/asciidoc/openminted-interoperability-spec/req/";
			spec["name"] = tf.name;
			try {
				def template = te.createTemplate(tf.getText("UTF-8"));
				def result = template.make([
					spec: spec,
                    wgSpecMapping: wgSpecMapping,
                    reqSpecMapping: reqSpecMapping]);

				def output = new File(adocTargetFolderWG, "${tf.name}");
				output.parentFile.mkdirs();
				output.setText(result.toString(), 'UTF-8');
			}
			catch (Exception e) {
				te.setVerbose(true);
				te.createTemplate(tf.getText("UTF-8"));
				throw e;
			}
		}

		//processing requirement template
		println "Applying templates for requirement..."
		File adocTargetFolder = new File("target/generated-adocs/openminted-interoperability-spec/req");
        RequirementUtils.requirementList.forEach { Requirement req ->
			//adding link to spec file
			def temp = new File("${baseDirSpec}/req/temp_${req.id}.adoc");
			temp.createNewFile();
			def adocDesc = reqSpecMapping.get(req.id);
			temp << "[[REQ-${req.id}]]"
			temp.append(req.file.getText("UTF-8"));
			temp.renameTo("${baseDirSpec}/req/${req.file.name}");
            
			//processing template
			println "Processing requirement ${req.id}...";
			def spec =[:];
			spec["source"]="https://github.com/openminted/interoperability-spec/blob/master/src/main/asciidoc/openminted-interoperability-spec/req/${req.file.name}";
			spec["name"] = "${req.file.name}";
			try {
				def template = te.createTemplate(req.file.getText("UTF-8"));
				def result = template.make([
					spec: spec]);
				def output = new File(adocTargetFolder, "${req.file.name}");
				output.parentFile.mkdirs();
				output.setText(result.toString(), 'UTF-8');
			}
			catch (Exception e) {
				te.setVerbose(true);
				te.createTemplate(req.file.getText("UTF-8"));
				throw e;
			}
		}

        ProductCategories.printUncategorizedProducts();
        
		Asciidoctor asciidoctor = Asciidoctor.Factory.create();

		HashMap<String, String> replaceMapScenarioMain=new HashMap<String, String>();
		replaceMapScenarioMain.put("include-dir-scenario",new File("target/generated-adocs/main").absolutePath+"/");
		render(false, "main", "target/generated-adocs/main",
				"target/generated-docs/", replaceMapScenarioMain, asciidoctor);

		HashMap<String, String> replaceMapSpec=new HashMap<String, String>();
		replaceMapSpec.put("include-dir-spec", new File("target/generated-adocs/openminted-interoperability-spec").absolutePath +"/");
		replaceMapSpec.putAt("include-dir-template",new File("target/generated-adocs/template").absolutePath +"/");
		render(false, "spec-main", "target/generated-adocs/openminted-interoperability-spec",
				"target/generated-docs/", replaceMapSpec, asciidoctor);

		HashMap<String, String> replaceMapScenario=new HashMap<String, String>();
		replaceMapScenario.put("include-dir-scenario", new File("target/generated-adocs/openminted-interoperability-scenarios").absolutePath +"/");
		render(false, "scenario-main", "target/generated-adocs/openminted-interoperability-scenarios",
				"target/generated-docs/", replaceMapScenario, asciidoctor);

		//copy JS files to generated-docs/js
		FileUtils.copyDirectory(new File("src/main/resources/"),new File("target/generated-docs/"));
	}

}