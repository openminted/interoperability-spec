package eu.openminted.interop.model

import java.util.concurrent.atomic.DoubleAdder

class ProductCategories
{
    public static Map<String, List<String>> map = new HashMap<>();
    public static Set<String> productCategories = new HashSet<>();
    public static Set<String> products = new HashSet<>();
    public static Set<String> productsSeen = new HashSet<>();
    
    // Products
    public static String AGROVOC = "Agrovoc";
    public static String ALVIS = "Alvis";
    public static String ARGO = "ARGO";
    public static String CLARIN_CCR = "CLARIN CCR";
    public static String CORE = "CORE";
    public static String DKPRO_CORE = "DKPro Core";
    public static String FRONTIERS = "Frontiers";
    public static String GATE = "GATE";
    public static String ILSP = "ILSP";
    public static String JATS = "JATS";
    public static String LAPPS = "LAPPS";
    public static String LICENSES = "Licences";
    public static String OLIA = "OLiA";
    public static String ONTOLEX = "Ontolex";
    public static String OPENAIRE = "OpenAIRE";
    public static String SCHEMA_ORG = "schema.org";
    public static String THE_SOZ = "TheSoz";
    public static String UIMA = "UIMA";
    
    // Categories
    public static String COMPONENT_COLLECTIONS = "Component Collections";
    public static String KNOWLEDGE_RESOURCES   = "Knowledge Resources";
    public static String CONTENT_PROVIDERS     = "Content Providers";
    public static String PROCESSING_FRAMEWORKS = "Processing Frameworks";
    public static String LEGAL = "Legal";
    
    static {
        map.put(AGROVOC,    [KNOWLEDGE_RESOURCES]);
        map.put(ALVIS,      [COMPONENT_COLLECTIONS,PROCESSING_FRAMEWORKS]);
        map.put(ARGO,       [COMPONENT_COLLECTIONS]);
        map.put(CLARIN_CCR, [KNOWLEDGE_RESOURCES]);
        map.put(CORE,       [CONTENT_PROVIDERS]);
        map.put(DKPRO_CORE, [COMPONENT_COLLECTIONS]);
        map.put(FRONTIERS,  [CONTENT_PROVIDERS]);
        map.put(GATE,       [COMPONENT_COLLECTIONS,PROCESSING_FRAMEWORKS]);
        map.put(ILSP,       [COMPONENT_COLLECTIONS]);
        map.put(JATS,       [KNOWLEDGE_RESOURCES]);
        map.put(LAPPS,      [COMPONENT_COLLECTIONS]);
        map.put(LICENSES,   [LEGAL]);
        map.put(OLIA,       [KNOWLEDGE_RESOURCES]);
        map.put(ONTOLEX,    [COMPONENT_COLLECTIONS]);
        map.put(OPENAIRE,   [CONTENT_PROVIDERS]);
        map.put(SCHEMA_ORG, [KNOWLEDGE_RESOURCES]);
        map.put(THE_SOZ,    [KNOWLEDGE_RESOURCES]);
        map.put(UIMA,       [PROCESSING_FRAMEWORKS]);
        
        // Create sets of all products and product categories
        map.each { prod, cats -> cats.each  { 
            products.add(prod);
            productCategories.add(it);
        } };
    }
    
    public static isProductCategory(String aCat) 
    {
        return productCategories.contains(aCat);
    }
    
    public static List<String> getProducts(String aCat)
    {
        List<String> products = [];
        map.each { prod, cats -> if (cats.contains(aCat)) products.add(prod) };
        return products;
    }
    
    public static productSeen(String aProduct)
    {
        productsSeen.add(aProduct);    
    }
    
    public static printUncategorizedProducts()
    {
        for (String prod : productsSeen) {
            if (!map.get(prod)) {
                println "Uncategorized product: [${prod}]";
            }
        }
    }
}
