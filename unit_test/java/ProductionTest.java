import static org.junit.Assert.assertTrue;
import nez.Grammar;
import nez.Production;
import nez.expr.NezParserCombinator;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		Grammar peg = NezParserCombinator.newGrammar();
		Production p = peg.getProduction("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
