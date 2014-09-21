package lodanalysis.metrics.voids;

import java.io.File;
import java.io.IOException;

import lodanalysis.Paths;
import lodanalysis.metrics.DescriptionCreator;
import lodanalysis.metrics.DescriptionsFactory;
import lodanalysis.metrics.DescriptionsFactory.Namespace;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;


public class NumSubUris extends DescriptionCreator {


	public NumSubUris(DescriptionsFactory factory) {
		super(factory);
	}

	@Override
	public void createDescription() throws IOException {
		doc.addProperty(getProp(Namespace.LLO, "distinctSubjectIRIs"), Integer.toString(countLines(new File(dir, Paths.DISTINCT_URIS_SUB))), XSDDatatype.XSDlong);
	}

}
