package lodanalysis.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import lodanalysis.Paths;
import lodanalysis.model.statements.Degree;
import lodanalysis.model.statements.DegreeIn;
import lodanalysis.model.statements.DegreeOut;
import lodanalysis.model.statements.LengthLiteral;
import lodanalysis.model.statements.LengthUri;
import lodanalysis.model.statements.LengthUriObj;
import lodanalysis.model.statements.LengthUriPred;
import lodanalysis.model.statements.LengthUriSub;
import lodanalysis.model.statements.NumAllLiterals;
import lodanalysis.model.statements.NumAllUris;
import lodanalysis.model.statements.NumBnodes;
import lodanalysis.model.statements.NumDistinctBnodes;
import lodanalysis.model.statements.NumDistinctClasses;
import lodanalysis.model.statements.NumDistinctDataTypes;
import lodanalysis.model.statements.NumDistinctDefinedClasses;
import lodanalysis.model.statements.NumDistinctDefinedProperties;
import lodanalysis.model.statements.NumDistinctEntities;
import lodanalysis.model.statements.NumDistinctLiterals;
import lodanalysis.model.statements.NumDistinctObjects;
import lodanalysis.model.statements.NumDistinctProperties;
import lodanalysis.model.statements.NumDistinctSubjects;
import lodanalysis.model.statements.NumDistinctTriples;
import lodanalysis.model.statements.NumDistinctUris;
import lodanalysis.model.statements.NumObjUris;
import lodanalysis.model.statements.NumSubUris;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class CreateModel {
	public enum Namespace {
		LL("ll", "http://lodlaundromat.org/resource/"),
		LLO("llo", "http://lodlaundromat.org/ontology/"),
		LLM("llm", "http://lodlaundromat.org/metrics/ontology/"),
		VOID("void", "http://rdfs.org/ns/void#"),
		VOID_EXT("void-ext", "http://ldf.fi/void-ext#"),
		RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
		RDFS("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
		PROV("prov", "http://www.w3.org/ns/prov#"),
		FOAF("foaf", "http://xmlns.com/foaf/0.1/"),
		DS("ds", "http://bio2rdf.org/bio2rdf.dataset_vocabulary:");
		
		private String prefix;
		private String url;

		Namespace(String prefix, String url) {
			this.prefix = prefix;
			this.url = url;
		}
		public String getPrefix() {
			return prefix;
		}
		public String getUrl() {
			return url;
		}
	}
	
	public Model model;
	public Resource voidDoc;
	public File metricDir;
	

	public CreateModel(File metricDir) throws IOException {
		this.metricDir = metricDir;
		model = ModelFactory.createDefaultModel();
		for (Namespace ns: Namespace.values()) {
			model.setNsPrefix(ns.getPrefix(), ns.getUrl());
		}

		voidDoc = model.createResource(Namespace.LL.getUrl() + metricDir.getName() + "/metrics");
		model.createResource(Namespace.LL.getUrl() + metricDir.getName()).addProperty(model.createProperty(Namespace.LLM.getUrl(), "metrics"), voidDoc);
		voidDoc.addProperty(model.createProperty(Namespace.RDF.getUrl(), "type"), model.createResource(Namespace.LLM.getUrl() + "Dataset"));
		
		CreateModelStatement[] descriptions = new CreateModelStatement[] {
				new NumDistinctTriples(this),
				new NumDistinctBnodes(this),
				new NumDistinctClasses(this),
				new NumDistinctEntities(this),
				new NumDistinctLiterals(this),
				new NumDistinctObjects(this),
				new NumDistinctProperties(this),
				new NumDistinctSubjects(this),
				new NumDistinctDefinedClasses(this),
				new NumDistinctDefinedProperties(this),
				new NumAllLiterals(this),
				new NumAllUris(this),
				new NumDistinctUris(this),
				new NumBnodes(this),
				new NumObjUris(this),
				new NumSubUris(this),
				new NumDistinctDataTypes(this),
				new NumDistinctLanguages(this),
//				new PartitionPropsVoid(this),
//				new PartitionTypesDs(this),
//				new PartitionTypesVoid(this),
				new Degree(this),
				new DegreeIn(this),
				new DegreeOut(this),
				new LengthUri(this),
				new LengthUriObj(this),
				new LengthUriPred(this),
				new LengthUriSub(this),
				new LengthLiteral(this),
		};
		for (CreateModelStatement description: descriptions) {
			description.createDescription();
		}
		
		
		model.write(new FileOutputStream(new File(metricDir, Paths.DESCRIPTION_NT)), "NT");
	}
	
	
}


