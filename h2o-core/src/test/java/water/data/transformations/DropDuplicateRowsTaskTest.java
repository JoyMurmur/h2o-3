package water.data.transformations;

import org.junit.Test;
import org.junit.runner.RunWith;
import water.Scope;
import water.fvec.Frame;
import water.fvec.TestFrameBuilder;
import water.fvec.Vec;
import water.runner.CloudSize;
import water.runner.H2ORunner;
import water.util.FrameUtils;
import water.util.VecUtils;

import static org.junit.Assert.*;

@RunWith(H2ORunner.class)
@CloudSize(1)
public class DropDuplicateRowsTaskTest {
    
    private static final String LABEL_COLUMN_NAME = "label";

    @Test
    public void testSimpleDuplicateDetection() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 3d})
                    .withDataForCol(1, new double[]{2d, 2d, 5d})
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            FrameUtils.labelRows(frame, LABEL_COLUMN_NAME);
            final int labelColumnIndex = frame.find(LABEL_COLUMN_NAME);
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumns, labelColumnIndex, DropOrder.DropFirst);
            final DuplicatedRow[] duplicatesFound = dropDuplicateRowsTask.doAll(frame).getResult().getDuplicatedRows();
            assertEquals(1, duplicatesFound.length);
            final DuplicatedRow duplicatedRow = duplicatesFound[0];
            assertEquals(0, duplicatedRow.getStartRowIndex());
            assertEquals(1, duplicatedRow.getEndRowIndex());
            assertEquals(0, duplicatedRow.getChunkId());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testTwoDuplicatesOneChunk() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 3d, 3d})
                    .withDataForCol(1, new double[]{2d, 2d, 5d, 5d})
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build(); 
            FrameUtils.labelRows(frame, LABEL_COLUMN_NAME);
            final int labelColumnIndex = frame.find(LABEL_COLUMN_NAME);
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumns, labelColumnIndex, DropOrder.DropFirst);
            final DuplicatedRow[] duplicatesFound = dropDuplicateRowsTask.doAll(frame).getResult().getDuplicatedRows();
            assertEquals(2, duplicatesFound.length);
            final DuplicatedRow firstDuplicatedRow = duplicatesFound[0];
            assertEquals(0, firstDuplicatedRow.getStartRowIndex());
            assertEquals(1, firstDuplicatedRow.getEndRowIndex());
            assertEquals(0, firstDuplicatedRow.getChunkId());

            final DuplicatedRow secondDuplicatedRow = duplicatesFound[1];
            assertEquals(2, secondDuplicatedRow.getStartRowIndex());
            assertEquals(3, secondDuplicatedRow.getEndRowIndex());
            assertEquals(0, secondDuplicatedRow.getChunkId());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testWholeChunkDuplicatedRow() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d})
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            FrameUtils.labelRows(frame, LABEL_COLUMN_NAME);
            final int labelColumnIndex = frame.find(LABEL_COLUMN_NAME);
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumns, labelColumnIndex, DropOrder.DropFirst);
            final DuplicatedRow[] duplicatesFound = dropDuplicateRowsTask.doAll(frame).getResult().getDuplicatedRows();
            assertEquals(1, duplicatesFound.length);
            final DuplicatedRow duplicatedRow = duplicatesFound[0];
            assertEquals(0, duplicatedRow.getStartRowIndex());
            assertEquals(2, duplicatedRow.getEndRowIndex());
            assertEquals(0, duplicatedRow.getChunkId());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testCrossChunkBoundaries() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d, 1d, 1d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d, 2d, 2d})
                    .withChunkLayout(3, 2)
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            FrameUtils.labelRows(frame, LABEL_COLUMN_NAME);
            final int labelColumnIndex = frame.find(LABEL_COLUMN_NAME);
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumns, labelColumnIndex, DropOrder.DropFirst);
            final DuplicatedRow[] duplicatesFound = dropDuplicateRowsTask.doAll(frame).getResult().getDuplicatedRows();
            assertEquals(2, duplicatesFound.length);
            
            final DuplicatedRow firstDuplicatedRow = duplicatesFound[0];
            assertEquals(0, firstDuplicatedRow.getStartRowIndex());
            assertEquals(2, firstDuplicatedRow.getEndRowIndex());
            assertEquals(0, firstDuplicatedRow.getChunkId());

            final DuplicatedRow secondDuplicatedRow = duplicatesFound[1];
            assertEquals(0, secondDuplicatedRow.getStartRowIndex());
            assertEquals(1, secondDuplicatedRow.getEndRowIndex());
            assertEquals(1, secondDuplicatedRow.getChunkId());

        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testCrossChunkBoundariesVariousSecondChunk() {
        try {
            Scope.enter();
            final Frame frame = new TestFrameBuilder()
                    .withColNames("C1", "C2")
                    .withDataForCol(0, new double[]{1d, 1d, 1d, 1d, 1d,2d, 2d})
                    .withDataForCol(1, new double[]{2d, 2d, 2d, 2d, 2d,3d, 3d})
                    .withChunkLayout(3, 4)
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM)
                    .build();
            FrameUtils.labelRows(frame, LABEL_COLUMN_NAME);
            final int labelColumnIndex = frame.find(LABEL_COLUMN_NAME);
            final int[] comparedColumns = new int[]{0, 1};
            final DropDuplicateRowsTask dropDuplicateRowsTask = new DropDuplicateRowsTask(comparedColumns, labelColumnIndex, DropOrder.DropFirst);
            final DuplicatedRow[] duplicatesFound = dropDuplicateRowsTask.doAll(frame).getResult().getDuplicatedRows();
            assertEquals(3, duplicatesFound.length);

            final DuplicatedRow firstDuplicatedRow = duplicatesFound[0];
            assertEquals(0, firstDuplicatedRow.getStartRowIndex());
            assertEquals(2, firstDuplicatedRow.getEndRowIndex());
            assertEquals(0, firstDuplicatedRow.getChunkId());

            final DuplicatedRow secondDuplicatedRow = duplicatesFound[1];
            assertEquals(0, secondDuplicatedRow.getStartRowIndex());
            assertEquals(1, secondDuplicatedRow.getEndRowIndex());
            assertEquals(1, secondDuplicatedRow.getChunkId());

            final DuplicatedRow thirdDuplicatedRow = duplicatesFound[2];
            assertEquals(2, thirdDuplicatedRow.getStartRowIndex());
            assertEquals(3, thirdDuplicatedRow.getEndRowIndex());
            assertEquals(1, thirdDuplicatedRow.getChunkId());

        } finally {
            Scope.exit();
        }
    }

}
