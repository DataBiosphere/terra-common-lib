package bio.terra.common.region;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RegionOntology {
    private static Map<String, HashSet<String>> ontology;

    public static Boolean IsInsideOf(String first, String second) {
        if (ontology == null) {

            InputStream inputStream = RegionOntology.class
                .getClassLoader()
                .getResourceAsStream("regions.yaml");
            Yaml yaml = new Yaml(new Constructor(RegionDescription.class));
            RegionDescription root = yaml.load(inputStream);

            Map<String, HashSet<String>> map = new HashMap<>();
            Recurse(new HashSet<>(), root, map);

            ontology = map;
        }

        return ontology.containsKey(first) && ontology.get(first).contains(second);
    }

    private static void Recurse(HashSet<String> path, RegionDescription current, Map<String, HashSet<String>> map) {
        if (current.getRegions() == null) {
            map.put(current.getId(), new HashSet<>(path));
            return;
        }

        path.add(current.getId());

        current.getRegions().forEach((RegionDescription child) -> {
            Recurse(path, child, map);
        });

        path.remove(current.getId());
    }
}
