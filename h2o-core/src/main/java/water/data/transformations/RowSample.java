package water.data.transformations;

import water.Iced;

import java.util.Arrays;
import java.util.Objects;

public class RowSample extends Iced<RowSample> {

    private final Iced[] values;
    private final int chunkId;
    private final long rowLabel;

    public RowSample(final Iced[] values, final int chunkId, final long rowLabel) {
        this.values = values;
        this.chunkId = chunkId;
        this.rowLabel = rowLabel;
    }

    public Iced[] getValues() {
        return values;
    }

    public int getChunkId() {
        return chunkId;
    }

    public long getRowLabel() {
        return rowLabel;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RowSample rowSample = (RowSample) o;
        return chunkId == rowSample.chunkId &&
                Arrays.equals(values, rowSample.values);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(chunkId);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}
