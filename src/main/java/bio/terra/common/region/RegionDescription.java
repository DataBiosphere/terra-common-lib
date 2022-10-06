package bio.terra.common.region;

import java.util.List;

public class RegionDescription {
    private String id;
    private String description;
    private List<RegionDescription> regions;
    private List<String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RegionDescription> getRegions() {
        return regions;
    }

    public void setRegions(List<RegionDescription> regions) {
        this.regions = regions;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
