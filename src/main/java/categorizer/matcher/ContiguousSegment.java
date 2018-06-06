package categorizer.matcher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class ContiguousSegment {
    private Integer start;
    private Integer end;

    Integer getLength() {
        return (end - start + 1);
    }

    boolean overlaps(ContiguousSegment other) {
        return !(this.start > other.end || this.end < other.start);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ContiguousSegment)) {
            return false;
        }
        ContiguousSegment other = (ContiguousSegment) object;
        return (this.start == other.start && this.end == other.end);
    }
}
