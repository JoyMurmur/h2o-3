package water.data.transformations;

import water.fvec.Chunk;
import water.fvec.NewChunk;

import java.util.Objects;

public class ChunkRemovalRange {

    private final int startRow;
    private final int endRow;

    public ChunkRemovalRange(int startRow, int endRow) {
        this.startRow = startRow;
        this.endRow = endRow;
    }

    public void apply(final Chunk originalChunk, final NewChunk newChunk) {

    }

    public static ChunkRemovalRange dropDuplicatedRow(final DuplicatedRow duplicatedRow) {
        Objects.requireNonNull(duplicatedRow);
        return new ChunkRemovalRange(duplicatedRow.getStartRowIndex(),
                duplicatedRow.getEndRowIndex());
    }

    public static ChunkRemovalRange[] dropDuplicatedRow(final DuplicatedRow duplicatedRow, final int... excludedRows) {
        Objects.requireNonNull(duplicatedRow);
        Objects.requireNonNull(excludedRows);

        if (excludedRows.length == 0) return new ChunkRemovalRange[]{dropDuplicatedRow(duplicatedRow)};

        final int removalRangesCount = excludedRows.length + 1;
        final ChunkRemovalRange[] chunkRemovalRanges = new ChunkRemovalRange[removalRangesCount];

        for (int removedRangeIndex = 0; removedRangeIndex < removalRangesCount; removedRangeIndex++) {

            final int startRow = removedRangeIndex == 0 ? duplicatedRow.getStartRowIndex() : excludedRows[removedRangeIndex - 1] + 1;
            final int endRow = excludedRows[removedRangeIndex] - 1;
            chunkRemovalRanges[removedRangeIndex] = new ChunkRemovalRange(startRow, endRow);
        }

        return chunkRemovalRanges;
    }
}
