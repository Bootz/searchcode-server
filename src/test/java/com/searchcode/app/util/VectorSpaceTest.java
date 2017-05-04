package com.searchcode.app.util;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class VectorSpaceTest extends TestCase {

    public void testMagnitude() {
        VectorSpace vectorSpace = new VectorSpace();
        double result = vectorSpace.magnitude(new HashMap<String, Integer>() {{
            put("this", 1);
        }});

        assertThat(result).isEqualTo(1);
    }

    public void testRelation() {
        VectorSpace vectorSpace = new VectorSpace();

        double result = vectorSpace.relation(new HashMap<String, Integer>() {{
            put("this", 1);
        }},
        new HashMap<String, Integer>() {{
            put("this", 1);
        }});

        assertThat(result).isEqualTo(1);
    }

    public void testRelation2() {
        VectorSpace vectorSpace = new VectorSpace();

        double result = vectorSpace.relation(new HashMap<String, Integer>() {{
            put("this", 1);
        }},
        new HashMap<String, Integer>() {{
            put("this", 1);
            put("something", 5);
        }});

        assertThat(result).isEqualTo(0.19611613513818404);
    }

    public void testConcordance() {
        VectorSpace vectorSpace = new VectorSpace();

        Map<String, Integer> concordance = vectorSpace.concordance("this is some text");
        assertThat(concordance.size()).isEqualTo(4);
    }

    public void testVectorSpace() {
        VectorSpace vectorSpace = new VectorSpace();
        Map<String, Integer> con1 = vectorSpace.concordance("Go has a lightweight test framework composed of the go test command and the testing package.");
        Map<String, Integer> con2 = vectorSpace.concordance("Package testing provides support for automated testing of Go packages. It is intended to be used in concert with the go test command, which automates execution of any function of the form.");

        double relation = vectorSpace.relation(con1, con2);

        // The relationship between the above is usually ~0.44 depending on how the text is formatted
        assertThat(relation).isEqualTo(0.4485426135725302);
    }
}
