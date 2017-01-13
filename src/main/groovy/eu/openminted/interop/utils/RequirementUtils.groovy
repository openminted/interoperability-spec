package eu.openminted.interop.utils

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils

import eu.openminted.interop.model.Requirement
import eu.openminted.interop.model.ProductView;;

class RequirementUtils {
	static List<Requirement> requirementList = [];
	static List<ProductView> requirementProductList = [];

	static void addRequirement(Requirement req){		
		requirementList.add(req);
		req.productDetails.each {product->
			ProductView pv = new ProductView();
			pv.id = req.id;
            pv.requirement = req;
			pv.compliance = product.compliance;
            pv.justification = product.justification;
			pv.productName = product.product;
			pv.requirementName = req.name;
			requirementProductList.add(pv); 
            
            if (StringUtils.isBlank(pv.compliance)) {
                println "REQ-${pv.id} is missing a compliance tag!";
            }
            
            if (StringUtils.isBlank(pv.productName)) {
                println "REQ-${pv.id} is missing a product name!";
            }
		}
	}
	static boolean startsWithDigitOrAlphabet(String s) {
		return Pattern.compile("^[A-Za-z0-9]").matcher(s).find();
	}	
}