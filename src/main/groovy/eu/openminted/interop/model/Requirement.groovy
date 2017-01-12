package eu.openminted.interop.model

import eu.openminted.interop.utils.RequirementUtils;

class Requirement {

	public Integer id;
    File file;
	String name;
	String concreteness;
	String strength;
	String status;
	List<WGCategory> category = new ArrayList();
	String description;
	List<ProductDetails> productDetails = new ArrayList();

	Requirement(File f){
        file = f;
		String concretenessRegex = "[small]#*_Concreteness:_*";
		String strengthRegex = "[small]#*_Strength:_*";
		String statusRegex = "[small]#*_Status:_*";
		String categoryRegex = "[small]#*_Category:_*";
		//check if table is started
		boolean tableStarted = false;
        boolean inComplianceTable = false;

		//flag for a row of product is entered while readline. Check template of requirement
		boolean rowEntered = false;
		StringBuilder concatenatedRow = new StringBuilder();

		this.id = Integer.parseInt(f.getName().replace(".adoc",""))
		f.readLines().each{ line->
			String trimmedLine= line.trim();
			if(trimmedLine.startsWith("//")) {
				return;
			}

			// Get name
			if (trimmedLine.startsWith("==")){
				this.name = trimmedLine.replace("==","").trim()
			}

			//Get concreteness
			if (trimmedLine.startsWith(concretenessRegex))
			{
				this.concreteness = trimmedLine.split("__")[1]
			}

			//Get strength
			if (trimmedLine.startsWith(strengthRegex))
			{
				this.strength = trimmedLine.split("__")[1]
			}

			//Get status
			if (trimmedLine.startsWith(statusRegex))
			{
				this.status = trimmedLine.split("__")[1]
			}

			//Get category
			if (trimmedLine.startsWith(categoryRegex))
			{
				def arr = trimmedLine.split("__")
				arr.eachWithIndex { wg,wgid->
					if(wgid.mod(2) == 1){
						WGCategory wgCat = new WGCategory();
						wgCat.category = wg;
						this.category.add(wgCat);
					}
				}
			}

			// Check if table has started
			if (trimmedLine.equals("|====")){
				if (tableStarted){
					tableStarted = false;
					rowEntered = false;
				} else {
					tableStarted = true;
				}
                return;
			}
            
            // Check if table is product table
            if (tableStarted && trimmedLine.startsWith("|Product|")) {
                inComplianceTable = true;
            }

			//Description should start with a character or numbers
			if (!tableStarted && RequirementUtils.startsWithDigitOrAlphabet(trimmedLine)){
				if (this.description) {
					this.description = this.description + " " + trimmedLine;
				} else {
					this.description = trimmedLine;
				}
			}

			if (inComplianceTable){
				if(trimmedLine.isEmpty()){
					if(rowEntered){
						//now perform actions on concatenatedRow to extract product details 
						if(concatenatedRow.size()>0){
							def rowArr = concatenatedRow.toString().split("\\|")					
//							|Product|Version|Compliant|Justification|Status							
							ProductDetails pro = new ProductDetails();
							pro.product = rowArr[1].trim();
							pro.version = rowArr[2].trim();
							pro.compliance = rowArr[3].trim();
							pro.justification = rowArr[4].trim();
							pro.status = rowArr[5].trim();
							this.productDetails.add(pro);
							concatenatedRow.setLength(0); 
						}
					}
					else
						rowEntered = true
				}

				if(rowEntered){
					concatenatedRow.append(trimmedLine)
				}
			}
		}		
	}
}
class WGCategory{
	String category;
}
