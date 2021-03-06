package lodanalysis.model.statements;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import lodanalysis.Paths;
import lodanalysis.model.CreateModel;
import lodanalysis.model.CreateModelStatement;
import lodanalysis.model.CreateModel.Namespace;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;


public class NumDistinctObjects extends CreateModelStatement {


	public NumDistinctObjects(CreateModel factory) {
		super(factory);
	}

	@Override
	public void createDescription() throws IOException {
		doc.addProperty(getProp(Namespace.VOID, "distinctObjects"), FileUtils.readFileToString(new File(dir, Paths.DISTINCT_OBJECTS)), XSDDatatype.XSDinteger);
	}

}
