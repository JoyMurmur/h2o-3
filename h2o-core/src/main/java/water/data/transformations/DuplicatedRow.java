package water.data.transformations;

import water.Iced;
import water.fvec.Frame;

import java.util.Objects;
import java.util.Optional;

public class DuplicatedRow extends Iced<DuplicatedRow> {

    public static class KeptRow extends Iced<KeptRow> {
        private final long rowLabel;
        private final int chunkRow;

        public KeptRow(long rowLabel, int chunkRow) {
            this.rowLabel = rowLabel;
            this.chunkRow = chunkRow;
        }

        public long getRowLabel() {
            return rowLabel;
        }

        public int getChunkRow() {
            return chunkRow;
        }
    }

    private final int chunkId;
    private final int startRow;
    private final int endRow;
    private final RowSample rowSample;
    private final KeptRow keptRow;

    public DuplicatedRow(int chunkId, int startRow, int endRow, KeptRow keptRow) {
        this.chunkId = chunkId;
        this.startRow = startRow;
        this.endRow = endRow;
        this.keptRow = keptRow;
        this.rowSample = null;
    }

    public DuplicatedRow(final int chunkId, final int startRow, final int endRow, final KeptRow keptRow, final RowSample rowSample) {
        this.chunkId = chunkId;
        this.startRow = startRow;
        this.endRow = endRow;
        this.keptRow = keptRow;
        this.rowSample = rowSample;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getStartRowIndex() {
        return startRow;
    }

    public int getEndRowIndex() {
        return endRow;
    }

    /**
     * Row sample of a Duplicated row. Guaranteed to be present on potentially overlapping {@link DuplicatedRow} that reach
     * the end of a chunk.
     *
     * @return An {@link Optional} instance with {@link RowSample} inside, if this {@link DuplicatedRow} instances
     * reaches the end of a chunk. Otherwise an empty {@link Optional}.
     */
    public Optional<RowSample> getRowSample() {
        return Optional.ofNullable(rowSample);
    }

    public boolean reachesChunkEnd(final Frame data) {
        Objects.requireNonNull(data);
        return endRow == data.anyVec().chunkForChunkIdx(this.chunkId).len() - 1;
    }

    public KeptRow getKeptRow() {
        return keptRow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicatedRow that = (DuplicatedRow) o;
        return chunkId == that.chunkId &&
                startRow == that.startRow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkId, startRow);
    }
}
