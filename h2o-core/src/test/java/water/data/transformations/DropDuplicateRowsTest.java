package water.data.transformations;

import org.junit.Test;
import org.junit.runner.RunWith;
import water.DKV;
import water.Scope;
import water.fvec.Frame;
import water.fvec.TestFrameBuilder;
import water.fvec.Vec;
import water.runner.CloudSize;
import water.runner.H2ORunner;

import static org.junit.Assert.*;

@RunWith(H2ORunner.class)
@CloudSize(1)
public class DropDuplicateRowsTest {
    
    @Test
    public void testDeduplication(){
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2", "C3")
                    .withDataForCol(0, new double[]{1d, 1d, 3d})
                    .withDataForCol(1, new double[]{2d, 2d, 5d})
                    .withDataForCol(2, new double[]{1d, 2d, 6d})
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM, Vec.T_NUM)
                    .build();
            final int[] comparedColumns =  new int[]{0,1};
            final DropDuplicateRows dropDuplicateRows = new DropDuplicateRows(frame, comparedColumns, DropOrder.DropFirst);
            final Frame deduplicatedFrame = Scope.track(dropDuplicateRows.dropDuplicates());
            assertNotNull(deduplicatedFrame);
            assertEquals(2, deduplicatedFrame.numRows());
            
        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testDuplicatedRowsSorted() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d, 1d, 1d,2d, 2d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d, 2d, 2d,3d, 3d})
                    .withChunkLayout(7)
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRows dropDuplicateRows = new DropDuplicateRows(frame, comparedColumns, DropOrder.DropFirst);
            final Frame deduplicatedFrame = Scope.track(dropDuplicateRows.dropDuplicates());
            assertNotNull(deduplicatedFrame);
            assertEquals(3, deduplicatedFrame.numRows());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testDuplicateValuesOnChunkBoundary() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d, 1d, 1d,2d, 2d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d, 2d, 2d,3d, 3d})
                    .withChunkLayout(3,4)
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRows dropDuplicateRows = new DropDuplicateRows(frame, comparedColumns, DropOrder.DropFirst);
            final Frame deduplicatedFrame = Scope.track(dropDuplicateRows.dropDuplicates());
            assertNotNull(deduplicatedFrame);
            assertNotNull(DKV.get(frame._key));
            assertEquals(3, deduplicatedFrame.numRows());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testDuplicateValueFirstRowOverlap() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d, 1d, 2d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d, 2d, 3d})
                    .withChunkLayout(3,2)
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRows dropDuplicateRows = new DropDuplicateRows(frame, comparedColumns, DropOrder.DropFirst);
            final Frame deduplicatedFrame = Scope.track(dropDuplicateRows.dropDuplicates());
            assertNotNull(deduplicatedFrame);
            assertNotNull(DKV.get(frame._key));
            assertEquals(3, deduplicatedFrame.numRows());

        } finally {
            Scope.exit();
        }
    }
}
