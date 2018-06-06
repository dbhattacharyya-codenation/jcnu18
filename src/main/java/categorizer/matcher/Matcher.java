package categorizer.matcher;

import java.util.ArrayList;
import java.util.List;

public class Matcher {
    private Integer statementCount;
    private Integer fileCount;
    private List<List<Integer>> continuityMatrix;
    private List<Extraction> extractions;
    private List<List<ContiguousSegment>> validSegments;

    public Matcher(List<List<Long>> nodeIdMappings, List<List<Long>> orderedIDs) {
        fileCount = nodeIdMappings.size();
        statementCount = nodeIdMappings.get(0).size();
        extractions = new ArrayList<>();
        continuityMatrix = new ArrayList<List<Integer>>();
        validSegments = new ArrayList<List<ContiguousSegment>>();
        buildContinuityMatrix(nodeIdMappings, orderedIDs);
    }

    private void buildContinuityMatrix(List<List<Long>> nodeIdMappings, List<List<Long>> orderedIDs) {
        List<Long> nodeIdMapping, orderedID;
        for(int fileNum = 0; fileNum < fileCount; fileNum++) {
            List<Integer> continuityVector = new ArrayList<>();
            nodeIdMapping = nodeIdMappings.get(fileNum);
            orderedID = orderedIDs.get(fileNum);

            Integer segmentNumber  = 1, mappingIndex = 0, orderedIDIndex = 0;

            while(mappingIndex < nodeIdMapping.size()) {
                if (nodeIdMapping.get(mappingIndex) == 0) {
                    continuityVector.add(0);
                    mappingIndex++;
                    segmentNumber++;
                }
                else if (nodeIdMapping.get(mappingIndex) != orderedID.get(orderedIDIndex)) {
                    orderedIDIndex++;
                    segmentNumber++;
                }
                else {
                    continuityVector.add(segmentNumber);
                    mappingIndex++;
                    orderedIDIndex++;
                }
            }
            continuityMatrix.add(continuityVector);
        }
    }

    private List<ContiguousSegment> generateContiguousSegmentsForFile(Integer fileNum) {
        List<ContiguousSegment> contiguousSegments = new ArrayList<>();
        List<Integer> continuityVector = continuityMatrix.get(fileNum);
        for(int start = 0; start < statementCount; start++) {
            if (continuityVector.get(start) != 0) {
                contiguousSegments.add(new ContiguousSegment(start, start));
            }
            for(int end = start+1; end < statementCount; end++) {
                if (continuityVector.get(end) != 0 && continuityVector.get(end) == continuityVector.get(end-1)) {
                    contiguousSegments.add(new ContiguousSegment(start,end));
                }
                else {
                    break;
                }
            }
        }
        return contiguousSegments;
    }

    private void generateContiguousSegments() {
        for(int fileNum = 0; fileNum < fileCount; fileNum++) {
            validSegments.add(generateContiguousSegmentsForFile(fileNum));
        }
    }

    private List<Extraction> generatePossibleExtractions() {
        generateContiguousSegments();
        ContiguousSegment segment = new ContiguousSegment(0,0);
        List<Extraction> possibleExtractions = new ArrayList<>();

        for(int i = 1; i < (1 << fileCount); i++) {
            if ((i & (i-1)) != 0) {
                List<Integer> currentFiles = new ArrayList<>();
                for(int j = 0; j < fileCount; j++) {
                    if (((1 << j) & i) != 0) {
                        currentFiles.add(j);
                    }
                }
                for(int start = 0; start < statementCount; start++) {
                    for(int end = statementCount - 1; end >= start; end--) {
                        segment.setStart(start);
                        segment.setEnd(end);
                        boolean found = true;
                        for(Integer fileNum : currentFiles) {
                            if (!validSegments.get(fileNum).contains(segment)) {
                                found = false;
                                break;
                            }
                        }
                        if (found) {
                            possibleExtractions.add(new Extraction(segment, currentFiles));
                            break;
                        }
                    }
                }
            }
        }
        return possibleExtractions;
    }

    public void matchGreedily() {
        List<Extraction> possibleExtractions = generatePossibleExtractions();
        possibleExtractions.sort((firstExtraction,secondExtraction) -> {
            if (firstExtraction.getWeight() == secondExtraction.getWeight()) {
                return secondExtraction.getFiles().size() - firstExtraction.getFiles().size();
            }
            return secondExtraction.getWeight() - firstExtraction.getWeight();
        });

        boolean overlaps;
        for(Extraction possibleExtraction : possibleExtractions) {
            overlaps = false;
            for(Extraction takenExtraction : extractions) {
                if (possibleExtraction.overlaps(takenExtraction)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                extractions.add(possibleExtraction);
            }
        }
    }
}
