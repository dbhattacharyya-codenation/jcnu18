package categorizer.matcher;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
class Extraction {
    private ContiguousSegment segment;
    private Integer weight;
    private List<Integer> files;

    Extraction(ContiguousSegment segment, List<Integer> files) {
        this.segment = segment;
        this.files = files;
        this.weight = segment.getLength() * files.size();
    }

    boolean overlaps(Extraction other) {
        if (this.segment.overlaps(other.segment)) {
            return true;
        }
        return !Collections.disjoint(this.files, other.files);
    }
}
