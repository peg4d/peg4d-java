import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import nez.Grammar;
import nez.Production;
import nez.expr.NezParserCombinator;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		Grammar peg = new Grammar("");
		assertTrue(peg != null);
		peg = new NezParserCombinator(peg).load();
		Production p = peg.getProduction("DIGIT");
		assertTrue(p.match("8"));
	}

}
