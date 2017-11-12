package eu.openminted.interop.model

import eu.openminted.interop.utils.RequirementUtils;

class Requirement 
{
	public Integer id;
    File file;
	String name;
	String concreteness;
	String strength;
	String status;
	List<WGCategory> category = new ArrayList();
    List<String> derivedFrom = new ArrayList();
	String description;
	List<ProductDetails> productDetails = new ArrayList();

	public Requirement(File f) 
    {
        file = f;
		String concretenessRegex = "[small]#*_Concreteness:_*";
		String strengthRegex = "[small]#*_Strength:_*";
		String statusRegex = "[small]#*_Status:_*";
		String categoryRegex = "[small]#*_Category:_*";
        String derivedFromRegex = "[small]#*_Derived from:_*";
		//check if table is started
		boolean tableStarted = false;
        boolean inComplianceTable = false;

		//flag for a row of product is entered while readline. Check template of requirement
		boolean rowEntered = false;
		StringBuilder concatenatedRow = new StringBuilder();

		this.id = Integer.parseInt(f.getName().replace(".adoc",""))
		f.readLines().each{ line->
			String trimmedLine= line.trim();
			if (trimmedLine.startsWith("//")) {
				return;
			}

			// Get name
			if (trimmedLine.startsWith("==")){
				this.name = trimmedLine.replace("==","").trim()
			}

			// Get concreteness
			if (trimmedLine.startsWith(concretenessRegex))
			{
				this.concreteness = trimmedLine.split("__")[1]
			}

			// Get strength
			if (trimmedLine.startsWith(strengthRegex))
			{
				this.strength = trimmedLine.split("__")[1]
			}

			// Get status
			if (trimmedLine.startsWith(statusRegex))
			{
				this.status = trimmedLine.split("__")[1]
			}

			// Get category
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

            // Get abstract requirements
            if (trimmedLine.startsWith(derivedFromRegex))
            {
                def arr = trimmedLine.split("__")
                arr.eachWithIndex { req, index ->
                    if(index.mod(2) == 1){
                        derivedFrom.add(req);
                    }
                }
            }

			// Check if table has started
			if (trimmedLine.equals("|====")){
				if (tableStarted){
					tableStarted = false;
					rowEntered = false;
                    
                    // Also create a product for the last entry in the compliance table
                    if (inComplianceTable) {
                        if (concatenatedRow.size() > 0){
                            ProductDetails pro = parseProduct(concatenatedRow);
                            this.productDetails.add(pro);
                            concatenatedRow.setLength(0);
                        }
                    }

				} else {
					tableStarted = true;
				}
                return;
			}
            
            // Check if table is product compliance table
            if (tableStarted && trimmedLine.startsWith("|Product|")) {
                inComplianceTable = true;
            }

			// Description should start with a character or numbers
			if (!tableStarted && RequirementUtils.startsWithDigitOrAlphabet(trimmedLine)) {
				if (this.description) {
					this.description = this.description + " " + trimmedLine;
				} else {
					this.description = trimmedLine;
				}
			}

			if (inComplianceTable) {
                // A new product entry is created when hitting a blank line in the compliance table
				if (trimmedLine.isEmpty()) {
					if (rowEntered){
						//now perform actions on concatenatedRow to extract product details 
						if (concatenatedRow.size() > 0){
                            ProductDetails pro = parseProduct(concatenatedRow);
                            ProductCategories.productSeen(pro.product);
							this.productDetails.add(pro);
							concatenatedRow.setLength(0); 
						}
					}
					else {
						rowEntered = true
					}
				}

				if (rowEntered) {
					concatenatedRow.append(trimmedLine)
				}
			}
		}
        
        // Ensure that all products are mentioned for every product category that is listed under the categories
        boolean productCategorySeen = false;
        for (WGCategory cat : category) {
            if (ProductCategories.isProductCategory(cat.category)) {
                productCategorySeen = true;
                List<String> prodsInCat = ProductCategories.getProducts(cat.category);
                for (ProductDetails prodDetails : productDetails) {
                    prodsInCat.remove(prodDetails.product);
                }
                
                for (String prod : prodsInCat) {
                    println "WARNING: REQ-${id} is missing a compliance assessment for product [${prod}]!";
                }
            }
        }
        
        // Ensure that all assessed products are also in a product category that is listed
        Set<String> assessedCategories = new HashSet<>();
        category.each { if (ProductCategories.isProductCategory(it.category)) { 
            assessedCategories.add(it.category)
        } };
        for (ProductDetails prodDetails : productDetails) {
            List<String> prodCats = ProductCategories.map.get(prodDetails.product);
            if (prodCats != null && !assessedCategories.any { prodCats.contains(it) }) {
                println "WARNING: REQ-${id} has assessment for product [${prodDetails.product}] but does not list any of its product categories!";
            }
        }
        
        if (!productCategorySeen) {
            println "WARNING: REQ-${id} declares no product categories!";
        }
        
        if (concreteness == "concrete" && derivedFrom.isEmpty()) {
            println "WARNING: REQ-${id} is concrete but declares no requirements it derives from!";
        }
	}
    
    private ProductDetails parseProduct(CharSequence concatenatedRow)
    {
        def rowArr = concatenatedRow.toString().split("\\|")
        // |Product|Version|Compliant|Justification|Status
        ProductDetails pro = new ProductDetails();
        pro.product = rowArr[1].trim();
        pro.version = rowArr[2].trim();
        pro.compliance = rowArr[3].trim();
        pro.justification = rowArr[4].trim();
        pro.status = rowArr[5].trim();
        return pro;
    }
}

class WGCategory {
	String category;
}
