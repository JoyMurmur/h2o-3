package water.data.transformations;

import water.Scope;
import water.fvec.Frame;
import water.fvec.Vec;
import water.rapids.Merge;
import water.util.FrameUtils;
import water.util.IcedHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class DropDuplicateRows {

    private static final String LABEL_COLUMN_NAME = "label";

    final Frame sourceFrame;
    final int[] comparedColumnIndices;
    final DropOrder dropOrder;

    public DropDuplicateRows(final Frame sourceFrame, final int[] comparedColumnIndices, final DropOrder dropOrder) {
        this.sourceFrame = sourceFrame;
        this.comparedColumnIndices = comparedColumnIndices;
        this.dropOrder = dropOrder;
    }

    public Frame dropDuplicates() {
        try {
            Scope.enter();
            FrameUtils.labelRows(sourceFrame, LABEL_COLUMN_NAME);
            final Frame sortedFrame = Scope.track(sortByComparedColumns());
            DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumnIndices, sourceFrame.find(LABEL_COLUMN_NAME), dropOrder);
            final DropDuplicateRowsTask detectDuplicatesResult = dropDuplicateRowsTask.doAll(sortedFrame).getResult();

            final List<DuplicatedRow> sortedDuplicatedRows = Arrays.asList(detectDuplicatesResult.getDuplicatedRows()).stream()
                    .sorted(Comparator.comparingInt(DuplicatedRow::getChunkId).thenComparingInt(DuplicatedRow::getStartRowIndex))
                    .collect(Collectors.toCollection(LinkedList::new));

            final List<RowSample> firstRowSamples = Arrays.asList(detectDuplicatesResult.getFirstRowSamples()).stream()
                    .sorted(Comparator.comparingInt(RowSample::getChunkId))
                    .collect(Collectors.toList());

            final IcedHashMap<Integer, ChunkRemovalRange[]> chunkRemovalRange = createChunkRemovalRange(sortedDuplicatedRows,
                    firstRowSamples, sortedFrame);


            return dropDuplicateRowsTask._fr;
        } finally {
            final Vec label = sourceFrame.remove(LABEL_COLUMN_NAME);
            if (label != null) {
                label.remove();
            }
            Scope.exit();
        }
    }

    /**
     * @param duplicatedRows  Duplicated rows, surt by chunk id in an ascending order
     * @param firstRowSamples Samples of each chunk's first row. Used for duplicate overlap detection
     * @param sortedData      Data sorted by de-duplicated columns.
     * @return An instance of {@link IcedHashMap} (possibly empty) with chunk ID as a key and an array of rows to remove in the
     * dataset sorted by de-duplicated columns in each chunk.
     */
    private IcedHashMap<Integer, ChunkRemovalRange[]> createChunkRemovalRange(final List<DuplicatedRow> duplicatedRows,
                                                                              final List<RowSample> firstRowSamples,
                                                                              final Frame sortedData) {
        final IcedHashMap<Integer, ChunkRemovalRange[]> removalRanges = new IcedHashMap<>();
        final ListIterator<DuplicatedRow> duplicatedRowIterator = duplicatedRows.listIterator();
        List<ChunkRemovalRange> chunkRemovalRanges = new ArrayList<>();
        final int nchunks = sortedData.anyVec().nChunks();

        int lastChunkId = -1;
        boolean chunkBoundaryCrossed = false;
        Optional<DuplicatedRow> optionalPreviousDupRow = Optional.empty();
        for (int i = 0; i < nchunks; i++)  {
            // TODO : This iteration is now being reworked. Previously, it iterated only over duplicated rows,
            // creating ChunkRemovalRange objects (ranges to remove afterwards in next pass). However, this does not
            // cover the cases where in a chunk, there are no duplicates, but the border values actually are duplicated.
            // This has sub-cases where the border values must be checked even if there are duplicated rows in one of the chunks
            // and the duplicated row from the previous chunk does not reach chunk's end.
            // If there are duplicated rows in the chunk, check them and create ranges for removal. In the next chunk,
            // check the border values, unless the chunk is the last one. If there are no duplicated rows in the previous chunk,
            // the last line of the previous chunk and the first line of the next still still have to be checked (those samples are collected).
            final Optional<DuplicatedRow> optionalNextDupRow = duplicatedRowIterator.hasNext() ? duplicatedRowIterator.next() : Optional.empty();

            if (optionalNextDupRow.isPresent()) {
                final Optional<DuplicatedRow> optionalNextDupRow = 
                if (lastChunkId < nextDupRow.getChunkId()) {
                    lastChunkId = nextDupRow.getChunkId();
                    chunkBoundaryCrossed = true;
                }

                if (optionalPreviousDupRow.isPresent()) {
                    final DuplicatedRow previousDupRow = optionalPreviousDupRow.get();
                    final ChunkOverlap chunkOverlap = isDuplicateOverlappingChunk(previousDupRow, nextDupRow, firstRowSamples, sortedData);
                    switch (chunkOverlap) {
                        case MergeNextAndPrevious:
                            break;
                        case FirstRowOnly:
                            final Map<Integer, ChunkRemovalRange[]> ranges = resolveFirstRowOnlyOverlap(previousDupRow,
                                    firstRowSamples.get(nextDupRow.getChunkId()), dropOrder);
                            removalRanges.putAll(ranges);
                            break;
                        case SameChunk:
                            // Do nothing and continue
                            break;
                        case None:
                            commitPreviousChunks(removalRanges, chunkRemovalRanges, previousDupRow.getChunkId());
                            chunkRemovalRanges.clear();
                            chunkRemovalRanges = new ArrayList<>();
                            break; // No duplicated row overlap, continue
                    }
                } else if (nextDupRow.reachesChunkEnd(sortedData)) {
                    continue; // Evaluate chunks that reach the end of the current chunk in the next iteration
                } else {
                    // Everything else are duplicated rows inside of a chunk, not touching boundaries.
                    chunkRemovalRanges.add(new ChunkRemovalRange(nextDupRow.getStartRowIndex(), nextDupRow.getEndRowIndex()));
                }

                optionalPreviousDupRow = Optional.of(nextDupRow);
            }
        }


        return removalRanges;
    }

    private static Map<Integer, ChunkRemovalRange[]> resolveFirstRowOnlyOverlap(final DuplicatedRow previousDupRow,
                                                                                final RowSample firstRowSample,
                                                                                final DropOrder dropOrder) {
        assert previousDupRow.getKeptRow().getRowLabel() - firstRowSample.getRowLabel() != 0;


        final Map<Integer, ChunkRemovalRange[]> chunksRemovedRanges = new HashMap<>();
        switch (dropOrder) {
            case DropFirst:
                if (previousDupRow.getKeptRow().getRowLabel() > firstRowSample.getRowLabel()) {
                    chunksRemovedRanges.put(previousDupRow.getChunkId(), ChunkRemovalRange.dropDuplicatedRow(previousDupRow,
                            previousDupRow.getKeptRow().getChunkRow()));
                    // Remove the very first line from the next chunk, as it's not labeled higher and thus not kept.
                    final ChunkRemovalRange[] nextChunkFirstRowRemoved = new ChunkRemovalRange[]{new ChunkRemovalRange(0, 1)};
                    chunksRemovedRanges.put(firstRowSample.getChunkId(), nextChunkFirstRowRemoved);
                } else {
                    // Remove whole first chunk, as the kept row is the first row of the next chunk.
                    final ChunkRemovalRange firstChunkRemovalRange = ChunkRemovalRange.dropDuplicatedRow(previousDupRow);
                    chunksRemovedRanges.put(previousDupRow.getChunkId(), new ChunkRemovalRange[]{firstChunkRemovalRange});
                }
                break;
            case DropLast:
                if (previousDupRow.getKeptRow().getRowLabel() < firstRowSample.getRowLabel()) {
                    chunksRemovedRanges.put(previousDupRow.getChunkId(), ChunkRemovalRange.dropDuplicatedRow(previousDupRow,
                            previousDupRow.getKeptRow().getChunkRow()));

                    // Remove the very first line from the next chunk, as it's not labeled higher and thus not kept.
                    final ChunkRemovalRange[] nextChunkFirstRowRemoved = new ChunkRemovalRange[]{new ChunkRemovalRange(0, 1)};
                    chunksRemovedRanges.put(firstRowSample.getChunkId(), nextChunkFirstRowRemoved);
                } else {
                    // Remove whole first chunk, as the kept row is the first row of the next chunk.
                    final ChunkRemovalRange firstChunkRemovalRange = ChunkRemovalRange.dropDuplicatedRow(previousDupRow);
                    chunksRemovedRanges.put(previousDupRow.getChunkId(), new ChunkRemovalRange[]{firstChunkRemovalRange});
                }
                break;
        }

        return chunksRemovedRanges;
    }


    private static void commitPreviousChunks(final IcedHashMap<Integer, ChunkRemovalRange[]> removalRanges,
                                             final List<ChunkRemovalRange> chunkDuplicatedRows, final int chunkId) {
        removalRanges.put(chunkId, chunkDuplicatedRows.toArray(new ChunkRemovalRange[chunkDuplicatedRows.size()]));
    }

    enum ChunkOverlap {
        MergeNextAndPrevious, // Next and previous chunks contain the same duplicated row
        FirstRowOnly, // Only the first row in the next chunks contains the same duplicated row as the previous chunk
        None,
        SameChunk
    }

    private static ChunkOverlap isDuplicateOverlappingChunk(final DuplicatedRow previousDupRow, final DuplicatedRow nextDupRow,
                                                            final List<RowSample> firstRowSamples, final Frame sortedData) {

        // There is no chunk overlap if two duplicated row records are in the same chunk
        if (previousDupRow.getChunkId() == nextDupRow.getChunkId()) return ChunkOverlap.SameChunk;
        assert nextDupRow.getChunkId() - previousDupRow.getChunkId() == 1; //nextDupRow comes from next chunk

        // Now it is guaranteed two duplicated rows are in a distinct chunks.
        // If the previous DuplicatedRow does not reach the end of it's chunk, there is no overlap
        if (!previousDupRow.reachesChunkEnd(sortedData)) return ChunkOverlap.None;

        final RowSample previousChunkRowSample = previousDupRow.getRowSample()
                .orElseThrow(() -> new IllegalStateException("Expected row sample of a potentially overlapping duplicated row to be present."));

        final RowSample nextChunkRowSample = firstRowSamples.get(nextDupRow.getChunkId());

        // If the last row of the previous chunk and the first row of the next chunk do not match, there is no overlap
        if (!previousChunkRowSample.equals(nextChunkRowSample)) return ChunkOverlap.None;

        // There are only two options left. Either only the first of the next chunks also belongs to the previous
        // duplicated row or the next DuplicatedRow as a whole should be joined with the previous one.

        if (nextDupRow.getStartRowIndex() == 0) {
            return ChunkOverlap.MergeNextAndPrevious;
        } else {
            return ChunkOverlap.FirstRowOnly;
        }

    }

    /**
     * Creates a copy of the original dataset, sorted by all compared columns.
     * The sort is done with respect to {@link DropOrder} value.
     *
     * @return A new Frame sorted by all compared columns.
     */
    private Frame sortByComparedColumns() {
        return Merge.sort(sourceFrame, comparedColumnIndices, true);
    }
}
