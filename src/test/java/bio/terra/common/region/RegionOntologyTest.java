package bio.terra.common.region;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test for {@link RegionOntology} */
@Tag("unit")
public class RegionOntologyTest {

    @Test
    public void californiaTest() {
        assertTrue(RegionOntology.IsInsideOf("us-west2", "california"));
        assertFalse(RegionOntology.IsInsideOf("us-central1", "california"));
    }
}