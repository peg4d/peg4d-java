import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class ExampleTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		assertTrue(true);
		List<String> contents = Files.readAllLines(Paths.get(this.getClass().getResource("sample.txt").toURI()));
		assertEquals(1, contents.size());
	}
}
